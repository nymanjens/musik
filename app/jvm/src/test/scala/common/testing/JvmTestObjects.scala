package common.testing

import scala.concurrent.duration._
import controllers.helpers.media.MediaScanner.MediaFile

object JvmTestObjects {

  val testMediaFile: MediaFile = MediaFile(
    relativePath = "test-relative-path",
    title = Some("test-title"),
    album = Some("test-album"),
    artist = Some("test-artist"),
    trackNumber = Some("test-track-number"),
    duration = 2.minutes,
    year = Some("1990"),
    disc = Some("2"),
    albumartist = Some("test-albumartist")
  )

  def mediaFile(artist: String = null, albumartist: String = null): MediaFile = {
    testMediaFile.copy(artist = Option(artist), albumartist = Option(albumartist))
  }
}
