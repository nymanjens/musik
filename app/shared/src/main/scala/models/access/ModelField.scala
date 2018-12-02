package models.access

import common.GuavaReplacement.ImmutableBiMap
import common.OrderToken
import common.time.LocalDateTime
import models.Entity
import models.access.ModelField.FieldType
import models.media.{Album, Artist, Song}
import models.modification.EntityType
import models.modification.EntityType._
import models.user.User

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

/**
  * Represents a field in an model entity.
  *
  * @param name A name for this field that is unique in E
  * @tparam V The type of the values
  * @tparam E The type corresponding to the entity that contains this field
  */
sealed abstract class ModelField[V, E] private[access] (val name: String, accessor: E => V)(
    implicit val fieldType: FieldType[V]) {

  def get(entity: E): V = accessor(entity)
}

object ModelField {

  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case UserType   => User.id.asInstanceOf[ModelField[Long, E]]
    case SongType   => Song.id.asInstanceOf[ModelField[Long, E]]
    case AlbumType  => Album.id.asInstanceOf[ModelField[Long, E]]
    case ArtistType => Artist.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Related types **************** //
  sealed trait FieldType[T]
  object FieldType {
    implicit case object BooleanType extends FieldType[Boolean]
    implicit case object IntType extends FieldType[Int]
    implicit case object MaybeIntType extends FieldType[Option[Int]]
    implicit case object LongType extends FieldType[Long]
    implicit case object MaybeLongType extends FieldType[Option[Long]]
    implicit case object DoubleType extends FieldType[Double]
    implicit case object StringType extends FieldType[String]
    implicit case object LocalDateTimeType extends FieldType[LocalDateTime]
    implicit case object MaybeLocalDateTimeType extends FieldType[Option[LocalDateTime]]
    implicit case object FiniteDurationType extends FieldType[FiniteDuration]
    implicit case object StringSeqType extends FieldType[Seq[String]]
    implicit case object OrderTokenType extends FieldType[OrderToken]
  }

  abstract sealed class IdModelField[E <: Entity] extends ModelField[Long, E]("id", _.idOption getOrElse -1)

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
    case object year extends ModelField[Option[Int], E]("year", _.year)
    case object disc extends ModelField[Int, E]("disc", _.disc)
  }

  object Album {
    private type E = Album

    case object id extends IdModelField[E]
    case object relativePath extends ModelField[String, E]("relativePath", _.relativePath)
    case object title extends ModelField[String, E]("title", _.title)
    case object artistId extends ModelField[Option[Long], E]("artistId", _.artistId)
  }

  object Artist {
    private type E = Artist

    case object id extends IdModelField[E]
    case object name extends ModelField[String, E]("name", _.name)
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
      Song.year,
      Song.disc,
      Album.id,
      Album.relativePath,
      Album.title,
      Album.artistId,
      Artist.id,
      Artist.name
    )
  def toNumber(field: ModelField[_, _]): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField[_, _] = fieldToNumberMap.inverse().get(number)

  private def toBiMapWithUniqueValues(fields: ModelField[_, _]*): ImmutableBiMap[ModelField[_, _], Int] = {
    val resultBuilder = ImmutableBiMap.builder[ModelField[_, _], Int]()
    for ((field, index) <- fields.zipWithIndex) {
      resultBuilder.put(field, index + 1)
    }
    resultBuilder.build()
  }
}
