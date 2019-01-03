package app.models.slick

import java.nio.ByteBuffer
import java.time.Instant

import app.api.Picklers._
import app.common.OrderToken
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.modification.EntityModification
import app.models.modification.EntityModificationEntity
import app.models.slick.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import app.models.slick.SlickUtils.finiteDurationToMillisMapper
import app.models.slick.SlickUtils.instantToSqlTimestampMapper
import app.models.slick.SlickUtils.orderTokenToBytesMapper
import app.models.user.User
import boopickle.Default.Pickle
import boopickle.Default.Unpickle
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

sealed trait SlickEntityTableDef[E <: Entity] {
  type Table <: SlickEntityTableDef.EntityTable[E]
  def tableName: String
  def table(tag: SlickTag): Table
}

object SlickEntityTableDef {

  val all: Seq[SlickEntityTableDef[_]] =
    Seq(UserDef, SongDef, AlbumDef, ArtistDef, PlaylistEntryDef, PlayStatusDef, EntityModificationEntityDef)

  /** Table extension to be used with an Entity model. */
  // Based on active-slick (https://github.com/strongtyped/active-slick)
  sealed abstract class EntityTable[E <: Entity](
      tag: SlickTag,
      tableName: String,
      schemaName: Option[String] = None)(implicit val colType: BaseColumnType[Long])
      extends SlickTable[E](tag, schemaName, tableName) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  }

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

  implicit object EntityModificationEntityDef extends SlickEntityTableDef[EntityModificationEntity] {

    override val tableName: String = "ENTITY_MODIFICATION_ENTITY"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[EntityModificationEntity](tag, tableName) {
      def userId = column[Long]("userId")
      def entityId = column[Long]("entityId")
      def change = column[EntityModification]("modification")
      def instant = column[Instant]("date")
      // The instant field can't hold the nano precision of the `instant` field above. It thus
      // has to be persisted separately.
      def instantNanos = column[Long]("instantNanos")

      override def * = {
        def tupled(
            tuple: (Long, Long, EntityModification, Instant, Long, Option[Long])): EntityModificationEntity =
          tuple match {
            case (userId, entityId, modification, instant, instantNanos, idOption) =>
              EntityModificationEntity(
                userId = userId,
                modification = modification,
                instant = Instant.ofEpochSecond(instant.getEpochSecond, instantNanos),
                idOption = idOption
              )
          }
        def unapply(e: EntityModificationEntity)
          : Option[(Long, Long, EntityModification, Instant, Long, Option[Long])] =
          Some((e.userId, e.modification.entityId, e.modification, e.instant, e.instant.getNano, e.idOption))

        (userId, entityId, change, instant, instantNanos, id.?) <> (tupled _, unapply _)
      }
    }

    implicit val entityModificationToBytesMapper: ColumnType[EntityModification] = {
      def toBytes(modification: EntityModification) = {
        val byteBuffer = Pickle.intoBytes(modification)

        val byteArray = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(byteArray)
        byteArray
      }
      def toEntityModification(bytes: Array[Byte]) = {
        val byteBuffer = ByteBuffer.wrap(bytes)
        Unpickle[EntityModification].fromBytes(byteBuffer)
      }
      MappedColumnType.base[EntityModification, Array[Byte]](toBytes, toEntityModification)
    }
  }
}
