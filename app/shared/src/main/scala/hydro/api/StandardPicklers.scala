package hydro.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

import hydro.common.GuavaReplacement.ImmutableBiMap
import app.models.modification.EntityTypes
import boopickle.Default._
import hydro.api.PicklableDbQuery.FieldWithValue
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity.LastUpdateTime

abstract class StandardPicklers {

  implicit val entityPickler: Pickler[Entity]

  implicit object LocalDateTimePickler extends Pickler[LocalDateTime] {
    override def pickle(dateTime: LocalDateTime)(implicit state: PickleState): Unit = logExceptions {
      val date = dateTime.toLocalDate
      val time = dateTime.toLocalTime

      state.pickle(date.getYear)
      state.pickle(date.getMonth.getValue)
      state.pickle(date.getDayOfMonth)
      state.pickle(time.getHour)
      state.pickle(time.getMinute)
      state.pickle(time.getSecond)
    }
    override def unpickle(implicit state: UnpickleState): LocalDateTime = logExceptions {
      LocalDateTime.of(
        LocalDate.of(
          state.unpickle[Int] /* year */,
          state.unpickle[Int] /* month */,
          state.unpickle[Int] /* dayOfMonth */
        ),
        LocalTime.of(
          state.unpickle[Int] /* hour */,
          state.unpickle[Int] /* minute */,
          state.unpickle[Int] /* second */
        )
      )
    }
  }

  implicit object InstantPickler extends Pickler[Instant] {
    override def pickle(instant: Instant)(implicit state: PickleState): Unit = logExceptions {
      state.pickle(instant.getEpochSecond)
      state.pickle(instant.getNano)
    }
    override def unpickle(implicit state: UnpickleState): Instant = logExceptions {
      Instant.ofEpochSecond(state.unpickle[Long], state.unpickle[Int])
    }
  }

  def enumPickler[T](values: Seq[T]): Pickler[T] = {
    val valueToNumber: ImmutableBiMap[T, Int] = {
      val builder = ImmutableBiMap.builder[T, Int]()
      for ((value, number) <- values.zipWithIndex) {
        builder.put(value, number + 1)
      }
      builder.build()
    }

    new Pickler[T] {
      override def pickle(value: T)(implicit state: PickleState): Unit = {
        state.pickle(valueToNumber.get(value))
      }
      override def unpickle(implicit state: UnpickleState): T = {
        valueToNumber.inverse().get(state.unpickle[Int])
      }
    }
  }

  implicit val EntityTypePickler: Pickler[EntityType.any] = enumPickler(EntityTypes.all)

  implicit object LastUpdateTimePickler extends Pickler[LastUpdateTime] {
    val neverUpdated: Byte = 1
    val allFields: Byte = 2
    val perField: Byte = 3

    override def pickle(lastUpdateTime: LastUpdateTime)(implicit state: PickleState): Unit = logExceptions {
      lastUpdateTime match {
        case LastUpdateTime.NeverUpdated =>
          state.pickle(neverUpdated)
        case LastUpdateTime.AllFields(time) =>
          state.pickle(allFields)
          state.pickle(time)
        case LastUpdateTime.PerField(timeMap) =>
          state.pickle(perField)
          state.pickle(timeMap.map { case (k, v) => PicklableModelField.fromRegular(k) -> v }.toMap)
      }
    }
    override def unpickle(implicit state: UnpickleState): LastUpdateTime = logExceptions {
      state.unpickle[Byte] match {
        case `neverUpdated` => LastUpdateTime.NeverUpdated
        case `allFields`    => LastUpdateTime.AllFields(state.unpickle[Instant])
        case `perField` =>
          LastUpdateTime.PerField(
            state
              .unpickle[Map[PicklableModelField, Instant]]
              .map {
                case (k, v) => k.toRegular -> v
              }
              .toMap)
      }
    }
  }

  implicit val fieldWithValuePickler: Pickler[FieldWithValue] =
    new Pickler[FieldWithValue] {
      override def pickle(obj: FieldWithValue)(implicit state: PickleState) = {
        def internal[E]: Unit = {
          state.pickle(obj.field)
          state.pickle(obj.value.asInstanceOf[E])(
            picklerForField(obj.field.toRegular).asInstanceOf[Pickler[E]])
        }
        internal
      }
      override def unpickle(implicit state: UnpickleState) = {
        def internal[E]: FieldWithValue = {
          val field = state.unpickle[PicklableModelField]
          val value = state.unpickle[E](picklerForField(field.toRegular).asInstanceOf[Pickler[E]])
          FieldWithValue(field = field, value = value)
        }
        internal
      }

      private def picklerForField(field: ModelField.any): Pickler[_] = {
        def fromFieldType(fieldType: ModelField.FieldType[_]): Pickler[_] = {
          def fromType[V: Pickler](fieldType: ModelField.FieldType[V]): Pickler[V] = implicitly
          fieldType match {
            case ModelField.FieldType.OptionType(valueFieldType) =>
              optionPickler(fromFieldType(valueFieldType))
            case ModelField.FieldType.BooleanType        => fromType(ModelField.FieldType.BooleanType)
            case ModelField.FieldType.IntType            => fromType(ModelField.FieldType.IntType)
            case ModelField.FieldType.LongType           => fromType(ModelField.FieldType.LongType)
            case ModelField.FieldType.DoubleType         => fromType(ModelField.FieldType.DoubleType)
            case ModelField.FieldType.StringType         => fromType(ModelField.FieldType.StringType)
            case ModelField.FieldType.LocalDateTimeType  => fromType(ModelField.FieldType.LocalDateTimeType)
            case ModelField.FieldType.FiniteDurationType => fromType(ModelField.FieldType.FiniteDurationType)
            case ModelField.FieldType.StringSeqType      => fromType(ModelField.FieldType.StringSeqType)
            case ModelField.FieldType.OrderTokenType     => fromType(ModelField.FieldType.OrderTokenType)
          }
        }
        fromFieldType(field.fieldType)
      }
    }

  implicit val picklableDbQueryPickler: Pickler[PicklableDbQuery] = {
    implicit val fieldWithDirectionPickler: Pickler[PicklableDbQuery.Sorting.FieldWithDirection] =
      boopickle.Default.generatePickler
    implicit val sortingPickler: Pickler[PicklableDbQuery.Sorting] = boopickle.Default.generatePickler
    boopickle.Default.generatePickler
  }

  implicit object EntityModificationPickler extends Pickler[EntityModification] {
    val addNumber: Byte = 1
    val updateNumber: Byte = 3
    val removeNumber: Byte = 2

    override def pickle(modification: EntityModification)(implicit state: PickleState): Unit =
      logExceptions {
        state.pickle[EntityType.any](modification.entityType)
        // Pickle number
        state.pickle(modification match {
          case _: EntityModification.Add[_]    => addNumber
          case _: EntityModification.Update[_] => updateNumber
          case _: EntityModification.Remove[_] => removeNumber
        })
        modification match {
          case EntityModification.Add(entity)      => state.pickle(entity)
          case EntityModification.Update(entity)   => state.pickle(entity)
          case EntityModification.Remove(entityId) => state.pickle(entityId)
        }
      }
    override def unpickle(implicit state: UnpickleState): EntityModification = logExceptions {
      val entityType = state.unpickle[EntityType.any]
      state.unpickle[Byte] match {
        case `addNumber` =>
          val entity = state.unpickle[Entity]
          def addModification[E <: Entity](entity: Entity, entityType: EntityType[E]): EntityModification = {
            EntityModification.Add(entityType.checkRightType(entity))(entityType)
          }
          addModification(entity, entityType)
        case `updateNumber` =>
          val entity = state.unpickle[Entity]
          def updateModification[E <: Entity](entity: Entity,
                                              entityType: EntityType[E]): EntityModification = {
            EntityModification.Update(entityType.checkRightType(entity))(entityType)
          }
          updateModification(entity, entityType)
        case `removeNumber` =>
          val entityId = state.unpickle[Long]
          EntityModification.Remove(entityId)(entityType)
      }
    }
  }

  protected def logExceptions[T](codeBlock: => T): T = {
    try {
      codeBlock
    } catch {
      case t: Throwable =>
        println(s"  Caught exception while pickling: $t")
        t.printStackTrace()
        throw t
    }
  }
}
