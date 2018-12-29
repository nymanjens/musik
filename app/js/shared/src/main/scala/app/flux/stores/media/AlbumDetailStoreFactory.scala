package app.flux.stores.media

import app.flux.stores.media.AlbumDetailStoreFactory.State
import hydro.flux.stores.StoreFactory
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.StoreFactory
import app.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import app.models.access.ModelField
import app.models.media._
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AlbumDetailStoreFactory(implicit entityAccess: JsEntityAccess, user: User) extends StoreFactory {

  // **************** Public API **************** //
  def get(albumId: Long): Store = getCachedOrCreate(albumId)

  // **************** Implementation of base class methods and types **************** //
  /* override */
  protected type Input = Long

  /* override */
  final class Store(albumId: Long) extends AsyncEntityDerivedStateStore[State] {
    // **************** Implementation of base class methods **************** //
    override protected def calculateState(): Future[State] = async {
      val album = await(JsAlbum.fromEntityId(albumId))
      val songs =
        await(entityAccess.newQuery[Song]().filter(ModelField.Song.albumId === albumId).data())
          .sortBy(_.trackNumber)

      State(
        album = album,
        songs = await(Future.sequence(songs.map(JsSong.fromEntity)))
      )
    }

    override protected def modificationImpactsState(entityModification: EntityModification,
                                                    state: State): Boolean =
      entityModification.entityType == EntityType.ArtistType ||
        entityModification.entityType == EntityType.AlbumType ||
        entityModification.entityType == EntityType.SongType
  }

  override def createNew(input: Input): Store = new Store(input)
}
object AlbumDetailStoreFactory {
  case class State(album: JsAlbum, songs: Seq[JsSong])
}
