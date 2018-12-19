package flux.stores.media

import common.OrderToken
import flux.action.Action.AddSongsToPlaylist
import flux.action.Action.AddSongsToPlaylist.Placement
import flux.action.Dispatcher
import flux.stores.AsyncEntityDerivedStateStore
import flux.stores.media.PlaylistStore.State
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
              val maybePlayStatus =
                await(entityAccess.newQuery[PlayStatus]().findOne(ModelField.PlayStatus.userId, user.id))
              val maybeCurrentPlaylistIndex =
                for {
                  playStatus <- maybePlayStatus
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

    // TODO: case RemoveSongsFromPlaylist =>
    // val modifications = playlistEntries.toVector.map(EntityModification.createDelete[PlaylistEntry])
    // entityAccess.persistModifications(modifications)
  }

  // **************** Implementation of base class methods **************** //
  override protected def calculateState(): Future[State] = async {
    val entries =
      await(entityAccess.newQuery[PlaylistEntry]().filter(ModelField.PlaylistEntry.userId === user.id).data())
        .sortBy(e => (e.orderToken, e.id))
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
