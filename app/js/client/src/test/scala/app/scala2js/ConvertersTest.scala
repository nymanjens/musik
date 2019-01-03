package app.scala2js

import java.time.Month.MARCH

import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import app.scala2js.AppConverters._
import hydro.common.time.LocalDateTime
import utest._

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.scalajs.js

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {
    "fromModelField" - {
      StandardConverters.fromModelField(ModelFields.User.loginName) ==> StandardConverters.StringConverter
      StandardConverters.fromModelField(ModelFields.id[User]) ==> StandardConverters.LongConverter
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
    "PlaylistEntryConverter: testToJsAndBack" - {
      testToJsAndBack[PlaylistEntry](testPlaylistEntry)
    }
    "PlayStatusConverter: testToJsAndBack" - {
      testToJsAndBack[PlayStatus](testPlayStatus)
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
