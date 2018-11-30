package models.slick

import java.nio.ByteBuffer
import java.time.Instant

import api.Picklers._
import boopickle.Default.{Pickle, Unpickle}
import common.{OrderToken, Tags}
import common.time.LocalDateTime
import models.Entity
import models.media.{Album, Artist, Song}
import models.modification.{EntityModification, EntityModificationEntity}
import models.slick.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import models.slick.SlickUtils.localDateTimeToSqlDateMapper
import models.slick.SlickUtils.instantToSqlTimestampMapper
import models.slick.SlickUtils.finiteDurationToMillisMapper
import models.slick.SlickUtils.orderTokenToBytesMapper
import models.user.User

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

sealed trait SlickEntityTableDef[E <: Entity] {
  type Table <: SlickEntityTableDef.EntityTable[E]
  def tableName: String
  def table(tag: SlickTag): Table
}

object SlickEntityTableDef {

  val all: Seq[SlickEntityTableDef[_]] =
    Seq(UserDef, SongDef, AlbumDef, ArtistDef, EntityModificationEntityDef)

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
      def albumId = column[Long]("albumId")
      def title = column[String]("title")
      def trackNumber = column[Int]("trackNumber")
      def duration = column[FiniteDuration]("duration")
      def year = column[Int]("year")
      def disc = column[Int]("disc")

      override def * =
        (albumId, title, trackNumber, duration, year, disc, id.?) <> (Song.tupled, Song.unapply)
    }
  }

  implicit object AlbumDef extends SlickEntityTableDef[Album] {

    override val tableName: String = "ALBUMS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[Album](tag, tableName) {
      def artistId = column[Long]("artistId")
      def title = column[String]("title")

      override def * =
        (artistId, title, id.?) <> (Album.tupled, Album.unapply)
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
