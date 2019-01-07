package app.models.access

import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.OrderToken
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.models.access.ModelField.IdModelField
import hydro.models.access.ModelField.toBiMapWithUniqueValues

import scala.concurrent.duration.FiniteDuration

object ModelFields {
  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case app.models.user.User.Type           => User.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Song.Type          => Song.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Album.Type         => Album.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Artist.Type        => Artist.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.PlaylistEntry.Type => PlaylistEntry.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.PlayStatus.Type    => PlayStatus.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Enumeration of all fields **************** //
  object User {
    private type E = User

    case object id extends IdModelField[E]
    case object loginName extends ModelField[String, E]("loginName", _.loginName)
    case object passwordHash extends ModelField[String, E]("passwordHash", _.passwordHash)
    case object name extends ModelField[String, E]("name", _.name)
    case object isAdmin extends ModelField[Boolean, E]("isAdmin", _.isAdmin)
  }

  object Song {
    private type E = Song

    case object id extends IdModelField[E]
    case object filename extends ModelField[String, E]("filename", _.filename)
    case object title extends ModelField[String, E]("title", _.title)
    case object albumId extends ModelField[Long, E]("albumId", _.albumId)
    case object artistId extends ModelField[Option[Long], E]("artistId", _.artistId)
    case object trackNumber extends ModelField[Int, E]("trackNumber", _.trackNumber)
    case object duration extends ModelField[FiniteDuration, E]("duration", _.duration)
    case object disc extends ModelField[Int, E]("disc", _.disc)
  }

  object Album {
    private type E = Album

    case object id extends IdModelField[E]
    case object relativePath extends ModelField[String, E]("relativePath", _.relativePath)
    case object title extends ModelField[String, E]("title", _.title)
    case object artistId extends ModelField[Option[Long], E]("artistId", _.artistId)
    case object year extends ModelField[Option[Int], E]("year", _.year)
  }

  object Artist {
    private type E = Artist

    case object id extends IdModelField[E]
    case object name extends ModelField[String, E]("name", _.name)
  }

  object PlaylistEntry {
    private type E = PlaylistEntry

    case object id extends IdModelField[E]
    case object songId extends ModelField[Long, E]("songId", _.songId)
    case object orderToken extends ModelField[OrderToken, E]("orderToken", _.orderToken)
    case object userId extends ModelField[Long, E]("userId", _.userId)
  }

  object PlayStatus {
    private type E = PlayStatus

    case object id extends IdModelField[E]
    case object currentPlaylistEntryId
        extends ModelField[Long, E]("currentPlaylistEntryId", _.currentPlaylistEntryId)
    case object hasStarted extends ModelField[Boolean, E]("hasStarted", _.hasStarted)
    case object stopAfterCurrentSong
        extends ModelField[Boolean, E]("stopAfterCurrentSong", _.stopAfterCurrentSong)
    case object userId extends ModelField[Long, E]("userId", _.userId)
  }

  // **************** Field numbers **************** //
  private val fieldToNumberMap: ImmutableBiMap[ModelField[_, _], Int] =
    toBiMapWithUniqueValues(
      User.id,
      User.loginName,
      User.passwordHash,
      User.name,
      User.isAdmin,
      Song.id,
      Song.filename,
      Song.title,
      Song.albumId,
      Song.artistId,
      Song.trackNumber,
      Song.duration,
      Song.disc,
      Album.id,
      Album.relativePath,
      Album.title,
      Album.artistId,
      Album.year,
      Artist.id,
      Artist.name,
      PlaylistEntry.id,
      PlaylistEntry.songId,
      PlaylistEntry.orderToken,
      PlaylistEntry.userId,
      PlayStatus.id,
      PlayStatus.currentPlaylistEntryId,
      PlayStatus.hasStarted,
      PlayStatus.stopAfterCurrentSong,
      PlayStatus.userId
    )
  def toNumber(field: ModelField[_, _]): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField[_, _] = fieldToNumberMap.inverse().get(number)
}
