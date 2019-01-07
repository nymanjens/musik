package app.flux.stores.media

import app.flux.stores.media.ArtistDetailStoreFactory.State
import app.models.access.ModelFields
import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import app.models.media._
import hydro.models.modification.EntityModification
import app.models.user.User
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.StoreFactory
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess

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
        await(entityAccess.newQuery[Album]().filter(ModelFields.Album.artistId === Some(artistId)).data())
          .sortBy(album => (album.year, album.title.toLowerCase))
      val albumIds = albums.map(_.id)
      val songsWithoutAlbum =
        await(
          entityAccess
            .newQuery[Song]()
            .filter(ModelFields.Song.artistId === Some(artistId))
            .filter(ModelFields.Song.albumId isNoneOf albumIds)
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
      entityModification.entityType == Artist.Type ||
        entityModification.entityType == Album.Type ||
        entityModification.entityType == Song.Type
  }

  override def createNew(input: Input): Store = new Store(input)
}
object ArtistDetailStoreFactory {
  case class State(artist: JsArtist, albums: Seq[JsAlbum], songsWithoutAlbum: Seq[JsSong])
}
