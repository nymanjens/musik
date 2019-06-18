package app.models.access

import app.common.testing.FakeScalaJsApiClient
import app.common.testing.TestObjects
import app.common.testing.TestObjects._
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.media.Song
import hydro.common.testing.Awaiter
import hydro.common.testing.FakeLocalDatabase
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.DbResultSet
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.modification.EntityModification
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object MusikEntitySyncLogicTest extends TestSuite {

  override def tests = TestSuite {

    implicit val fakeApiClient = new FakeScalaJsApiClient
    implicit val testUser = TestObjects.testUser
    val logic = new MusikEntitySyncLogic
    implicit val db = new FakeLocalDatabase()

    val playlistArtist1 = createArtist()
    val playlistArtist2 = createArtist()
    val playlistAlbum = createAlbum(artistId = playlistArtist1.id)
    val playlistSong = createSong(albumId = playlistAlbum.id, artistId = playlistArtist2.id)
    val playlistEntry = createPlaylistEntry(songId = playlistSong.id, userId = testUser.id)

    "populateLocalDatabaseAndGetUpdateToken" - async {
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
      "PlayStatus query" - async {
        await(
          logic.canBeExecutedLocally(
            DbQuery[PlayStatus](
              filter = ModelFields.PlayStatus.userId isAnyOf Seq(1, 2, 3),
              sorting = None,
              limit = None),
            db)) ==> true
      }
      "Song lookup (local)" - {
        db.addAll(Seq(testSong))

        "Song is persisted locally" - async {
          await(
            logic.canBeExecutedLocally(
              DbQuery[Song](filter = ModelFields.Song.id === testSong.id, sorting = None, limit = None),
              db)) ==> true
        }
        "Song is missing locally" - async {
          await(
            logic.canBeExecutedLocally(
              DbQuery[Song](filter = ModelFields.Song.id === 9999, sorting = None, limit = None),
              db)) ==> false
        }
      }
    }

    "handleEntityModificationUpdate" - {
      fakeApiClient.addEntities(playlistArtist1, playlistArtist2, createArtist())
      fakeApiClient.addEntities(playlistAlbum, createAlbum())
      fakeApiClient.addEntities(playlistSong, createSong())
      fakeApiClient.addEntities(playlistEntry)

      "empty modifications" - async {
        await(logic.handleEntityModificationUpdate(Seq(), db))

        await(Awaiter.expectConsistently.isEmpty(db.allModifications))
      }
      "update playlist entry" - {
        "media updates necessary" - async {
          await(logic.handleEntityModificationUpdate(Seq(EntityModification.Add(playlistEntry)), db))

          await(
            Awaiter.expectEventually.equal(
              db.allModifications.toSet,
              Set(
                EntityModification.Add(playlistEntry),
                EntityModification.Add(playlistArtist1),
                EntityModification.Add(playlistArtist2),
                EntityModification.Add(playlistAlbum),
                EntityModification.Add(playlistSong)
              )
            ))
        }
        "media is already present" - async {
          db.addAll(Seq(playlistArtist1, playlistArtist2, createArtist()))
          db.addAll(Seq(playlistAlbum, createAlbum()))
          db.addAll(Seq(playlistSong, createSong()))
          val preExistingModifications = db.allModifications

          await(logic.handleEntityModificationUpdate(Seq(EntityModification.Add(playlistEntry)), db))

          await(
            Awaiter.expectConsistently.equal(
              db.allModifications.toSet,
              preExistingModifications.toSet ++ Set(EntityModification.Add(playlistEntry))
            ))
        }
      }
      "update play status" - async {
        await(logic.handleEntityModificationUpdate(Seq(EntityModification.Add(testPlayStatus)), db))

        await(
          Awaiter.expectConsistently.equal(
            db.allModifications.toSet,
            Set(EntityModification.Add(testPlayStatus))
          ))
      }
    }
  }

  private def allEntitiesInDb[E <: Entity: EntityType]()(implicit db: FakeLocalDatabase): Future[Seq[E]] = {
    DbResultSet.fromExecutor(db.queryExecutor[E]()).data()
  }
}
