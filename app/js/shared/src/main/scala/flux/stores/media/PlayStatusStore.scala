package flux.stores.media

import flux.action.Dispatcher
import flux.stores.AsyncEntityDerivedStateStore
import flux.stores.media.PlayStatusStore.State
import models.access.DbQueryImplicits._
import models.access.{JsEntityAccess, ModelField}
import models.media.{JsPlaylistEntry, PlayStatus, PlaylistEntry}
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlayStatusStore(implicit entityAccess: JsEntityAccess, user: User, dispatcher: Dispatcher)
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

  def advanceEntriesInPlaylist(step: Int): Future[Unit] = async {
    val playlistEntries = await(PlaylistEntry.getOrderedSeq())
    val currentSongIndex = {
      for {
        playStatus <- await(PlayStatus.get())
        index <- {
          val entryIds = playlistEntries.map(_.id)
          if (entryIds contains playStatus.currentPlaylistEntryId) {
            Some(entryIds.indexOf(playStatus.currentPlaylistEntryId))
          } else {
            None
          }
        }
      } yield index
    } getOrElse 0

    val newSongIndex = currentSongIndex + step
    if (playlistEntries.indices contains newSongIndex) {
      await(upsertPlayStatus(currentPlaylistEntryId = playlistEntries(newSongIndex).id))
    }
  }

  def indicateSongEnded(): Future[Unit] = async {
    if (await(stateFuture).stopAfterCurrentSong) {
      await(upsertPlayStatus(hasStarted = false, stopAfterCurrentSong = false))
    }
    await(advanceEntriesInPlaylist(step = +1))
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
    val modifications = await(PlayStatus.get()) match {
      case Some(playStatus) =>
        Seq(
          EntityModification.createUpdate(PlayStatus(
            currentPlaylistEntryId =
              nonNullLongOrElse(currentPlaylistEntryId, fallback = playStatus.currentPlaylistEntryId),
            hasStarted = nonNullBooleanOrElse(hasStarted, fallback = playStatus.hasStarted),
            stopAfterCurrentSong =
              nonNullBooleanOrElse(stopAfterCurrentSong, fallback = playStatus.stopAfterCurrentSong),
            userId = user.id,
            idOption = Some(playStatus.id)
          )))
      case None if currentPlaylistEntryId != null =>
        Seq(
          EntityModification.createAddWithRandomId(PlayStatus(
            currentPlaylistEntryId = currentPlaylistEntryId,
            hasStarted = nonNullBooleanOrElse(hasStarted, fallback = false),
            stopAfterCurrentSong = nonNullBooleanOrElse(stopAfterCurrentSong, fallback = false),
            userId = user.id
          )))
      case _ => Seq()
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
    entityModification.entityType == EntityType.PlayStatusType ||
      entityModification.entityType == EntityType.PlaylistEntryType

  object EntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit =
      async {
        val userId = user.id
        modifications.collect {
          case EntityModification.Add(PlayStatus(_, /* hasStarted */ true, _, `userId`, _)) |
              EntityModification.Update(PlayStatus(_, /* hasStarted */ true, _, `userId`, _)) =>
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
