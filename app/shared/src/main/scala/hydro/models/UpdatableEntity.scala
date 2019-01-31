package hydro.models

import hydro.common.time.JavaTimeImplicits._
import java.time.Instant

import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.access.ModelField

import scala.collection.immutable.Seq
import scala.collection.mutable

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
    val (baseEntity, baseTime) =
      (oldEntity.lastUpdateTime.otherFieldsTime, newEntity.lastUpdateTime.otherFieldsTime) match {
        case (None, maybeT2)                  => (newEntity, maybeT2 getOrElse Instant.MIN)
        case (Some(t), None)                  => (oldEntity, t)
        case (Some(t1), Some(t2)) if t1 <= t2 => (newEntity, t2)
        case (Some(t1), Some(t2)) if t1 > t2  => (oldEntity, t1)
      }

    var result: E = baseEntity
    val allFields = oldEntity.lastUpdateTime.timePerField.keySet ++ newEntity.lastUpdateTime.timePerField.keySet
    for (field <- allFields) {
      val oldUpdateTime = oldEntity.lastUpdateTime.timePerField.getOrElse(field, Instant.MIN)
      val newUpdateTime = newEntity.lastUpdateTime.timePerField.getOrElse(field, Instant.MIN)

      if (max(oldUpdateTime, newUpdateTime) >= baseTime) {
        def setFromEntity[V](field: ModelField[V, E], entity: E): Unit = {
          result = field.set(result, field.get(entity))
        }
        setFromEntity(
          field.asInstanceOf[ModelField[_, E]],
          if (oldUpdateTime <= newUpdateTime) newEntity else oldEntity)
      }
    }

    result = result
      .withLastUpdateTime(oldEntity.lastUpdateTime.merge(newEntity.lastUpdateTime, forceIncrement = false))
      .asInstanceOf[E]

    result
  }

  private def max(instants: Instant*): Instant = Seq(instants: _*).max

  case class LastUpdateTime(timePerField: Map[ModelField.any, Instant], otherFieldsTime: Option[Instant]) {

    /**
      * Returns an instance that merges the fields in `this` and `that`.
      *
      * If `forceIncrement` is true: for all instants in `that` that are smaller than their corresponding
      * value in `this` (if any), the returned value will be strictly higher than the highest value. This
      * means that all set fields in `that` force an increment.
      */
    def merge(that: LastUpdateTime, forceIncrement: Boolean): LastUpdateTime = {
      def mergeInstant(thisTime: Option[Instant], thatTime: Option[Instant]): Option[Instant] =
        (thisTime, thatTime) match {
          case (Some(t1), Some(t2)) if t1 < t2 => Some(t2)
          case (Some(t1), Some(t2)) if t1 >= t2 =>
            if (forceIncrement) Some(t1 plusNanos 1) else Some(t1)
          case (Some(t), None) => Some(t)
          case (None, Some(t)) => Some(t)
          case (None, None)    => None
        }
      LastUpdateTime(
        timePerField = {
          val allFields = this.timePerField.keySet ++ that.timePerField.keySet
          for (field <- allFields)
            yield field -> mergeInstant(this.timePerField.get(field), that.timePerField.get(field)).get
        }.toMap,
        otherFieldsTime = mergeInstant(this.otherFieldsTime, that.otherFieldsTime)
      )
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
