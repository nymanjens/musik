package hydro.models.access

import app.common.testing.TestObjects.testUser
import app.common.testing._
import app.models.access.ModelFields
import hydro.common.testing._
import hydro.models.access.InMemoryEntityDatabase.EntitiesFetcher
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.models.Entity
import hydro.models.access.DbQuery.Sorting
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.InMemoryEntityDatabase.Sortings
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class InMemoryEntityDatabaseTest extends HookedSpecification {

  private val entitiesFetcher = new FakeEntitiesFetcher

  val user1 = createUser(loginName = "login1", name = "name3")
  val user2 = createUser(loginName = "login2", name = "name2")
  val user3 = createUser(loginName = "login3", name = "name1")
  val user4 = createUser(loginName = "login4", name = "name0")

  "queryExecutor()" in {
    entitiesFetcher.users ++= Seq(user1, user2, user3)

    implicit val database = new InMemoryEntityDatabase(
      entitiesFetcher,
      sortings = Sortings.create.withSorting(Sorting.ascBy(ModelFields.User.loginName)))

    "all data" in {
      newUserQuery().data() must containTheSameElementsAs(Seq(user1, user2, user3))
    }
    "filter" in {
      newUserQuery().filter(ModelFields.User.name === "name2").data() mustEqual Seq(user2)
    }
    "limit" in {
      newUserQuery().sort(Sorting.ascBy(ModelFields.User.loginName)).limit(2).data() mustEqual
        Seq(user1, user2)
    }
    "sorting" in {
      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }
  }

  "update()" in {
    "Add" in {
      entitiesFetcher.users ++= Seq(user1, user2)
      implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
      triggerLazyFetching(database)

      entitiesFetcher.users += user3
      database.update(EntityModification.Add(user3))

      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }

    "Remove" in {
      entitiesFetcher.users ++= Seq(user1, user2, user3, user4)
      implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
      triggerLazyFetching(database)

      entitiesFetcher.users -= user4
      database.update(EntityModification.createRemove(user4))

      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }

    "Update" in {
      val user2AtCreate = createUser(id = user2.id, loginName = "login9", name = "name99")

      entitiesFetcher.users ++= Seq(user1, user2AtCreate, user3)
      implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
      triggerLazyFetching(database)

      entitiesFetcher.users -= user2AtCreate
      entitiesFetcher.users += user2
      database.update(EntityModification.Update(user2))

      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }
  }

  private def assertDatabaseContainsExactlySorted(users: User*)(implicit database: InMemoryEntityDatabase) = {
    "assertDatabaseContainsExactlySorted" in {
      "cached asc sorting" in {
        newUserQuery().sort(Sorting.ascBy(ModelFields.User.loginName)).data() mustEqual users
      }
      "cached desc sorting" in {
        newUserQuery().sort(Sorting.descBy(ModelFields.User.loginName)).data() mustEqual users.reverse
      }
      "non-cached sorting" in {
        newUserQuery().sort(Sorting.ascBy(ModelFields.User.name)).data() mustEqual users.reverse
      }
    }
  }

  private def newUserQuery()(implicit database: InMemoryEntityDatabase): DbResultSet.Sync[User] =
    DbResultSet.fromExecutor(database.queryExecutor[User])

  private def triggerLazyFetching(database: InMemoryEntityDatabase): Unit = {
    DbResultSet
      .fromExecutor(database.queryExecutor[User])
      .data() // ensure lazy fetching gets triggered (if any)
  }

  private def createUser(id: Long = -1, loginName: String, name: String): User = {
    testUser.copy(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      loginName = loginName,
      name = name
    )
  }

  private class FakeEntitiesFetcher extends EntitiesFetcher {
    val users: mutable.Set[User] = mutable.Set()

    override def fetch[E <: Entity](entityType: EntityType[E]) = entityType match {
      case User.Type => users.toVector.asInstanceOf[Seq[E]]
      case _         => Seq()
    }
  }
}
