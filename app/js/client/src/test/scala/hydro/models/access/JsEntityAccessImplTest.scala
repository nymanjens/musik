package hydro.models.access

import app.common.testing.FakeScalaJsApiClient
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.common.testing.ModificationsBuffer
import hydro.common.time.Clock
import hydro.models.Entity
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object JsEntityAccessImplTest extends TestSuite {

  override def tests = TestSuite {
    implicit val fakeApiClient: FakeScalaJsApiClient = new FakeScalaJsApiClient()
    implicit val fakeClock: Clock = new TestModule().fakeClock
    implicit val entitySyncLogic: EntitySyncLogic = new TestModule().entitySyncLogic
    implicit val getInitialDataResponse = testGetInitialDataResponse
    implicit val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
      new HydroPushSocketClientFactory
    val fakeLocalDatabase: FakeLocalDatabase = new FakeLocalDatabase()
    val localDatabasePromise: Promise[LocalDatabase] = Promise()
    implicit val remoteDatabaseProxy: HybridRemoteDatabaseProxy =
      HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
    val entityAccess = new JsEntityAccessImpl()

    "Fake local database not yet loaded" - {
      "newQuery" - async {
        fakeApiClient.addEntities(testUser)

        await(entityAccess.newQuery[User]().data()) ==> Seq(testUser)
      }

      "persistModifications()" - async {
        await(entityAccess.persistModifications(Seq(testModification)))

        fakeApiClient.allModifications ==> Seq(testModification)
      }

      "persistModifications(): calls listeners" - async {
        val listener = new FakeProxyListener()
        entityAccess.registerListener(listener)

        await(entityAccess.persistModifications(Seq(testModification)))

        listener.modifications ==> Seq(Seq(testModification))
      }
    }

    "Fake local database loaded" - {
      "loads initial data if db is empty" - async {
        await(fakeApiClient.persistEntityModifications(Seq(testModification)))
        localDatabasePromise.success(fakeLocalDatabase)
        await(remoteDatabaseProxy.localDatabaseReadyFuture)

        fakeLocalDatabase.allModifications ==> Seq(testModification)
      }

      "loads initial data if db is non-empty but has wrong version" - async {
        fakeLocalDatabase.applyModifications(Seq(testModificationA))
        fakeApiClient.persistEntityModifications(Seq(testModificationB))
        localDatabasePromise.success(fakeLocalDatabase)
        await(remoteDatabaseProxy.localDatabaseReadyFuture)

        fakeLocalDatabase.allModifications ==> Seq(testModificationB)
      }

      "does not load initial data if db is non-empty with right version" - async {
        fakeApiClient.persistEntityModifications(Seq(testModificationA))
        localDatabasePromise.success(fakeLocalDatabase)

        val entityAccess1 = {
          implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
          await(remoteDatabaseProxy.localDatabaseReadyFuture)
          new JsEntityAccessImpl()
        }
        fakeApiClient.persistEntityModifications(Seq(testModificationB))

        val entityAccess2 = {
          implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
          await(remoteDatabaseProxy.localDatabaseReadyFuture)
          new JsEntityAccessImpl()
        }

        fakeLocalDatabase.allModifications ==> Seq(testModificationA)
      }
    }
  }

  private final class FakeLocalDatabase extends LocalDatabase {
    val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
    val _pendingModifications: mutable.Buffer[EntityModification] = mutable.Buffer()
    private val singletonMap: mutable.Map[SingletonKey[_], js.Any] = mutable.Map()

    // **************** Getters ****************//
    override def queryExecutor[E <: Entity: EntityType]() = {
      DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E]).asAsync
    }
    override def pendingModifications() = Future.successful(_pendingModifications.toVector)
    override def getSingletonValue[V](key: SingletonKey[V]) = {
      Future.successful(singletonMap.get(key) map key.valueConverter.toScala)
    }
    override def isEmpty = {
      Future.successful(modificationsBuffer.isEmpty && singletonMap.isEmpty)
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = {
      modificationsBuffer.addModifications(modifications)
      Future.successful((): Unit)
    }
    override def addAll[E <: Entity: EntityType](entities: Seq[E]) = {
      modificationsBuffer.addEntities(entities)
      Future.successful((): Unit)
    }
    override def addPendingModifications(modifications: Seq[EntityModification]) = Future.successful {
      _pendingModifications ++= modifications
    }
    override def removePendingModifications(modifications: Seq[EntityModification]) = Future.successful {
      _pendingModifications --= modifications
    }
    override def setSingletonValue[V](key: SingletonKey[V], value: V) = {
      singletonMap.put(key, key.valueConverter.toJs(value))
      Future.successful((): Unit)
    }
    override def save() = Future.successful((): Unit)
    override def resetAndInitialize() = {
      modificationsBuffer.clear()
      singletonMap.clear()
      Future.successful((): Unit)
    }

    // **************** Additional methods for tests ****************//
    def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()
  }

  private final class FakeProxyListener extends JsEntityAccess.Listener {
    private val _modifications: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()

    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]) = {
      _modifications += modifications
    }

    def modifications: Seq[Seq[EntityModification]] = _modifications.toVector
  }
}
