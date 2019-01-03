package app.models.access

import java.time.Duration
import java.util.concurrent.Executors

import app.api.ScalaJsApi.ModificationsWithToken
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.media.Song
import app.models.modification.EntityTypes
import app.models.slick.SlickEntityTableDef
import app.models.slick.SlickEntityTableDefs
import app.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import app.models.user.User
import com.google.inject._
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.publisher.TriggerablePublisher
import hydro.common.time.Clock
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityModificationEntity
import hydro.models.modification.EntityType
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.dbRun
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryExecutor
import hydro.models.access.DbResultSet
import hydro.models.access.EntityAccess
import hydro.models.slick.SlickEntityManager
import net.jcip.annotations.GuardedBy
import org.reactivestreams.Publisher

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent._

final class JvmEntityAccess @Inject()(clock: Clock) extends EntityAccess {

  // lazy val because dropAndCreateTables() can be called before first fetch
  private lazy val inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase(
    entitiesFetcher = new InMemoryEntityDatabase.EntitiesFetcher {
      override def fetch[E <: Entity](entityType: EntityType[E]): Seq[E] =
        getManager(entityType).fetchAll().asInstanceOf[Seq[E]]
    })

  private val entityModificationPublisher_ : TriggerablePublisher[ModificationsWithToken] =
    new TriggerablePublisher()

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType]() = DbResultSet.fromExecutor(queryExecutor[E].asAsync)
  def newQuerySync[E <: Entity: EntityType](): DbResultSet.Sync[E] =
    DbResultSet.fromExecutor(queryExecutor)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = inMemoryEntityDatabase.queryExecutor

  def newSlickQuery[E <: Entity]()(
      implicit entityTableDef: SlickEntityTableDef[E]): TableQuery[entityTableDef.Table] =
    SlickEntityManager.forType[E].newQuery.asInstanceOf[TableQuery[entityTableDef.Table]]

  def entityModificationPublisher: Publisher[ModificationsWithToken] = entityModificationPublisher_

  // **************** Setters ****************//
  def persistEntityModifications(modifications: EntityModification*)(implicit user: User): Unit = {
    persistEntityModifications(modifications.toVector)
  }

  def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    Await.ready(persistEntityModificationsAsync(modifications), scala.concurrent.duration.Duration.Inf)
  }

  def persistEntityModificationsAsync(modifications: Seq[EntityModification])(
      implicit user: User): Future[Unit] = {
    EntityModificationAsyncProcessor.processAsync(modifications)
  }

  // ********** Management methods ********** //
  def dropAndCreateTables(): Unit = {
    for (tableDef <- SlickEntityTableDefs.all) {
      def internal[E <: Entity](tableDef: SlickEntityTableDef[E]) = {
        val entityManager = SlickEntityManager.forType[E](tableDef)
        dbRun(sqlu"""DROP TABLE IF EXISTS #${tableDef.tableName}""")
        entityManager.createTable()
      }
      internal(tableDef.asInstanceOf[SlickEntityTableDef[Entity]])
    }
  }

  def checkConsistentCaches(): Unit = {
    for (entityType <- EntityTypes.all) {
      def run[E <: Entity](entityType: EntityType[E]): Unit = {
        val allEntitiesInDb: Seq[E] = getManager(entityType).fetchAll().sortBy(_.id)
        val allEntitiesInMemory: Seq[E] =
          DbResultSet
            .fromExecutor(inMemoryEntityDatabase.queryExecutor(entityType))(entityType)
            .sort(DbQuery.Sorting.ascBy(ModelFields.id(entityType)))
            .data()
        require(
          allEntitiesInMemory.size == allEntitiesInDb.size,
          s"Mismatch between db and cache for entityType $entityType: Size mismatch: " +
            s"${allEntitiesInMemory.size} != ${allEntitiesInDb.size}"
        )
        require(
          allEntitiesInMemory == allEntitiesInDb,
          s"Mismatch between db and cache for entityType $entityType")
      }
      run(entityType)
    }
  }

  // ********** Private helper methods ********** //
  private def getManager(entityType: EntityType.any): SlickEntityManager[entityType.get] =
    SlickEntityManager.forType(getEntityTableDef(entityType))

  private def getEntityTableDef(entityType: EntityType.any): SlickEntityTableDef[entityType.get] = {
    val tableDef = entityType match {
      case User.Type          => SlickEntityTableDefs.UserDef
      case Song.Type          => SlickEntityTableDefs.SongDef
      case Album.Type         => SlickEntityTableDefs.AlbumDef
      case Artist.Type        => SlickEntityTableDefs.ArtistDef
      case PlaylistEntry.Type => SlickEntityTableDefs.PlaylistEntryDef
      case PlayStatus.Type    => SlickEntityTableDefs.PlayStatusDef
    }
    tableDef.asInstanceOf[SlickEntityTableDef[entityType.get]]
  }

  // ********** Private inner types ********** //
  private object EntityModificationAsyncProcessor {
    @GuardedBy("this")
    private val alreadySeenAddsAndRemoves: mutable.Set[EntityModification] = mutable.Set()

    private val singleThreadedExecutor =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

    def processAsync(modifications: Seq[EntityModification])(implicit user: User): Future[Unit] =
      this.synchronized {
        val uniqueModifications = modifications.filterNot(alreadySeenAddsAndRemoves)
        alreadySeenAddsAndRemoves ++= uniqueModifications.filter(m =>
          m.isInstanceOf[EntityModification.Add[_]] || m.isInstanceOf[EntityModification.Remove[_]])
        Future(processSync(uniqueModifications))(singleThreadedExecutor)
      }

    private def processSync(modifications: Seq[EntityModification])(implicit user: User): Unit = {
      def isDuplicate(modification: EntityModification,
                      existingModifications: Iterable[EntityModification]): Boolean = {
        val existingEntities = existingModifications.toStream
          .filter(_.entityId == modification.entityId)
          .filter(_.entityType == modification.entityType)
          .toSet
        val entityAlreadyRemoved =
          existingEntities.exists(_.isInstanceOf[EntityModification.Remove[_]])
        modification match {
          case _: EntityModification.Add[_]    => existingEntities.nonEmpty
          case _: EntityModification.Update[_] => entityAlreadyRemoved
          case _: EntityModification.Remove[_] => false // Always allow removes to fix inconsistencies
        }
      }

      val existingModifications: mutable.Set[EntityModification] =
        mutable.Set() ++
          dbRun(
            newSlickQuery[EntityModificationEntity]()
              .filter(_.entityId inSet modifications.map(_.entityId).toSet)
              .result)
            .map(_.modification)

      // Remove some time from the next update token because a slower persistEntityModifications() invocation B
      // could start earlier but end later than invocation A. If the WebSocket closes before the modifications B
      // get published, the `nextUpdateToken` value returned by A must be old enough so that modifications from
      // B all happened after it.
      val nextUpdateToken = toUpdateToken(clock.nowInstant minus Duration.ofSeconds(20))

      val uniqueModifications =
        for {
          modification <- modifications
          if {
            if (isDuplicate(modification, existingModifications)) {
              println(s"  Note: Modification marked as duplicate: modification = $modification")
              false
            } else {
              true
            }
          }
        } yield {
          existingModifications += modification

          // Apply modification
          val entityType = modification.entityType
          modification match {
            case EntityModification.Add(entity) =>
              getManager(entityType).addNew(entity.asInstanceOf[entityType.get])
            case EntityModification.Update(entity) =>
              getManager(entityType).updateIfExists(entity.asInstanceOf[entityType.get])
            case EntityModification.Remove(entityId) =>
              getManager(entityType).deleteIfExists(entityId)
          }

          // Add modification
          SlickEntityManager
            .forType[EntityModificationEntity]
            .addNew(
              EntityModificationEntity(
                idOption = Some(EntityModification.generateRandomId()),
                userId = user.id,
                modification = modification,
                instant = clock.nowInstant
              ))

          inMemoryEntityDatabase.update(modification)
          modification
        }

      entityModificationPublisher_.trigger(ModificationsWithToken(uniqueModifications, nextUpdateToken))
    }
  }
}
