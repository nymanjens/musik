package hydro.models

import scala.collection.immutable.Seq
import app.common.testing.TestObjects._
import app.models.access.ModelFields
import hydro.common.testing._
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.access.ModelField
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UpdatableEntityTest extends HookedSpecification {

  private val fieldA: ModelField.any = ModelFields.User.loginName
  private val fieldB: ModelField.any = ModelFields.User.name
  private val fieldC: ModelField.any = ModelFields.User.passwordHash
  private val testInstantBIncrement = testInstantB plusNanos 1
  require(testInstantBIncrement != testInstantB)

  "merge" in { 1 mustEqual 1 }

  "LastUpdateTime" in {
    "canonicalized" in {
      "neverUpdated" in {
        val time = LastUpdateTime.neverUpdated
        time.canonicalized mustEqual time
      }
      "allFieldsUpdated" in {
        val time = LastUpdateTime.allFieldsUpdated(testInstantA)
        time.canonicalized mustEqual time
      }
      "someFieldsUpdated" in {
        val time = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
        time.canonicalized mustEqual time
      }
      "general" in {
        "otherFieldsTime is oldest" in {
          val time =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantC), otherFieldsTime = Some(testInstantA))
          time.canonicalized mustEqual time
        }
        "otherFieldsTime is newest" in {
          LastUpdateTime(timePerField = Map(fieldA -> testInstantA), otherFieldsTime = Some(testInstantB)).canonicalized mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantB)
        }
        "otherFieldsTime is in the middle" in {
          LastUpdateTime(
            timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantC),
            otherFieldsTime = Some(testInstantB)).canonicalized mustEqual
            LastUpdateTime(timePerField = Map(fieldB -> testInstantC), otherFieldsTime = Some(testInstantB))
        }
      }
    }

    "merge" in {
      "forceIncrement = false" in {
        "allFieldsUpdated + allFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.allFieldsUpdated(testInstantB)

          val expected = time2
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "allFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          val expected =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantB), otherFieldsTime = Some(testInstantA))
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "neverUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.neverUpdated
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          val expected = time2
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "someFieldsUpdated + someFieldsUpdated (overlap)" in {
          val time1 = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB, fieldC), testInstantB)

          val expected =
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantB, fieldC -> testInstantB),
              otherFieldsTime = None)
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
        "general + someFieldsUpdated (no overlap)" in {
          val time1 =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantC), otherFieldsTime = Some(testInstantA))
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB), testInstantB)

          val expected = LastUpdateTime(
            timePerField = Map(fieldA -> testInstantC, fieldB -> testInstantB),
            otherFieldsTime = Some(testInstantA))
          time1.merge(time2, forceIncrement = false) mustEqual expected
          time2.merge(time1, forceIncrement = false) mustEqual expected
        }
      }
      "forceIncrement = true" in {
        "allFieldsUpdated + allFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.allFieldsUpdated(testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual time2
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantBIncrement)
        }
        "allFieldsUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.allFieldsUpdated(testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(timePerField = Map(fieldA -> testInstantB), otherFieldsTime = Some(testInstantA))
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime.allFieldsUpdated(testInstantBIncrement)
        }
        "neverUpdated + someFieldsUpdated" in {
          val time1 = LastUpdateTime.neverUpdated
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldA), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual time2
        }
        "someFieldsUpdated + someFieldsUpdated (overlap)" in {
          val time1 = LastUpdateTime.someFieldsUpdated(Seq(fieldA, fieldB), testInstantA)
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB, fieldC), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantA, fieldB -> testInstantB, fieldC -> testInstantB),
              otherFieldsTime = None)
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField =
                Map(fieldA -> testInstantA, fieldB -> testInstantBIncrement, fieldC -> testInstantB),
              otherFieldsTime = None)
        }
        "general + someFieldsUpdated (no overlap)" in {
          val time1 =
            LastUpdateTime(timePerField = Map(fieldA -> testInstantC), otherFieldsTime = Some(testInstantA))
          val time2 = LastUpdateTime.someFieldsUpdated(Seq(fieldB), testInstantB)

          time1.merge(time2, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantC, fieldB -> testInstantB),
              otherFieldsTime = Some(testInstantA))
          time2.merge(time1, forceIncrement = true) mustEqual
            LastUpdateTime(
              timePerField = Map(fieldA -> testInstantC),
              otherFieldsTime = Some(testInstantBIncrement))
        }
      }
    }
  }
}
