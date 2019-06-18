package app.flux.stores.media

import app.models.access.ModelFields
import app.models.media.JsPlaylistEntry
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.CombiningStateStore
import hydro.flux.stores.StateStore
import hydro.models.access.JsEntityAccess
import hydro.models.access.ModelField
import hydro.models.modification.EntityModification
import org.scalajs.dom

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlayStatusStore private (entityDerivedStore: PlayStatusStore.EntityDerived,
                                     fromLocalStorageStore: PlayStatusStore.FromLocalStorage)(
    implicit entityAccess: JsEntityAccess,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock)
    extends CombiningStateStore[
      Option[PlayStatusStore.EntityDerived.State],
      PlayStatusStore.FromLocalStorage.State,
      Option[PlayStatusStore.State]](entityDerivedStore, fromLocalStorageStore) {

  private var playWasEverStartedInThisSession: Boolean = false

  // **************** Public mutating API **************** //
  def play(playlistEntryId: Long): Future[Unit] = {
    upsertPlayStatus(currentPlaylistEntryId = playlistEntryId, hasStarted = true)
  }

  def togglePlay(playing: java.lang.Boolean = null): Future[Unit] = async {
    val newHasStarted: Boolean = playing match {
      case null =>
        val combinedState =
          combineStoreStates(await(entityDerivedStore.stateFuture), fromLocalStorageStore.state)
        !combinedState.hasStarted
      case bool => bool
    }
    if (newHasStarted) {
      playWasEverStartedInThisSession = true
    }
    await(upsertPlayStatus(hasStarted = newHasStarted))
  }

  def toggleStopAfterCurrentSong(): Future[Unit] = async {
    await(
      upsertPlayStatus(stopAfterCurrentSong = !await(entityDerivedStore.stateFuture).stopAfterCurrentSong))
  }

  /** Returns true if anything changed */
  def advanceEntriesInPlaylist(step: Int): Future[Boolean] = async {
    val playlistEntries = await(PlaylistEntry.getOrderedSeq())
    val currentSongIndex = {
      for (playStatus <- await(PlayStatus.get()))
        yield playlistEntries.map(_.id).indexOf(playStatus.currentPlaylistEntryId)
    } getOrElse 0

    val newSongIndex = currentSongIndex + step
    if (playlistEntries.indices contains newSongIndex) {
      await(upsertPlayStatus(currentPlaylistEntryId = playlistEntries(newSongIndex).id))
      true
    } else {
      false
    }
  }

  def indicateSongEnded(): Future[Unit] = async {
    if (await(entityDerivedStore.stateFuture).stopAfterCurrentSong) {
      await(upsertPlayStatus(hasStarted = false, stopAfterCurrentSong = false))
    }
    val advanced = await(advanceEntriesInPlaylist(step = +1))

    if (!advanced) {
      // This is the last song of the playlist
      await(upsertPlayStatus(hasStarted = false))
    }
  }

  def toggleRemoteControl(): Unit = {
    fromLocalStorageStore.setIsRemoteControl(!fromLocalStorageStore.state.isRemoteControl)
  }

  private def upsertPlayStatus(currentPlaylistEntryId: java.lang.Long = null,
                               hasStarted: java.lang.Boolean = null,
                               stopAfterCurrentSong: java.lang.Boolean = null): Future[Unit] = async {
    def nonNullBooleanOrElse(bool: java.lang.Boolean, fallback: Boolean): Boolean = bool match {
      case null                    => fallback
      case java.lang.Boolean.TRUE  => true
      case java.lang.Boolean.FALSE => false
    }
    def nonNullLongOrElse(long: java.lang.Long, fallback: Long): Long = long match {
      case null => fallback
      case v    => v
    }
    def someIfNonNull(value: Any,
                      modelField: ModelField[_, PlayStatus]): Option[ModelField[_, PlayStatus]] = {
      if (value == null) None
      else Some(modelField)
    }
    val modifications = await(PlayStatus.get(verifyConsistency = false)) match {
      case Some(playStatus) =>
        Seq(
          EntityModification.createUpdate(
            playStatus.copy(
              currentPlaylistEntryId =
                nonNullLongOrElse(currentPlaylistEntryId, fallback = playStatus.currentPlaylistEntryId),
              hasStarted = nonNullBooleanOrElse(hasStarted, fallback = playStatus.hasStarted),
              stopAfterCurrentSong =
                nonNullBooleanOrElse(stopAfterCurrentSong, fallback = playStatus.stopAfterCurrentSong)
            ),
            fieldMask = Seq() ++
              someIfNonNull(currentPlaylistEntryId, ModelFields.PlayStatus.currentPlaylistEntryId) ++
              someIfNonNull(hasStarted, ModelFields.PlayStatus.hasStarted) ++
              someIfNonNull(stopAfterCurrentSong, ModelFields.PlayStatus.stopAfterCurrentSong)
          ))
      case None =>
        val maybeEntryId: Option[Long] =
          if (currentPlaylistEntryId != null) Some(currentPlaylistEntryId)
          else await(entityDerivedStore.stateFuture).currentPlaylistEntry.map(_.id)
        maybeEntryId match {
          case Some(entryId) =>
            Seq(
              EntityModification.createAddWithRandomId(PlayStatus(
                currentPlaylistEntryId = entryId,
                hasStarted = nonNullBooleanOrElse(hasStarted, fallback = false),
                stopAfterCurrentSong = nonNullBooleanOrElse(stopAfterCurrentSong, fallback = false),
                userId = user.id
              )))
          case None => Seq()

        }
    }
    await(entityAccess.persistModifications(modifications))
  }

  // **************** Implementation of base class methods **************** //
  override protected def combineStoreStates(
      maybeEntityDerivedState: Option[PlayStatusStore.EntityDerived.State],
      localStorageState: PlayStatusStore.FromLocalStorage.State): Option[PlayStatusStore.State] = {
    for (entityDerivedState <- maybeEntityDerivedState)
      yield combineStoreStates(entityDerivedState, localStorageState)
  }

  private def combineStoreStates(
      entityDerivedState: PlayStatusStore.EntityDerived.State,
      localStorageState: PlayStatusStore.FromLocalStorage.State): PlayStatusStore.State = {
    val isRemoteControl = localStorageState.isRemoteControl

    PlayStatusStore.State(
      currentPlaylistEntry = entityDerivedState.currentPlaylistEntry,
      hasStarted = entityDerivedState.hasStarted && (playWasEverStartedInThisSession || isRemoteControl),
      stopAfterCurrentSong = entityDerivedState.stopAfterCurrentSong,
      isRemoteControl = isRemoteControl,
    )
  }
}
object PlayStatusStore {

  def apply()(implicit entityAccess: JsEntityAccess,
              user: User,
              dispatcher: Dispatcher,
              clock: Clock): PlayStatusStore = {
    new PlayStatusStore(new EntityDerived(), new FromLocalStorage)
  }

  case class State(currentPlaylistEntry: Option[JsPlaylistEntry],
                   hasStarted: Boolean,
                   stopAfterCurrentSong: Boolean,
                   isRemoteControl: Boolean,
  )
  object State {
    def nullInstance: State =
      State(
        currentPlaylistEntry = None,
        hasStarted = false,
        stopAfterCurrentSong = false,
        isRemoteControl = false,
      )
  }

  private final class EntityDerived(implicit entityAccess: JsEntityAccess,
                                    user: User,
                                    dispatcher: Dispatcher,
                                    clock: Clock)
      extends AsyncEntityDerivedStateStore[EntityDerived.State] {

    // **************** Implementation of base class methods **************** //
    override protected def calculateState(): Future[EntityDerived.State] = async {
      val maybePlayStatus = await(PlayStatus.get())
      val currentPlaylistEntryId = maybePlayStatus.map(_.currentPlaylistEntryId) match {
        case Some(id) => Some(id)
        case None     => await(PlaylistEntry.getOrderedSeq()).headOption.map(_.id)
      }
      val currentPlaylistEntry =
        if (currentPlaylistEntryId.isDefined)
          Some(await(JsPlaylistEntry.fromEntityId(currentPlaylistEntryId.get)))
        else None
      EntityDerived.State(
        currentPlaylistEntry = currentPlaylistEntry,
        hasStarted = maybePlayStatus.map(_.hasStarted) getOrElse false,
        stopAfterCurrentSong = maybePlayStatus.map(_.stopAfterCurrentSong) getOrElse false
      )
    }

    override protected def modificationImpactsState(entityModification: EntityModification,
                                                    state: EntityDerived.State): Boolean =
      entityModification.entityType == PlayStatus.Type ||
        entityModification.entityType == PlaylistEntry.Type

  }
  object EntityDerived {
    case class State(currentPlaylistEntry: Option[JsPlaylistEntry],
                     hasStarted: Boolean,
                     stopAfterCurrentSong: Boolean,
    )
  }

  private final class FromLocalStorage extends StateStore[FromLocalStorage.State] {

    private val remoteControlStorageKey: String = "remote-control"
    private var remoteControlValueCache: Option[Boolean] = None

    // **************** API **************** //
    def setIsRemoteControl(value: Boolean): Unit = {
      val previousValue = state.isRemoteControl
      dom.window.localStorage.setItem(remoteControlStorageKey, value.toString)
      remoteControlValueCache = Some(value)
      if (previousValue != value) {
        invokeStateUpdateListeners()
      }
    }

    // **************** Implementation of base class methods **************** //
    override def state: FromLocalStorage.State = {
      val isRemoteControl = remoteControlValueCache getOrElse {
        dom.window.localStorage.getItem(remoteControlStorageKey) match {
          case "true"  => true
          case "false" => false
          case null    => false
        }
      }
      FromLocalStorage.State(isRemoteControl = isRemoteControl)
    }
  }
  object FromLocalStorage {
    case class State(isRemoteControl: Boolean)
  }

}
