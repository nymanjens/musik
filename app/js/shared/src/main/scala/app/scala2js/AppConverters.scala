package app.scala2js

import app.models._
import app.models.access.ModelFields
import hydro.models.access.ModelField
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
      ModelFields.User.loginName,
      ModelFields.User.passwordHash,
      ModelFields.User.name,
      ModelFields.User.isAdmin,
    ),
    toScalaWithoutId = dict =>
      User(
        loginName = dict.getRequired(ModelFields.User.loginName),
        passwordHash = dict.getRequired(ModelFields.User.passwordHash),
        name = dict.getRequired(ModelFields.User.name),
        isAdmin = dict.getRequired(ModelFields.User.isAdmin)
    )
  )

  implicit val SongConverter: EntityConverter[Song] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Song.filename,
      ModelFields.Song.title,
      ModelFields.Song.albumId,
      ModelFields.Song.artistId,
      ModelFields.Song.trackNumber,
      ModelFields.Song.duration,
      ModelFields.Song.disc,
    ),
    toScalaWithoutId = dict =>
      Song(
        filename = dict.getRequired(ModelFields.Song.filename),
        title = dict.getRequired(ModelFields.Song.title),
        albumId = dict.getRequired(ModelFields.Song.albumId),
        artistId = dict.getRequired(ModelFields.Song.artistId),
        trackNumber = dict.getRequired(ModelFields.Song.trackNumber),
        duration = dict.getRequired(ModelFields.Song.duration),
        disc = dict.getRequired(ModelFields.Song.disc)
    )
  )
  implicit val AlbumConverter: EntityConverter[Album] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Album.relativePath,
      ModelFields.Album.title,
      ModelFields.Album.artistId,
      ModelFields.Album.year,
    ),
    toScalaWithoutId = dict =>
      Album(
        relativePath = dict.getRequired(ModelFields.Album.relativePath),
        title = dict.getRequired(ModelFields.Album.title),
        artistId = dict.getRequired(ModelFields.Album.artistId),
        year = dict.getRequired(ModelFields.Album.year)
    )
  )
  implicit val ArtistConverter: EntityConverter[Artist] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Artist.name,
    ),
    toScalaWithoutId = dict => Artist(name = dict.getRequired(ModelFields.Artist.name))
  )
  implicit val PlaylistEntryConverter: EntityConverter[PlaylistEntry] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.PlaylistEntry.songId,
      ModelFields.PlaylistEntry.orderToken,
      ModelFields.PlaylistEntry.userId,
    ),
    toScalaWithoutId = dict =>
      PlaylistEntry(
        songId = dict.getRequired(ModelFields.PlaylistEntry.songId),
        orderToken = dict.getRequired(ModelFields.PlaylistEntry.orderToken),
        userId = dict.getRequired(ModelFields.PlaylistEntry.userId)
    )
  )
  implicit val PlayStatusConverter: EntityConverter[PlayStatus] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.PlayStatus.currentPlaylistEntryId,
      ModelFields.PlayStatus.hasStarted,
      ModelFields.PlayStatus.stopAfterCurrentSong,
      ModelFields.PlayStatus.userId,
    ),
    toScalaWithoutId = dict =>
      PlayStatus(
        currentPlaylistEntryId = dict.getRequired(ModelFields.PlayStatus.currentPlaylistEntryId),
        hasStarted = dict.getRequired(ModelFields.PlayStatus.hasStarted),
        stopAfterCurrentSong = dict.getRequired(ModelFields.PlayStatus.stopAfterCurrentSong),
        userId = dict.getRequired(ModelFields.PlayStatus.userId)
    )
  )
}
