package controllers.helpers.media

import models.access.DbQueryImplicits._
import com.google.inject.Inject
import common.RelativePaths
import common.time.Clock
import controllers.helpers.media.AlbumParser.ParsedAlbum
import models.Entity
import models.access.{JvmEntityAccess, ModelField}
import models.media.{Album, Artist, Song}
import models.modification.{EntityModification, EntityType}
import models.user.{User, Users}

import scala.collection.immutable.Seq

final class StoredMediaSyncer @Inject()(implicit entityAccess: JvmEntityAccess, clock: Clock) {

  def addEntitiesFromParsedAlbums(albums: Seq[ParsedAlbum]): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    for (album <- albums) {
      val albumId = fetchOrAddAlbum(album).id
      for (song <- album.songs) {
        val artistId = song.canonicalArtistName.map(name => fetchOrAddArtist(name).id)

        entityAccess.persistEntityModifications(
          EntityModification.createAddWithRandomId(Song(
            filename = song.filename,
            title = song.title,
            albumId = albumId,
            artistId = artistId,
            trackNumber = song.trackNumber,
            duration = song.duration,
            year = song.year,
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
        entityAccess.newQuerySync[Song]().filter(ModelField.Song.albumId === album.id).data()
      if (songChildren.isEmpty) {
        entityAccess.persistEntityModifications(EntityModification.createDelete(album))
      }
    }

    for (artistId <- affectedArtistIds) {
      val albumChildren =
        entityAccess.newQuerySync[Album]().filter(ModelField.Album.artistId === Some(artistId)).data()
      val songChildren =
        entityAccess.newQuerySync[Song]().filter(ModelField.Song.artistId === Some(artistId)).data()
      if (albumChildren.isEmpty && songChildren.isEmpty) {
        entityAccess.persistEntityModifications(EntityModification.Remove[Artist](artistId))
      }
    }
  }

  private def fetchSong(relativePath: String): Song = {
    val album = maybeFetchAlbum(RelativePaths.getFolderPath(relativePath)).get
    entityAccess
      .newQuerySync[Song]()
      .filter(ModelField.Song.albumId === album.id)
      .findOne(ModelField.Song.filename, RelativePaths.getFilename(relativePath))
      .get
  }

  private def fetchAlbum(id: Long): Album = entityAccess.newQuerySync[Album]().findById(id)
  private def maybeFetchAlbum(relativePath: String): Option[Album] = {
    entityAccess.newQuerySync[Album]().findOne(ModelField.Album.relativePath, relativePath)
  }

  private def fetchOrAddAlbum(album: ParsedAlbum)(implicit user: User): Album = {
    val artistId = album.canonicalArtistName.map(name => fetchOrAddArtist(name).id)

    fetchOrAddEntity(
      fetchExistingEntity = () => maybeFetchAlbum(album.relativePath),
      entityWithoutId = Album(relativePath = album.relativePath, title = album.title, artistId = artistId)
    )
  }

  private def fetchOrAddArtist(canonicalArtistName: String)(implicit user: User): Artist = fetchOrAddEntity(
    fetchExistingEntity =
      () => entityAccess.newQuerySync[Artist]().findOne(ModelField.Artist.name, canonicalArtistName),
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
