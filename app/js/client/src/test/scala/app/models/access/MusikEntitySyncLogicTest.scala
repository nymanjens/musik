package app.models.access

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.common.testing.TestObjects._
import app.common.testing.FakeScalaJsApiClient
import app.common.testing.TestObjects
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.media.Album
import app.models.media.Artist
import hydro.common.testing.FakeLocalDatabase
import hydro.common.testing.FakeLocalDatabase
import hydro.models.access.DbResultSet
import hydro.models.modification.EntityType
import hydro.models.Entity
import utest._

import scala.concurrent.Future
import scala.language.reflectiveCalls

object MusikEntitySyncLogicTest extends TestSuite {

  override def tests = TestSuite {

    implicit val fakeApiClient = new FakeScalaJsApiClient
    implicit val testUser = TestObjects.testUser
    val logic = new MusikEntitySyncLogic
    implicit val db = new FakeLocalDatabase()

    "populateLocalDatabaseAndGetUpdateToken" - async {
      val playlistArtist1 = createArtist()
      val playlistArtist2 = createArtist()
      val playlistAlbum = createAlbum(artistId = playlistArtist1.id)
      val playlistSong = createSong(albumId = playlistAlbum.id, artistId = playlistArtist2.id)
      val playlistEntry = createPlaylistEntry(songId = playlistSong.id, userId = testUser.id)

      fakeApiClient.addEntities(playlistArtist1, playlistArtist2, createArtist())
      fakeApiClient.addEntities(playlistAlbum, createAlbum())
      fakeApiClient.addEntities(playlistSong, createSong())
      fakeApiClient.addEntities(playlistEntry)

      await(logic.populateLocalDatabaseAndGetUpdateToken(db))

      await(allEntitiesInDb[PlaylistEntry]()) ==> Seq(playlistEntry)
      await(allEntitiesInDb[Artist]()) ==> Seq(playlistArtist1, playlistArtist2)
      await(allEntitiesInDb[Album]()) ==> Seq(playlistAlbum)
      await(allEntitiesInDb[Song]()) ==> Seq(playlistSong)
    }

    "canBeExecutedLocally" - {
      "yes" - {
        1 ==> 1
      }
      "no" - {
        1 ==> 1
      }
    }

    "handleEntityModificationUpdate" - {
      "media updates necessary" - {
        1 ==> 1
      }
      "no media updates necessary" - {
        1 ==> 1
      }
    }
  }

  private def allEntitiesInDb[E <: Entity: EntityType]()(implicit db: FakeLocalDatabase): Future[Seq[E]] = {
    DbResultSet.fromExecutor(db.queryExecutor[E]()).data()
  }
}
