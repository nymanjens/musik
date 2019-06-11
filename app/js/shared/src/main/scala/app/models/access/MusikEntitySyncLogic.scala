package app.models.access

import hydro.models.access.DbQueryImplicits._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.api.ScalaJsApi.UpdateToken
import app.api.ScalaJsApiClient
import app.models.media.PlaylistEntry
import app.models.modification.EntityTypes
import app.models.user.User
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access
import hydro.models.access.DbQuery
import hydro.models.access.DbResultSet
import hydro.models.access.EntitySyncLogic
import hydro.models.access.LocalDatabase

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class MusikEntitySyncLogic(implicit apiClient: ScalaJsApiClient, user: User) extends EntitySyncLogic {

  private val fullySyncedPart: EntitySyncLogic =
    new access.EntitySyncLogic.FullySynced(EntityTypes.fullySyncedLocally)

  def populateLocalDatabaseAndGetUpdateToken(db: LocalDatabase): Future[UpdateToken] = async {
    val updateToken = await(fullySyncedPart.populateLocalDatabaseAndGetUpdateToken(db))

    val playlistEntries = await(
      DbResultSet
        .fromExecutor(db.queryExecutor[PlaylistEntry]())
        .filter(ModelFields.PlaylistEntry.userId === user.id)
        .data())

    await(fetchAndLocallyPersisteMedia(playlistEntries, db))

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
    await(fetchAndLocallyPersisteMedia(affectedPlaylistEntries, db))
  }

  private def fetchAndLocallyPersisteMedia(playlistEntries: Seq[PlaylistEntry],
                                           db: LocalDatabase): Future[Unit] = ???
}
