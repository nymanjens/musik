package hydro.models

import java.time.Instant

import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.access.ModelField

/**
  * Extension of Entity that allows safe* client updates via lastUpdateTime.
  *
  * *Safe means that EntityModifications can be re-ordered but all clients will eventually have the same
  * view on the entity.
  */
trait UpdatableEntity extends Entity {

  def lastUpdateTime: LastUpdateTime

  /** Returns a copy of this Entity with the given LastUpdateTime. */
  def withLastUpdateTime(time: LastUpdateTime): Entity
}
object UpdatableEntity {

  def withLastUpdateTime[E <: UpdatableEntity](time: LastUpdateTime, entity: E): E =
    entity.withLastUpdateTime(time).asInstanceOf[E]

  def merge[E <: UpdatableEntity](oldEntity: E, newEntity: E): E = {
    // TODO: Implement merge based on lastUpdateTime
    newEntity
  }

  sealed trait LastUpdateTime {
    def merge(lastUpdateTime: LastUpdateTime): LastUpdateTime = ???
  }
  object LastUpdateTime {
    object NeverUpdated extends LastUpdateTime
    case class AllFields(time: Instant) extends LastUpdateTime
    case class PerField(timePerField: Map[ModelField.any, Instant]) extends LastUpdateTime
  }
}
