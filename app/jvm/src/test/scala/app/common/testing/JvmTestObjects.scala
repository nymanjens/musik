package app.common.testing

import app.controllers.helpers.media.MediaScanner.MediaFile

import scala.concurrent.duration._

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

  def mediaFile(
      relativePath: String = "test-relative-path",
      title: String = null,
      album: String = null,
      artist: String = null,
      albumartist: String = null,
      trackNumber: String = null,
      duration: FiniteDuration = 2.minutes,
      year: String = null,
      disc: String = null
  ): MediaFile =
    MediaFile(
      relativePath = relativePath,
      title = Option(title),
      album = Option(album),
      artist = Option(artist),
      trackNumber = Option(trackNumber),
      duration = duration,
      year = Option(year),
      disc = Option(disc),
      albumartist = Option(albumartist)
    )
}
