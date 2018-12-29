package app.scala2js

import app.models._
import app.models.access.ModelField
import app.models.media._
import app.models.modification._
import app.models.user.User
import hydro.scala2js.Scala2Js.Converter
import hydro.scala2js.Scala2Js.MapConverter
import hydro.scala2js.StandardConverters
import hydro.scala2js.StandardConverters.EntityConverter

import scala.collection.immutable.Seq

object AppConverters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: MapConverter[_ <: Entity] = entityType match {
      case EntityType.UserType          => UserConverter
      case EntityType.SongType          => SongConverter
      case EntityType.AlbumType         => AlbumConverter
      case EntityType.ArtistType        => ArtistConverter
      case EntityType.PlaylistEntryType => PlaylistEntryConverter
      case EntityType.PlayStatusType    => PlayStatusConverter
    }
    converter.asInstanceOf[MapConverter[E]]
  }

  // **************** General converters **************** //
  implicit val EntityTypeConverter: Converter[EntityType.any] =
    StandardConverters.enumConverter(
      EntityType.UserType,
      EntityType.SongType,
      EntityType.AlbumType,
      EntityType.ArtistType,
      EntityType.PlaylistEntryType,
      EntityType.PlayStatusType)

  // **************** Entity converters **************** //
  implicit val UserConverter: EntityConverter[User] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.User.loginName,
      ModelField.User.passwordHash,
      ModelField.User.name,
      ModelField.User.isAdmin,
    ),
    toScalaWithoutId = dict =>
      User(
        loginName = dict.getRequired(ModelField.User.loginName),
        passwordHash = dict.getRequired(ModelField.User.passwordHash),
        name = dict.getRequired(ModelField.User.name),
        isAdmin = dict.getRequired(ModelField.User.isAdmin)
    )
  )

  implicit val SongConverter: EntityConverter[Song] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.Song.filename,
      ModelField.Song.title,
      ModelField.Song.albumId,
      ModelField.Song.artistId,
      ModelField.Song.trackNumber,
      ModelField.Song.duration,
      ModelField.Song.disc,
    ),
    toScalaWithoutId = dict =>
      Song(
        filename = dict.getRequired(ModelField.Song.filename),
        title = dict.getRequired(ModelField.Song.title),
        albumId = dict.getRequired(ModelField.Song.albumId),
        artistId = dict.getRequired(ModelField.Song.artistId),
        trackNumber = dict.getRequired(ModelField.Song.trackNumber),
        duration = dict.getRequired(ModelField.Song.duration),
        disc = dict.getRequired(ModelField.Song.disc)
    )
  )
  implicit val AlbumConverter: EntityConverter[Album] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.Album.relativePath,
      ModelField.Album.title,
      ModelField.Album.artistId,
      ModelField.Album.year,
    ),
    toScalaWithoutId = dict =>
      Album(
        relativePath = dict.getRequired(ModelField.Album.relativePath),
        title = dict.getRequired(ModelField.Album.title),
        artistId = dict.getRequired(ModelField.Album.artistId),
        year = dict.getRequired(ModelField.Album.year)
    )
  )
  implicit val ArtistConverter: EntityConverter[Artist] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.Artist.name,
    ),
    toScalaWithoutId = dict => Artist(name = dict.getRequired(ModelField.Artist.name))
  )
  implicit val PlaylistEntryConverter: EntityConverter[PlaylistEntry] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.PlaylistEntry.songId,
      ModelField.PlaylistEntry.orderToken,
      ModelField.PlaylistEntry.userId,
    ),
    toScalaWithoutId = dict =>
      PlaylistEntry(
        songId = dict.getRequired(ModelField.PlaylistEntry.songId),
        orderToken = dict.getRequired(ModelField.PlaylistEntry.orderToken),
        userId = dict.getRequired(ModelField.PlaylistEntry.userId)
    )
  )
  implicit val PlayStatusConverter: EntityConverter[PlayStatus] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelField.PlayStatus.currentPlaylistEntryId,
      ModelField.PlayStatus.hasStarted,
      ModelField.PlayStatus.stopAfterCurrentSong,
      ModelField.PlayStatus.userId,
    ),
    toScalaWithoutId = dict =>
      PlayStatus(
        currentPlaylistEntryId = dict.getRequired(ModelField.PlayStatus.currentPlaylistEntryId),
        hasStarted = dict.getRequired(ModelField.PlayStatus.hasStarted),
        stopAfterCurrentSong = dict.getRequired(ModelField.PlayStatus.stopAfterCurrentSong),
        userId = dict.getRequired(ModelField.PlayStatus.userId)
    )
  )
}
