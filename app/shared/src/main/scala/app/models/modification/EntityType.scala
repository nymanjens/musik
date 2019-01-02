package app.models.modification

import app.common.ScalaUtils
import hydro.models.Entity
import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.user.User

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

/** Enumeration of all entity types that are transfered between server and client. */
final class EntityType[E <: Entity](val entityClass: Class[E]) {
  type get = E

  def checkRightType(entity: Entity): get = {
    require(
      entity.getClass == entityClass,
      s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass")
    entity.asInstanceOf[E]
  }

  lazy val name: String = entityClass.getSimpleName + "Type"
  override def toString = name
}
object EntityType {
  type any = EntityType[_ <: Entity]

  def apply[E <: Entity]()(implicit classTag: ClassTag[E]): EntityType[E] =
    new EntityType[E](classTag.runtimeClass.asInstanceOf[Class[E]])

  lazy val values: Seq[EntityType.any] =
    Seq(User.Type, Song.Type, Album.Type, Artist.Type, PlaylistEntry.Type, PlayStatus.Type)
}
