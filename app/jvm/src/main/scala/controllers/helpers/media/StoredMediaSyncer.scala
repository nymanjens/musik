package controllers.helpers.media

import hydro.models.access.DbQueryImplicits._
import com.google.inject.Inject
import app.common.RelativePaths
import hydro.common.time.Clock
import controllers.helpers.media.AlbumParser.ParsedAlbum
import app.models.Entity
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.user.User
import app.models.user.Users

import scala.collection.immutable.Seq

final class StoredMediaSyncer @Inject()(implicit entityAccess: JvmEntityAccess, clock: Clock) {

  def addEntitiesFromParsedAlbums(albums: Seq[ParsedAlbum]): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    for (album <- albums) {
      val albumId = fetchOrAddAlbum(album).id
      for (song <- album.songs) {
        val artistId = song.canonicalArtistName.map(name => fetchOrAddArtist(name).id)

        entityAccess.persistEntityModifications(
          EntityModification.createAddWithRandomId(
            Song(
              filename = song.filename,
              title = song.title,
              albumId = albumId,
              artistId = artistId,
              trackNumber = song.trackNumber,
              duration = song.duration,
              disc = song.disc
            )))
      }
    }
  }

  def removeEntitiesFromRelativeSongPaths(relativeSongPaths: Seq[String]): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    val songsToRemove = relativeSongPaths.map(fetchSong)
    val affectedAlbums = songsToRemove.map(_.albumId).distinct.map(fetchAlbum)
    val affectedArtistIds = songsToRemove.flatMap(_.artistId) ++ affectedAlbums.flatMap(_.artistId)

    entityAccess.persistEntityModifications(songsToRemove.map(EntityModification.createDelete[Song]))

    for (album <- affectedAlbums) {
      val songChildren =
        entityAccess.newQuerySync[Song]().filter(ModelFields.Song.albumId === album.id).data()
      if (songChildren.isEmpty) {
        entityAccess.persistEntityModifications(EntityModification.createDelete(album))
      }
    }

    for (artistId <- affectedArtistIds) {
      val albumChildren =
        entityAccess.newQuerySync[Album]().filter(ModelFields.Album.artistId === Some(artistId)).data()
      val songChildren =
        entityAccess.newQuerySync[Song]().filter(ModelFields.Song.artistId === Some(artistId)).data()
      if (albumChildren.isEmpty && songChildren.isEmpty) {
        entityAccess.persistEntityModifications(EntityModification.Remove[Artist](artistId))
      }
    }
  }

  private def fetchSong(relativePath: String): Song = {
    val album = maybeFetchAlbum(RelativePaths.getFolderPath(relativePath)).get
    entityAccess
      .newQuerySync[Song]()
      .filter(ModelFields.Song.albumId === album.id)
      .findOne(ModelFields.Song.filename === RelativePaths.getFilename(relativePath))
      .get
  }

  private def fetchAlbum(id: Long): Album = entityAccess.newQuerySync[Album]().findById(id)
  private def maybeFetchAlbum(relativePath: String): Option[Album] = {
    entityAccess.newQuerySync[Album]().findOne(ModelFields.Album.relativePath === relativePath)
  }

  private def fetchOrAddAlbum(album: ParsedAlbum)(implicit user: User): Album = {
    val artistId = album.canonicalArtistName.map(name => fetchOrAddArtist(name).id)

    fetchOrAddEntity(
      fetchExistingEntity = () => maybeFetchAlbum(album.relativePath),
      entityWithoutId =
        Album(relativePath = album.relativePath, title = album.title, artistId = artistId, year = album.year)
    )
  }

  private def fetchOrAddArtist(canonicalArtistName: String)(implicit user: User): Artist = fetchOrAddEntity(
    fetchExistingEntity =
      () => entityAccess.newQuerySync[Artist]().findOne(ModelFields.Artist.name === canonicalArtistName),
    entityWithoutId = Artist(name = canonicalArtistName)
  )

  private def fetchOrAddEntity[E <: Entity: EntityType](fetchExistingEntity: () => Option[E],
                                                        entityWithoutId: E)(implicit user: User): E = {
    fetchExistingEntity() match {
      case Some(entity) => entity
      case None =>
        entityAccess.persistEntityModifications(EntityModification.createAddWithRandomId(entityWithoutId))
        fetchExistingEntity().get
    }
  }
}
