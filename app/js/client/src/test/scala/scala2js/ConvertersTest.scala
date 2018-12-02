package scala2js

import java.time.Month.MARCH

import scala.concurrent.duration._
import common.testing.TestObjects._
import common.time.LocalDateTime
import models.access.ModelField
import models.media.{Album, Artist, Song}
import models.modification.{EntityModification, EntityType}
import models.user.User
import utest._

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Converters._

import scala.concurrent.duration.FiniteDuration

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {
    "fromModelField" - {
      Converters.fromModelField(ModelField.User.loginName) ==> Converters.StringConverter
      Converters.fromModelField(ModelField.id[User]) ==> Converters.LongConverter
    }
    "LongConverter" - {
      "to JS and back" - {
        testToJsAndBack[Long](1L)
        testToJsAndBack[Long](0L)
        testToJsAndBack[Long](-1L)
        testToJsAndBack[Long](-12392913292L)
        testToJsAndBack[Long](911427549585351L) // 15 digits, which is the maximal javascript precision
        testToJsAndBack[Long](6886911427549585129L)
        testToJsAndBack[Long](-6886911427549585129L)
      }
      "Produces ordered results" - {
        val lower = Scala2Js.toJs(999L).asInstanceOf[String]
        val higher = Scala2Js.toJs(1000L).asInstanceOf[String]
        (lower < higher) ==> true
      }
    }

    "fromEntityType" - {
      fromEntityType(EntityType.UserType) ==> UserConverter
    }

    "seqConverter" - {
      val seq = Seq(1, 2)
      val jsValue = Scala2Js.toJs(seq)
      assert(jsValue.isInstanceOf[js.Array[_]])
      Scala2Js.toScala[Seq[Int]](jsValue) ==> seq
    }

    "seqConverter: testToJsAndBack" - {
      testToJsAndBack[Seq[String]](Seq("a", "b"))
      testToJsAndBack[Seq[String]](Seq())
    }

    "optionConverter: testToJsAndBack" - {
      testToJsAndBack[Option[String]](Some("x"))
      testToJsAndBack[Option[String]](None)
    }

    "LocalDateTimeConverter: testToJsAndBack" - {
      testToJsAndBack[LocalDateTime](LocalDateTime.of(2022, MARCH, 13, 12, 13))
    }

    "LocalDateTimeConverter: testToJsAndBack" - {
      testToJsAndBack[FiniteDuration](28.minutes)
    }

    "EntityTypeConverter" - {
      testToJsAndBack[EntityType.any](EntityType.UserType)
    }

    "EntityModificationConverter" - {
      "Add" - {
        testToJsAndBack[EntityModification](EntityModification.Add(testUserRedacted))
      }
      "Update" - {
        testToJsAndBack[EntityModification](EntityModification.Update(testUserA))
      }
      "Remove" - {
        testToJsAndBack[EntityModification](EntityModification.Remove[User](19238))
      }
    }

    "UserConverter: testToJsAndBack" - {
      testToJsAndBack[User](testUserRedacted)
    }
    "SongConverter: testToJsAndBack" - {
      testToJsAndBack[Song](testSong)
    }
    "AlbumConverter: testToJsAndBack" - {
      testToJsAndBack[Album](testAlbum)
    }
    "ArtistConverter: testToJsAndBack" - {
      testToJsAndBack[Artist](testArtist)
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
