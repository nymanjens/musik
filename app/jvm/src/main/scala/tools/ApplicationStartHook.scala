package tools

import java.nio.file.{Path, Paths}

import com.google.inject.Inject
import common.time.Clock
import common.{OrderToken, ResourceFiles}
import models.access.JvmEntityAccess
import models.media.{Album, Artist, Song, PlaylistEntry, PlayStatus}
import models.modification.EntityModification
import models.user.Users
import play.api.{Application, Mode}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

final class ApplicationStartHook @Inject()(implicit app: Application,
                                           entityAccess: JvmEntityAccess,
                                           clock: Clock) {
  onStart()

  private def onStart(): Unit = {
    processFlags()

    // Set up database if necessary
    if (app.mode == Mode.Test || app.mode == Mode.Dev) {
      if (AppConfigHelper.dropAndCreateNewDb) {
        dropAndCreateNewDb()
      }
    }

    // Populate the database with dummy data
    if (app.mode == Mode.Test || app.mode == Mode.Dev) {
      if (AppConfigHelper.loadDummyUsers) {
        loadDummyUsers()
      }
      if (AppConfigHelper.loadDummyData) {
        loadDummyData()
      }
    }
  }

  private def processFlags(): Unit = {
    if (CommandLineFlags.dropAndCreateNewDb) {
      println("")
      println("  Dropping the database tables (if present) and creating new ones...")
      dropAndCreateNewDb()
      println("  Done. Exiting.")

      System.exit(0)
    }

    if (CommandLineFlags.createAdminUser) {
      implicit val user = Users.getOrCreateRobotUser()

      val loginName = "admin"
      val password = AppConfigHelper.defaultPassword getOrElse "changeme"

      println("")
      println(s"  Creating admin user...")
      println(s"      loginName: $loginName")
      println(s"      password: $password")
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(
          Users.createUser(loginName, password, name = "Admin", isAdmin = true)))
      println("  Done. Exiting.")

      System.exit(0)
    }
  }

  private def dropAndCreateNewDb(): Unit = {
    entityAccess.dropAndCreateTables()
  }

  private def loadDummyUsers(): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    entityAccess.persistEntityModifications(
      EntityModification
        .createAddWithId(
          1111,
          Users.createUser(loginName = "admin", password = "a", name = "Admin", isAdmin = true)),
      EntityModification
        .createAddWithId(2222, Users.createUser(loginName = "alice", password = "a", name = "Alice")),
      EntityModification
        .createAddWithId(3333, Users.createUser(loginName = "bob", password = "b", name = "Bob"))
    )
  }

  private def loadDummyData(): Unit = {
    implicit val user = Users.getOrCreateRobotUser()

    // TODO: Implement
  }

  private def assertExists(path: Path): Path = {
    require(ResourceFiles.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def dropAndCreateNewDb: Boolean = getBoolean("dropAndCreateNewDb")
    def createAdminUser: Boolean = getBoolean("createAdminUser")

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def dropAndCreateNewDb: Boolean = getBoolean("app.development.dropAndCreateNewDb")
    def loadDummyUsers: Boolean = getBoolean("app.development.loadDummyUsers")
    def loadDummyData: Boolean = getBoolean("app.development.loadDummyData")
    def defaultPassword: Option[String] = getString("app.setup.defaultPassword")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getOptional[Boolean](cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getOptional[String](cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.get[String](cfgPath))
    }
  }
}
