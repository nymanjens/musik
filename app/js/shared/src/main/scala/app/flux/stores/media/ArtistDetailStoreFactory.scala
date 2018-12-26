package app.flux.stores.media

import app.models.access.DbQueryImplicits._
import hydro.flux.stores.StoreFactory
import app.flux.stores.media.ArtistDetailStoreFactory.State
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.StoreFactory
import app.models.access.JsEntityAccess
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

final class ArtistDetailStoreFactory(implicit entityAccess: JsEntityAccess, user: User) extends StoreFactory {

  // **************** Public API **************** //
  def get(artistId: Long): Store = getCachedOrCreate(artistId)

  // **************** Implementation of base class methods and types **************** //
  /* override */
  protected type Input = Long

  /* override */
  final class Store(artistId: Long) extends AsyncEntityDerivedStateStore[State] {
    // **************** Implementation of base class methods **************** //
    override protected def calculateState(): Future[State] = async {
      val artist = await(JsArtist.fromEntityId(artistId))
      val albums =
        await(entityAccess.newQuery[Album]().filter(ModelField.Album.artistId === Some(artistId)).data())
          .sortBy(album => (album.year, album.title.toLowerCase))
      val albumIds = albums.map(_.id)
      val songsWithoutAlbum =
        await(
          entityAccess
            .newQuery[Song]()
            .filter(ModelField.Song.artistId === Some(artistId))
            .filter(ModelField.Song.albumId isNoneOf albumIds)
            .data())
          .sortWith((a, b) => (a.title compareToIgnoreCase b.title) < 0)

      State(
        artist = artist,
        albums = albums.map(a => JsAlbum.fromEntities(a, artist)),
        songsWithoutAlbum = await(Future.sequence(songsWithoutAlbum.map(JsSong.fromEntity)))
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
object ArtistDetailStoreFactory {
  case class State(artist: JsArtist, albums: Seq[JsAlbum], songsWithoutAlbum: Seq[JsSong])
}
