package app.models.slick

import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.media.Song
import app.models.slick.SlickEntityTableDef.EntityTable
import app.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import app.models.user.User
import hydro.common.OrderToken
import hydro.models.slick.SlickUtils.dbApi.{Tag => SlickTag}
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.finiteDurationToMillisMapper
import hydro.models.slick.SlickUtils.orderTokenToBytesMapper

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

object SlickEntityTableDefs {

  val all: Seq[SlickEntityTableDef[_]] =
    Seq(UserDef, SongDef, AlbumDef, ArtistDef, PlaylistEntryDef, PlayStatusDef, EntityModificationEntityDef)


  implicit object UserDef extends SlickEntityTableDef[User] {

    override val tableName: String = "USERS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[User](tag, tableName) {
      def loginName = column[String]("loginName")
      def passwordHash = column[String]("passwordHash")
      def name = column[String]("name")
      def isAdmin = column[Boolean]("isAdmin")

      override def * =
        (loginName, passwordHash, name, isAdmin, id.?) <> (User.tupled, User.unapply)
    }
  }

  implicit object SongDef extends SlickEntityTableDef[Song] {

    override val tableName: String = "SONGS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[Song](tag, tableName) {
      def filename = column[String]("filename")
      def title = column[String]("title")
      def albumId = column[Long]("albumId")
      def artistId = column[Option[Long]]("artistId")
      def trackNumber = column[Int]("trackNumber")
      def duration = column[FiniteDuration]("duration")
      def disc = column[Int]("disc")

      override def * =
        (filename, title, albumId, artistId, trackNumber, duration, disc, id.?) <> (Song.tupled, Song.unapply)
    }
  }

  implicit object AlbumDef extends SlickEntityTableDef[Album] {

    override val tableName: String = "ALBUMS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[Album](tag, tableName) {
      def relativePath = column[String]("relativePath")
      def title = column[String]("title")
      def artistId = column[Option[Long]]("artistId")
      def year = column[Option[Int]]("year")

      override def * =
        (relativePath, title, artistId, year, id.?) <> (Album.tupled, Album.unapply)
    }
  }

  implicit object ArtistDef extends SlickEntityTableDef[Artist] {

    override val tableName: String = "ARTISTS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[Artist](tag, tableName) {
      def name = column[String]("name")

      override def * =
        (name, id.?) <> (Artist.tupled, Artist.unapply)
    }
  }

  implicit object PlaylistEntryDef extends SlickEntityTableDef[PlaylistEntry] {

    override val tableName: String = "PLAYLIST_ENTRIES"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[PlaylistEntry](tag, tableName) {
      def songId = column[Long]("songId")
      def orderToken = column[OrderToken]("orderToken")
      def userId = column[Long]("userId")

      override def * =
        (songId, orderToken, userId, id.?) <> (PlaylistEntry.tupled, PlaylistEntry.unapply)
    }
  }

  implicit object PlayStatusDef extends SlickEntityTableDef[PlayStatus] {

    override val tableName: String = "PLAY_STATUSES"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[PlayStatus](tag, tableName) {
      def currentPlaylistEntryId = column[Long]("currentPlaylistEntryId")
      def hasStarted = column[Boolean]("hasStarted")
      def stopAfterCurrentSong = column[Boolean]("stopAfterCurrentSong")
      def userId = column[Long]("userId")

      override def * =
        (currentPlaylistEntryId, hasStarted, stopAfterCurrentSong, userId, id.?) <> (PlayStatus.tupled, PlayStatus.unapply)
    }
  }
}
