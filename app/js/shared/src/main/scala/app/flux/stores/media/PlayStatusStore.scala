package app.flux.stores.media

import app.flux.stores.media.PlayStatusStore.State
import app.models.access.ModelFields
import app.models.media.JsPlaylistEntry
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import hydro.models.modification.EntityModification
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.JsEntityAccess
import hydro.models.access.ModelField

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlayStatusStore(implicit entityAccess: JsEntityAccess,
                            user: User,
                            dispatcher: Dispatcher,
                            clock: Clock)
    extends AsyncEntityDerivedStateStore[State] {

  private var playWasEverStartedInThisSession: Boolean = false

  entityAccess.registerListener(EntityAccessListener)

  // **************** Public mutating API **************** //
  def play(playlistEntryId: Long): Future[Unit] = {
    upsertPlayStatus(currentPlaylistEntryId = playlistEntryId, hasStarted = true)
  }

  def togglePlay(playing: java.lang.Boolean = null): Future[Unit] = async {
    val newHasStarted: Boolean = playing match {
      case null =>
        val state = await(stateFuture)
        !state.hasStarted
      case bool => bool
    }
    await(upsertPlayStatus(hasStarted = newHasStarted))
  }

  def toggleStopAfterCurrentSong(): Future[Unit] = async {
    await(upsertPlayStatus(stopAfterCurrentSong = !await(stateFuture).stopAfterCurrentSong))
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
    if (await(stateFuture).stopAfterCurrentSong) {
      await(upsertPlayStatus(hasStarted = false, stopAfterCurrentSong = false))
    }
    val advanced = await(advanceEntriesInPlaylist(step = +1))

    if (!advanced) {
      // This is the last song of the playlist
      await(upsertPlayStatus(hasStarted = false))
    }
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
          else await(stateFuture).currentPlaylistEntry.map(_.id)
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
  override protected def calculateState(): Future[State] = async {
    val maybePlayStatus = await(PlayStatus.get())
    val currentPlaylistEntryId = maybePlayStatus.map(_.currentPlaylistEntryId) match {
      case Some(id) => Some(id)
      case None     => await(PlaylistEntry.getOrderedSeq()).headOption.map(_.id)
    }
    val currentPlaylistEntry =
      if (currentPlaylistEntryId.isDefined)
        Some(await(JsPlaylistEntry.fromEntityId(currentPlaylistEntryId.get)))
      else None
    State(
      currentPlaylistEntry = currentPlaylistEntry,
      hasStarted = maybePlayStatus.map(_.hasStarted && playWasEverStartedInThisSession) getOrElse false,
      stopAfterCurrentSong = maybePlayStatus.map(_.stopAfterCurrentSong) getOrElse false
    )
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == PlayStatus.Type ||
      entityModification.entityType == PlaylistEntry.Type

  object EntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit =
      async {
        val userId = user.id
        modifications.collect {
          case EntityModification.Add(PlayStatus(_, /* hasStarted */ true, _, `userId`, _, _)) |
              EntityModification.Update(PlayStatus(_, /* hasStarted */ true, _, `userId`, _, _)) =>
            playWasEverStartedInThisSession = true
        }
      }
  }
}
object PlayStatusStore {
  case class State(currentPlaylistEntry: Option[JsPlaylistEntry],
                   hasStarted: Boolean,
                   stopAfterCurrentSong: Boolean)
  object State {
    def nullInstance: State =
      State(currentPlaylistEntry = None, hasStarted = false, stopAfterCurrentSong = false)
  }
}
