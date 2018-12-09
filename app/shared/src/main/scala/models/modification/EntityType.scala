package models.modification

import common.ScalaUtils
import models.Entity
import models.media.{Album, Artist, Song, PlaylistEntry, PlayStatus}
import models.user.User

import scala.collection.immutable.Seq

/** Enumeration of all entity types that are transfered between server and client. */
sealed trait EntityType[E <: Entity] {
  type get = E

  def entityClass: Class[E]

  def checkRightType(entity: Entity): get = {
    require(
      entity.getClass == entityClass,
      s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass")
    entity.asInstanceOf[E]
  }

  def name: String = ScalaUtils.objectName(this)
  override def toString = name
}
object EntityType {
  type any = EntityType[_ <: Entity]

  // @formatter:off
  implicit case object UserType extends EntityType[User] { override def entityClass = classOf[User]}
  implicit case object SongType extends EntityType[Song] { override def entityClass = classOf[Song]}
  implicit case object AlbumType extends EntityType[Album] { override def entityClass = classOf[Album]}
  implicit case object ArtistType extends EntityType[Artist] { override def entityClass = classOf[Artist]}
  implicit case object PlaylistEntryType extends EntityType[PlaylistEntry] { override def entityClass = classOf[PlaylistEntry]}
  implicit case object PlayStatusType extends EntityType[PlayStatus] { override def entityClass = classOf[PlayStatus]}
  // @formatter:on

  val values: Seq[EntityType.any] = Seq(UserType, SongType, AlbumType, ArtistType)
}
