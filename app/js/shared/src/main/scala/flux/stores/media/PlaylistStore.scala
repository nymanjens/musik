package flux.stores.media

import models.access.DbQueryImplicits._
import api.ScalaJsApiClient
import common.OrderToken
import flux.action.Action.UpsertUser
import flux.action.Dispatcher
import flux.stores.AsyncEntityDerivedStateStore
import flux.stores.media.PlaylistStore.State
import models.access.{JsEntityAccess, ModelField}
import models.media.{PlaylistEntry, Song}
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlaylistStore(implicit entityAccess: JsEntityAccess, user: User)
    extends AsyncEntityDerivedStateStore[State] {

  // **************** Additional public API: Mutating methods **************** //
  def addEntriesToEnd(songs: Iterable[Song]): Future[Unit] = async {
    val currentPlaylist = await(stateFuture).entries
    val orderTokens = OrderToken.evenlyDistributedValuesBetween(
      numValues = songs.size,
      lowerExclusive = currentPlaylist.lastOption.map(_.orderToken),
      higherExclusive = None)
    val modifications = for ((song, orderToken) <- songs.toVector zip orderTokens)
      yield
        EntityModification.createAddWithRandomId(
          PlaylistEntry(songId = song.id, orderToken = orderToken, userId = user.id))
    await(entityAccess.persistModifications(modifications))
  }

  def removeEntries(playlistEntries: Iterable[PlaylistEntry]): Future[Unit] = {
    val modifications = playlistEntries.toVector.map(EntityModification.createDelete[PlaylistEntry])
    entityAccess.persistModifications(modifications)
  }

  // **************** Implementation of base class methods **************** //
  override protected def calculateState(): Future[State] = async {
    val entries =
      await(entityAccess.newQuery[PlaylistEntry]().filter(ModelField.PlaylistEntry.userId === user.id).data())
        .sortBy(e => (e.orderToken, e.id))
    State(entries = entries)
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == EntityType.PlaylistEntryType
}
object PlaylistStore {
  case class State(entries: Seq[PlaylistEntry])
}
