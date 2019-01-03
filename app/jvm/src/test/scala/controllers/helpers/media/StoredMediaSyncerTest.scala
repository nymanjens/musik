package controllers.helpers.media

import app.common.testing.TestObjects._
import app.common.testing._
import app.models.access.JvmEntityAccess
import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import hydro.models.modification.EntityModification
import app.models.user.User
import com.google.inject.Guice
import com.google.inject.Inject
import controllers.helpers.media.AlbumParser.ParsedAlbum
import controllers.helpers.media.AlbumParser.ParsedSong
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

import scala.collection.immutable.Seq
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class StoredMediaSyncerTest extends HookedSpecification {

  implicit private val robotUser: User = testUser

  @Inject private val entityAccess: JvmEntityAccess = null

  @Inject private val storedMediaSycer: StoredMediaSyncer = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "addEntitiesFromParsedAlbums" in {
    "Adds song & album & artist" in new WithApplication {
      storedMediaSycer.addEntitiesFromParsedAlbums(
        Seq(ParsedAlbum(
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
        )))

      val Seq(songA) = entityAccess.newQuerySync[Song]().data()
      val Seq(albumA) = entityAccess.newQuerySync[Album]().data()
      val Seq(artistA) = entityAccess.newQuerySync[Artist]().data()

      songA.filename mustEqual "footwear.dd"
      songA.albumId mustEqual albumA.id
      songA.artistId must beSome(artistA.id)
      albumA.relativePath mustEqual "some-folder/I_love_my_feet"
      albumA.artistId must beSome(artistA.id)
      artistA.name mustEqual "Socks"
    }
    "Reuses existing albums & artist" in new WithApplication {
      entityAccess.persistEntityModifications(EntityModification.Add(testArtist.copy(name = "Socks")))
      entityAccess.persistEntityModifications(
        EntityModification.Add(testAlbum.copy(relativePath = "some-folder/I_love_my_feet")))

      storedMediaSycer.addEntitiesFromParsedAlbums(
        Seq(ParsedAlbum(
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
        )))

      val Seq(songA) = entityAccess.newQuerySync[Song]().data()
      val Seq(albumA) = entityAccess.newQuerySync[Album]().data()
      val Seq(artistA) = entityAccess.newQuerySync[Artist]().data()

      songA.filename mustEqual "footwear.dd"
      songA.albumId mustEqual testAlbum.id
      songA.artistId must beSome(testArtist.id)
      albumA.id mustEqual testAlbum.id
      artistA.id mustEqual testArtist.id
    }
  }
  "removeEntitiesFromRelativeSongPaths" in {
    "Removes songs but keeps album & artist" in new WithApplication {
      entityAccess.persistEntityModifications(EntityModification.Add(testArtist))
      entityAccess.persistEntityModifications(
        EntityModification.Add(
          testAlbum.copy(relativePath = "I_love_my_feet", artistId = Some(testArtist.id))))
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          testSong.copy(idOption = None, filename = "song1.mm", albumId = testAlbum.id)))
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          testSong.copy(idOption = None, filename = "song2.mm", albumId = testAlbum.id)))

      storedMediaSycer.removeEntitiesFromRelativeSongPaths(Seq("I_love_my_feet/song1.mm"))

      val Seq(songA) = entityAccess.newQuerySync[Song]().data()
      val Seq(albumA) = entityAccess.newQuerySync[Album]().data()
      val Seq(artistA) = entityAccess.newQuerySync[Artist]().data()

      songA.filename mustEqual "song2.mm"
      albumA.id mustEqual testAlbum.id
      artistA.id mustEqual testArtist.id
    }
    "Removes empty albums & artists" in new WithApplication {
      entityAccess.persistEntityModifications(EntityModification.Add(testArtist))
      entityAccess.persistEntityModifications(
        EntityModification.Add(
          testAlbum.copy(relativePath = "I_love_my_feet", artistId = Some(testArtist.id))))
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          testSong.copy(idOption = None, filename = "song1.mm", albumId = testAlbum.id)))

      storedMediaSycer.removeEntitiesFromRelativeSongPaths(Seq("I_love_my_feet/song1.mm"))

      entityAccess.newQuerySync[Song]().data() must beEmpty
      entityAccess.newQuerySync[Album]().data() must beEmpty
      entityAccess.newQuerySync[Artist]().data() must beEmpty
    }
  }
}
