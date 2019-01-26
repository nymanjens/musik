package hydro.models

import java.time.Instant

import hydro.models.Entity.LastUpdateTime
import hydro.models.access.ModelField

// Based on active-slick (https://github.com/strongtyped/active-slick)

/** Base trait to define a model having an ID (i.e.: Entity). */
trait Entity {

  /** Returns the Entity ID */
  final def id: Long = idOption.getOrElse(throw new IllegalStateException(s"This entity has no ID: $this"))

  /**
    * The Entity ID wrapped in an Option.
    * Expected to be None when Entity not yet persisted, otherwise Some[Id].
    */
  def idOption: Option[Long]

  def lastUpdateTime: LastUpdateTime

  /** Returns a copy of this Entity with an ID. */
  def withId(id: Long): Entity
}

object Entity {
  def asEntity(entity: Entity): Entity = entity

  def withId[E <: Entity](id: Long, entity: E): E = entity.withId(id).asInstanceOf[E]

  sealed trait LastUpdateTime
  object LastUpdateTime {
    object NeverUpdated extends LastUpdateTime
    case class AllFields(time: Instant) extends LastUpdateTime
    case class PerField(timePerField: Map[ModelField[_, _], Instant]) extends LastUpdateTime
  }
}
