package app.scala2js

import java.time.Month.MARCH

import app.common.testing.TestObjects._
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.media.Song
import app.models.user.User
import app.scala2js.AppConverters._
import hydro.common.time.LocalDateTime
import hydro.models.modification.EntityType
import hydro.scala2js.Scala2Js
import utest._

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {

    "fromEntityType" - {
      fromEntityType(User.Type) ==> UserConverter
    }

    "UserConverter" - {
      testToJsAndBack[User](testUserRedacted)
    }
    "SongConverter" - {
      testToJsAndBack[Song](testSong)
    }
    "AlbumConverter" - {
      testToJsAndBack[Album](testAlbum)
    }
    "ArtistConverter" - {
      testToJsAndBack[Artist](testArtist)
    }
    "PlaylistEntryConverter" - {
      testToJsAndBack[PlaylistEntry](testPlaylistEntry)
    }
    "PlayStatusConverter" - {
      testToJsAndBack[PlayStatus](testPlayStatus)
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
