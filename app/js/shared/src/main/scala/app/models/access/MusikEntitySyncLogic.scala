package app.models.access

import hydro.models.access.DbQueryImplicits._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.api.ScalaJsApi.UpdateToken
import app.api.ScalaJsApiClient
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.modification.EntityTypes
import app.models.user.User
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryExecutor
import hydro.models.access.DbResultSet
import hydro.models.access.EntitySyncLogic
import hydro.models.access.LocalDatabase

import scala.collection.immutable.Seq
import scala.concurrent.Future

private[access] final class MusikEntitySyncLogic(implicit apiClient: ScalaJsApiClient, user: User)
    extends EntitySyncLogic {

  private val fullySyncedPart: EntitySyncLogic =
    new access.EntitySyncLogic.FullySynced(EntityTypes.fullySyncedLocally)

  def populateLocalDatabaseAndGetUpdateToken(db: LocalDatabase): Future[UpdateToken] = async {
    val updateToken = await(fullySyncedPart.populateLocalDatabaseAndGetUpdateToken(db))

    val playlistEntries = await(
      DbResultSet
        .fromExecutor(db.queryExecutor[PlaylistEntry]())
        .filter(ModelFields.PlaylistEntry.userId === user.id)
        .data())

    await(fetchAndLocallyPersistMedia(playlistEntries, db))

    updateToken
  }

  def canBeExecutedLocally[E <: Entity: EntityType](dbQuery: DbQuery[E], db: LocalDatabase): Future[Boolean] =
    async {
      if (await(fullySyncedPart.canBeExecutedLocally(dbQuery, db))) {
        true
      } else {
        dbQuery.filter match {
          // Heuristic: Only support findById() queries
          case DbQuery.Filter.Equal(field, value) if field.name == "id" =>
            // Can be executed locally if there is a hit
            await(db.queryExecutor[E]().count(dbQuery)) > 0
          case _ => false
        }
      }
    }

  def handleEntityModificationUpdate(entityModifications: Seq[EntityModification],
                                     db: LocalDatabase): Future[Unit] = async {
    // TODO: Handle media updates (although they are (not yet) supported)

    await(fullySyncedPart.handleEntityModificationUpdate(entityModifications, db))

    val affectedPlaylistEntries = entityModifications.flatMap {
      case EntityModification.Add(entity: PlaylistEntry)    => Some(entity)
      case EntityModification.Update(entity: PlaylistEntry) => Some(entity)
      case _                                                => None
    }
    if (affectedPlaylistEntries.nonEmpty) {
      fetchAndLocallyPersistMedia(affectedPlaylistEntries, db) // Note: Not waiting for future to complete!
    }
  }

  private def fetchAndLocallyPersistMedia(playlistEntries: Seq[PlaylistEntry],
                                          db: LocalDatabase): Future[Unit] = async {
    // TODO: Check if IDs are already locally persisted
    val songIds = playlistEntries.map(_.songId)
    val songs = await(newApiQuery[Song]().filter(ModelFields.Song.id isAnyOf songIds).data())
    val songsFuture = db.addAll(songs)

    val albumIds = songs.map(_.albumId)
    val albums = await(newApiQuery[Album]().filter(ModelFields.Album.id isAnyOf albumIds).data())
    val albumsFuture = db.addAll(albums)

    val artistIds = (songs.flatMap(_.artistId) ++ albums.flatMap(_.artistId)).distinct
    val artists = await(newApiQuery[Artist]().filter(ModelFields.Artist.id isAnyOf artistIds).data())
    val artistsFuture = db.addAll(artists)

    await(songsFuture)
    await(albumsFuture)
    await(artistsFuture)
  }

  private def newApiQuery[E <: Entity: EntityType](): DbResultSet.Async[E] =
    DbResultSet.fromExecutor(new DbQueryExecutor.Async[E] {
      override def data(dbQuery: DbQuery[E]) = apiClient.executeDataQuery(dbQuery)
      override def count(dbQuery: DbQuery[E]) = apiClient.executeCountQuery(dbQuery)
    })
}
