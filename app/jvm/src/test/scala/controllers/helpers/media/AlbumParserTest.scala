package controllers.helpers.media

import com.google.inject.Guice
import com.google.inject.Inject
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.JvmTestObjects.mediaFile
import common.testing._
import controllers.helpers.media.AlbumParser.ParsedAlbum
import controllers.helpers.media.AlbumParser.ParsedSong
import controllers.helpers.media.ArtistAssignerFactory.ArtistAssigner
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class AlbumParserTest extends HookedSpecification {

  @Inject private val albumParser: AlbumParser = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "parse()" in {
    val assigner = new ArtistAssigner(Map("socks" -> "Socks", "sweaters" -> "Sweaters", "pots" -> "Pots"))

    "empty media list" in {
      albumParser.parse(Seq(), assigner) must beEmpty
    }

    "everything is set" in {
      val file = mediaFile(
        relativePath = "some-folder/I_love_my_feet/footwear.dd",
        title = "I love my feet!",
        album = "Footwear",
        artist = "SOCKS",
        albumartist = "SOCKS",
        trackNumber = "22",
        duration = 5.minutes,
        year = "1999",
        disc = "1"
      )
      val parsedAlbums = albumParser.parse(Seq(file), assigner)

      parsedAlbums mustEqual Seq(
        ParsedAlbum(
          relativePath = "some-folder/I_love_my_feet",
          title = "Footwear",
          canonicalArtistName = Some("Socks"),
          songs = Seq(
            ParsedSong(
              filename = "footwear.dd",
              title = "I love my feet!",
              canonicalArtistName = Some("Socks"),
              trackNumber = 22,
              duration = 5.minutes,
              disc = 1
            )),
          year = Some(1999)
        )
      )
    }

    "nothing is set" in {
      val file = mediaFile(relativePath = "some-folder/I_love_my_feet/footwear.dd", duration = 5.minutes)
      val parsedAlbums = albumParser.parse(Seq(file), assigner)

      parsedAlbums mustEqual Seq(
        ParsedAlbum(
          relativePath = "some-folder/I_love_my_feet",
          title = "I_love_my_feet",
          canonicalArtistName = None,
          songs = Seq(
            ParsedSong(
              filename = "footwear.dd",
              title = "footwear",
              canonicalArtistName = None,
              trackNumber = 1,
              duration = 5.minutes,
              disc = 1
            )),
          year = None
        )
      )
    }

    "multiple albums" in {
      val parsedAlbums =
        albumParser.parse(
          Seq(
            mediaFile(relativePath = "songs/album1/a.mm"),
            mediaFile(relativePath = "songs/album1/b.mm"),
            mediaFile(relativePath = "songs/album2/c.mm")),
          assigner
        )

      val Seq(album1, album2) = parsedAlbums.sortBy(_.relativePath)
      val Seq(songA, songB) = album1.songs.sortBy(_.filename)
      val Seq(songC) = album2.songs.sortBy(_.filename)

      album1.relativePath mustEqual "songs/album1"
      album2.relativePath mustEqual "songs/album2"
      songA.filename mustEqual "a.mm"
      songB.filename mustEqual "b.mm"
      songC.filename mustEqual "c.mm"
    }

    "parses track number" in {
      val parsedAlbums = albumParser.parse(Seq(mediaFile(trackNumber = "(22)")), assigner)

      getOnlyElement(getOnlyElement(parsedAlbums).songs).trackNumber mustEqual 22
    }

    "parses year" in {
      val parsedAlbums = albumParser.parse(Seq(mediaFile(year = "2012-08-22")), assigner)

      getOnlyElement(parsedAlbums).year must beSome(2012)
    }

    "parses year: no number" in {
      val parsedAlbums = albumParser.parse(Seq(mediaFile(year = "xyz")), assigner)

      getOnlyElement(parsedAlbums).year must beNone
    }

    "parses disc" in {
      val parsedAlbums = albumParser.parse(Seq(mediaFile(disc = "2 / 3")), assigner)

      getOnlyElement(getOnlyElement(parsedAlbums).songs).disc mustEqual 2
    }

    "track number: Falls back to file index if track numbers not unique" in {
      val parsedAlbums =
        albumParser.parse(
          Seq(
            mediaFile(relativePath = "a/b.mm", trackNumber = "12"),
            mediaFile(relativePath = "a/a.mm", trackNumber = "13"),
            mediaFile(relativePath = "a/c.mm", trackNumber = "12")),
          assigner
        )

      val Seq(song1, song2, song3) = getOnlyElement(parsedAlbums).songs.sortBy(_.trackNumber)

      song1.trackNumber mustEqual 1
      song1.filename mustEqual "a.mm"
      song2.trackNumber mustEqual 2
      song2.filename mustEqual "b.mm"
      song3.trackNumber mustEqual 3
      song3.filename mustEqual "c.mm"
    }

    "folder with multiple artists and albums" in {
      val parsedAlbums =
        albumParser.parse(
          Seq(
            mediaFile(relativePath = "a/a.mm", album = "Feet", artist = "Socks"),
            mediaFile(relativePath = "a/b.mm", album = "Stirring the Pot", artist = "Pots"),
            mediaFile(relativePath = "a/c.mm", album = "Wooly Pleasures", artist = "Sweaters")
          ),
          assigner
        )

      val albumA = getOnlyElement(parsedAlbums)

      albumA.title mustEqual "a"
      albumA.canonicalArtistName must beNone
    }

    "album.artist: prefer albumartist above artist" in {
      val parsedAlbums =
        albumParser.parse(
          Seq(
            mediaFile(relativePath = "a/a.mm", artist = "Socks", albumartist = "Pots"),
            mediaFile(relativePath = "a/c.mm", artist = "Socks")
          ),
          assigner
        )

      val albumA = getOnlyElement(parsedAlbums)

      albumA.canonicalArtistName must beSome("Pots")
    }
  }
}
