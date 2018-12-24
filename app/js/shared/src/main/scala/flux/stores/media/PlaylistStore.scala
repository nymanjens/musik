package flux.stores.media

import common.OrderToken
import flux.action.Actions
import flux.action.Actions.{AddSongsToPlaylist, RemoveEntriesFromPlaylist}
import flux.action.Actions
import flux.action.Actions.AddSongsToPlaylist.Placement
import flux.stores.media.PlaylistStore.State
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import models.access.DbQueryImplicits._
import models.access.{JsEntityAccess, ModelField}
import models.media.{JsPlaylistEntry, PlayStatus, PlaylistEntry, Song}
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
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
    entityModification.entityType == EntityType.PlaylistEntryType
}
object PlaylistStore {
  case class State(entries: Seq[JsPlaylistEntry])
}
