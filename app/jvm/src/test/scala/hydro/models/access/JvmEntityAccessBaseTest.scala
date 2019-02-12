package hydro.models.access

import app.models.slick.SlickEntityTableDefs.UserDef
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import app.common.testing.TestObjects._
import app.common.testing._
import hydro.common.testing._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityModificationEntity
import hydro.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import hydro.models.slick.SlickUtils.dbRun
import app.models.user.User
import com.google.inject._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class JvmEntityAccessBaseTest extends HookedSpecification {

  implicit private val adminUser = createUser().copy(loginName = "admin")

  @Inject implicit private val fakeClock: FakeClock = null

  @Inject private val entityAccess: JvmEntityAccessBase = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "persistEntityModifications()" in {
    "Persists EntityModification" in new WithApplication {
      fakeClock.setNowInstant(testInstant)

      entityAccess.persistEntityModifications(testModification)

      val modificationEntity = getOnlyElement(allEntityModifications())
      modificationEntity.userId mustEqual adminUser.id
      modificationEntity.modification mustEqual testModification
      modificationEntity.instant mustEqual testInstant
    }

    "EntityModification.Add" in new WithApplication {
      val user = createUser()

      entityAccess.persistEntityModifications(EntityModification.Add(user))

      assertPersistedUsersEqual(user)
    }

    "EntityModification.Update" in new WithApplication {
      val user1 = createUser()
      val user1Update = EntityModification.createUpdateAllFields(user1.copy(name = "other nme"))
      entityAccess.persistEntityModifications(EntityModification.Add(user1))

      entityAccess.persistEntityModifications(user1Update)

      assertPersistedUsersEqual(user1Update.updatedEntity)
    }

    "EntityModification.Remove" in new WithApplication {
      val user1 = createUser()
      entityAccess.persistEntityModifications(EntityModification.Add(user1))

      entityAccess.persistEntityModifications(EntityModification.createRemove(user1))

      assertPersistedUsersEqual()
    }

    "All modifications are idempotent" in {
      "EntityModification.Add is idempotent" in new WithApplication {
        val user1 = createUser()
        val updatedUser1 = user1.copy(name = "other name")
        val user2 = createUser()

        entityAccess.persistEntityModifications(
          EntityModification.Add(user1),
          EntityModification.Add(user1),
          EntityModification.Add(updatedUser1),
          EntityModification.Add(user2)
        )

        assertPersistedUsersEqual(user1, user2)
      }

      "EntityModification.Update is idempotent" in new WithApplication {
        val user1 = createUser()
        val updatedUser1 = user1.copy(name = "other name")
        val user2 = createUser()
        entityAccess.persistEntityModifications(EntityModification.Add(user1))

        entityAccess.persistEntityModifications(
          EntityModification.Update(updatedUser1),
          EntityModification.Update(updatedUser1),
          EntityModification.Update(user2)
        )

        assertPersistedUsersEqual(updatedUser1, user2)
      }

      "EntityModification.Remove is idempotent" in new WithApplication {
        val user1 = createUser()
        val user2 = createUser()
        val user3 = createUser()
        entityAccess.persistEntityModifications(EntityModification.Add(user1))
        entityAccess.persistEntityModifications(EntityModification.Add(user2))

        entityAccess.persistEntityModifications(
          EntityModification.createRemove(user2),
          EntityModification.createRemove(user2),
          EntityModification.createRemove(user3)
        )

        assertPersistedUsersEqual(user1)
      }
    }

    "Filters duplicates" in {
      "Filters duplicates: EntityModification.Add" in new WithApplication {
        val user1 = createUser()
        val updatedUser1 = user1.copy(name = "other name")
        entityAccess.persistEntityModifications(EntityModification.Add(user1))
        entityAccess.persistEntityModifications(EntityModification.Update(updatedUser1))
        val initialModifications = allEntityModifications()

        entityAccess.persistEntityModifications(EntityModification.Add(user1), EntityModification.Add(user1))

        assertPersistedUsersEqual(updatedUser1)
        allEntityModifications() mustEqual initialModifications
      }
      "Filters duplicates: EntityModification.Update" in new WithApplication {
        val user1 = createUser()
        val updatedUser1 = user1.copy(name = "other name")

        entityAccess.persistEntityModifications(EntityModification.Add(user1))
        entityAccess.persistEntityModifications(EntityModification.createRemove(user1))
        val initialModifications = allEntityModifications()

        entityAccess.persistEntityModifications(EntityModification.Update(updatedUser1))

        assertPersistedUsersEqual()
        allEntityModifications() mustEqual initialModifications
      }
    }
  }

  private def assertPersistedUsersEqual(users: User*) = {
    entityAccess.newQuerySync[User]().data() must containTheSameElementsAs(users)
    dbRun(entityAccess.newSlickQuery[User]()) must containTheSameElementsAs(users)
  }

  private def allEntityModifications(): Seq[EntityModificationEntity] =
    dbRun(entityAccess.newSlickQuery[EntityModificationEntity]()).toVector

  private def createUser(): User = testUser.copy(idOption = Some(EntityModification.generateRandomId()))
}
