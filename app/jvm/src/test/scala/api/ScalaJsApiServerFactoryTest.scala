package api

import java.time.Instant

import models.access.DbQueryImplicits._
import api.ScalaJsApi.UserPrototype
import api.UpdateTokens.toUpdateToken
import com.google.inject._
import common.GuavaReplacement.Iterables.getOnlyElement
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing._
import models.access.{DbQuery, JvmEntityAccess, ModelField}
import models.modification.{EntityModification, EntityModificationEntity, EntityType}
import models.slick.SlickUtils.dbRun
import models.user.User
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.SortedMap
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ScalaJsApiServerFactoryTest extends HookedSpecification {

  implicit private val user = testUserA

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val entityAccess: JvmEntityAccess = null

  @Inject private val serverFactory: ScalaJsApiServerFactory = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "getInitialData()" in new WithApplication {
    fakeClock.setNowInstant(testInstant)
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)

    val response = serverFactory.create().getInitialData()

    response.user mustEqual user
    response.nextUpdateToken mustEqual toUpdateToken(testInstant)
  }

  "getAllEntities()" in new WithApplication {
    fakeClock.setNowInstant(testInstant)
    TestUtils.persist(testUser)

    val response = serverFactory.create().getAllEntities(Seq(EntityType.UserType))

    response.entities(EntityType.UserType) mustEqual Seq(testUser)
    response.nextUpdateToken mustEqual toUpdateToken(testInstant)
  }

  "executeDataQuery()" in new WithApplication {
    TestUtils.persist(testUserA)
    TestUtils.persist(testUserB)

    val entities = serverFactory
      .create()
      .executeDataQuery(
        PicklableDbQuery.fromRegular(
          DbQuery[User](
            filter = ModelField.User.loginName === testUserA.loginName,
            sorting = None,
            limit = None)))

    entities.toSet mustEqual Set(testUserA)
  }

  "upsertUser()" should {
    "add" in new WithApplication {
      serverFactory
        .create()(testUser.copy(isAdmin = true))
        .upsertUser(UserPrototype.create(loginName = "tester", plainTextPassword = "abc", name = "Tester"))

      val storedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

      storedUser.loginName mustEqual "tester"
      storedUser.name mustEqual "Tester"
      storedUser.isAdmin mustEqual false
    }

    "update" should {
      "password" in new WithApplication {
        serverFactory
          .create()(testUser.copy(isAdmin = true))
          .upsertUser(UserPrototype.create(loginName = "tester", plainTextPassword = "abc", name = "Tester"))
        val createdUser = getOnlyElement(entityAccess.newQuerySync[User]().data())
        serverFactory
          .create()(testUser.copy(idOption = Some(createdUser.id)))
          .upsertUser(UserPrototype.create(id = createdUser.id, plainTextPassword = "def"))
        val updatedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

        updatedUser.passwordHash mustNotEqual createdUser.passwordHash
      }
      "isAdmin" in new WithApplication {
        serverFactory
          .create()(testUser.copy(isAdmin = true))
          .upsertUser(UserPrototype
            .create(loginName = "tester", plainTextPassword = "abc", name = "Tester", isAdmin = false))
        val createdUser = getOnlyElement(entityAccess.newQuerySync[User]().data())
        serverFactory
          .create()(testUser.copy(idOption = Some(createdUser.id)))
          .upsertUser(UserPrototype.create(id = createdUser.id, isAdmin = true))
        val updatedUser = getOnlyElement(entityAccess.newQuerySync[User]().data())

        createdUser.isAdmin mustEqual false
        updatedUser.isAdmin mustEqual true
      }
    }
  }
}
