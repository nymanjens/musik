package hydro.models

import java.time.Instant

import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.access.ModelField

import scala.collection.immutable.Seq

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

  case class LastUpdateTime(timePerField: Map[ModelField.any, Instant], otherFieldsTime: Option[Instant]) {
    def merge(that: LastUpdateTime): LastUpdateTime = {
      // TODO: Implement merge by taking most recent times
      that
    }
  }
  object LastUpdateTime {
    val neverUpdated: LastUpdateTime = LastUpdateTime(Map(), None)
    def allFieldsUpdated(time: Instant): LastUpdateTime =
      LastUpdateTime(timePerField = Map(), otherFieldsTime = Some(time))
    def someFieldsUpdated(fields: Seq[ModelField.any], time: Instant): LastUpdateTime =
      LastUpdateTime(timePerField = fields.map(_ -> time).toMap, otherFieldsTime = None)
  }
}
