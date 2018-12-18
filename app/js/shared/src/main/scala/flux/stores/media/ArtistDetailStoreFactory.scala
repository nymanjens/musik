package flux.stores.media

import models.access.DbQueryImplicits._
import flux.stores.{AsyncEntityDerivedStateStore, StoreFactory}
import flux.stores.media.ArtistDetailStoreFactory.State
import models.access.{JsEntityAccess, ModelField}
import models.media._
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
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
          .sortWith((a, b) => (a.title compareToIgnoreCase b.title) < 0)
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
