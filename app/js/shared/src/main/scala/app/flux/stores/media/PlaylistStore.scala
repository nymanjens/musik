package app.flux.stores.media

import app.common.OrderToken
import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist
import app.flux.action.AppActions.RemoveEntriesFromPlaylist
import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.flux.stores.media.PlaylistStore.State
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.media.JsPlaylistEntry
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.modification.EntityTypes
import app.models.media.Song
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Artist
import app.models.media.Album
import app.models.user.User
import app.models.media.Song
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Artist
import app.models.media.Album
import app.models.user.User
import app.models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlaylistStore(implicit entityAccess: JsEntityAccess, user: User, dispatcher: Dispatcher)
    extends AsyncEntityDerivedStateStore[State] {
  dispatcher.registerPartialAsync {
    case AddSongsToPlaylist(songIds, placement) =>
      async {
        val currentPlaylist = await(stateFuture).entries
        val orderTokens = {
          def valuesBetweenPlaylistEntries(lower: Option[JsPlaylistEntry], higher: Option[JsPlaylistEntry]) =
            OrderToken.evenlyDistributedValuesBetween(
              numValues = songIds.size,
              lowerExclusive = lower.map(_.orderToken),
              higherExclusive = higher.map(_.orderToken))
          def maybeGetInPlaylist(index: Int): Option[JsPlaylistEntry] =
            if (index < currentPlaylist.size) Some(currentPlaylist(index)) else None
          placement match {
            case Placement.AfterCurrentSong =>
              val maybeCurrentPlaylistIndex =
                for {
                  playStatus <- await(PlayStatus.get())
                  currentPlaylistEntry <- currentPlaylist.find(_.id == playStatus.currentPlaylistEntryId)
                } yield currentPlaylist.indexOf(currentPlaylistEntry)

              maybeCurrentPlaylistIndex match {
                case Some(currentPlaylistIndex) =>
                  valuesBetweenPlaylistEntries(
                    maybeGetInPlaylist(currentPlaylistIndex),
                    maybeGetInPlaylist(currentPlaylistIndex + 1))
                case None =>
                  valuesBetweenPlaylistEntries(None, currentPlaylist.headOption)
              }
            case Placement.AtEnd =>
              valuesBetweenPlaylistEntries(currentPlaylist.lastOption, None)
          }
        }
        val modifications = for ((songId, orderToken) <- songIds.toVector zip orderTokens)
          yield
            EntityModification.createAddWithRandomId(
              PlaylistEntry(songId = songId, orderToken = orderToken, userId = user.id))
        await(entityAccess.persistModifications(modifications))
      }

    case RemoveEntriesFromPlaylist(playlistEntryIdsToRemove) =>
      async {
        val playStatus = await(PlayStatus.get())
        if (playStatus.isDefined) {
          val currentEntryId = playStatus.get.currentPlaylistEntryId
          if (playlistEntryIdsToRemove contains currentEntryId) {
            val entries = await(PlaylistEntry.getOrderedSeq())
            val currentIndex = entries.map(_.id).indexOf(currentEntryId)
            def searchNonRemovedEntry(index: Int, direction: Int): Option[PlaylistEntry] = {
              if (entries.indices contains index) {
                if (playlistEntryIdsToRemove contains entries(index).id) {
                  searchNonRemovedEntry(index + direction, direction)
                } else {
                  Some(entries(index))
                }
              } else {
                None
              }
            }
            val maybeNextEntry =
              searchNonRemovedEntry(currentIndex + 1, direction = +1) orElse
                searchNonRemovedEntry(currentIndex - 1, direction = -1)
            await(entityAccess.persistModifications {
              maybeNextEntry match {
                case Some(nextEntry) =>
                  EntityModification.createUpdate(playStatus.get.copy(currentPlaylistEntryId = nextEntry.id))
                case None => EntityModification.createDelete(playStatus.get)
              }
            })
          }
        }
        val modifications = playlistEntryIdsToRemove.toVector.map(EntityModification.Remove[PlaylistEntry])
        await(entityAccess.persistModifications(modifications))
      }
  }

  // **************** Implementation of base class methods **************** //
  override protected def calculateState(): Future[State] = async {
    val entries = await(PlaylistEntry.getOrderedSeq())
    val jsEntries = await(Future.sequence(entries.map(JsPlaylistEntry.fromEntity)))
    State(entries = jsEntries)
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == PlaylistEntry.Type
}
object PlaylistStore {
  case class State(entries: Seq[JsPlaylistEntry])
}
