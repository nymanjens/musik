package app.flux.stores.media

import app.flux.stores.media.helpers.ComplexQueryFilterFactory
import app.flux.stores.media.ComplexQueryStoreFactory.Query
import app.flux.stores.media.ComplexQueryStoreFactory.State
import app.models.access.ModelFields
import app.models.media.Album
import app.models.media.Artist
import app.models.media.JsAlbum
import app.models.media.JsArtist
import app.models.media.JsSong
import app.models.media.Song
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.StoreFactory
import hydro.models.access.DbQuery
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class ComplexQueryStoreFactory(implicit entityAccess: JsEntityAccess,
                                     complexQueryFilterFactory: ComplexQueryFilterFactory)
    extends StoreFactory {

  // **************** Public API **************** //
  def get(query: String): Store = getCachedOrCreate(query)

  // **************** Implementation of base class methods and types **************** //
  /* override */
  protected type Input = Query

  /* override */
  final class Store(query: Query) extends AsyncEntityDerivedStateStore[State] {
    private val complexQueryFilter = complexQueryFilterFactory.fromQuery(query)

    override protected def calculateState(): Future[State] = async {
      State(
        artists = toJsArtists(
          await(
            entityAccess
              .newQuery[Artist]()
              .filter(await(complexQueryFilter.getArtistFilter()))
              .sort(DbQuery.Sorting.ascBy(ModelFields.Artist.name))
              .limit(20)
              .data()))
          .sortBy(artist => artist.name.toLowerCase),
        albums = await(
          toJsAlbums(
            await(
              entityAccess
                .newQuery[Album]()
                .filter(await(complexQueryFilter.getAlbumFilter()))
                .sort(DbQuery.Sorting
                  .ascBy(ModelFields.Album.artistId)
                  .thenAscBy(ModelFields.Album.year)
                  .thenAscBy(ModelFields.Album.title))
                .limit(30)
                .data())))
          .sortBy(
            album =>
              (
                album.artist.map(_.name.toLowerCase),
                album.year,
                album.title.toLowerCase
            )),
        songs = await(
          toJsSongs(
            await(
              entityAccess
                .newQuery[Song]()
                .filter(await(complexQueryFilter.getSongFilter()))
                .sort(DbQuery.Sorting.ascBy(ModelFields.Song.albumId).thenAscBy(ModelFields.Song.trackNumber))
                .limit(100)
                .data())))
          .sortBy(
            song =>
              (
                song.album.artist.map(_.name.toLowerCase),
                song.album.year,
                song.album.title.toLowerCase,
                song.trackNumber,
                song.title.toLowerCase)),
      )
    }

    override protected def modificationImpactsState(entityModification: EntityModification,
                                                    state: State): Boolean =
      entityModification.entityType == Artist.Type ||
        entityModification.entityType == Album.Type ||
        entityModification.entityType == Song.Type
  }

  override def createNew(input: Input): Store = new Store(input)

  private def toJsArtists(artists: Seq[Artist]): Seq[JsArtist] = artists map JsArtist.fromEntity
  private def toJsAlbums(albums: Seq[Album]): Future[Seq[JsAlbum]] =
    Future.sequence(albums map JsAlbum.fromEntity)
  private def toJsSongs(songs: Seq[Song]): Future[Seq[JsSong]] = Future.sequence(songs map JsSong.fromEntity)
}

object ComplexQueryStoreFactory {
  type Query = String
  case class State(artists: Seq[JsArtist], albums: Seq[JsAlbum], songs: Seq[JsSong])
}
