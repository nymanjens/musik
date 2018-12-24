package models.user

import models.access.DbQueryImplicits._
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.time.Clock
import models.access.JvmEntityAccess
import models.access.ModelField
import models.modification.EntityModification

object Users {

  def createUser(loginName: String, password: String, name: String, isAdmin: Boolean = false): User =
    User(
      loginName = loginName,
      passwordHash = hash(password),
      name = name,
      isAdmin = isAdmin
    )

  def copyUserWithPassword(user: User, password: String): User = {
    user.copy(passwordHash = hash(password))
  }

  def getOrCreateRobotUser()(implicit entityAccess: JvmEntityAccess, clock: Clock): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString

    entityAccess.newQuerySync[User]().findOne(ModelField.User.loginName === loginName) match {
      case Some(user) => user
      case None =>
        val userAddition = EntityModification.createAddWithRandomId(
          createUser(
            loginName = loginName,
            password = hash(clock.now.toString),
            name = "Robot"
          ))
        val userWithId = userAddition.entity
        entityAccess.persistEntityModifications(userAddition)(user = userWithId)
        userWithId
    }
  }

  def authenticate(loginName: String, password: String)(implicit entityAccess: JvmEntityAccess): Boolean = {
    entityAccess.newQuerySync[User]().findOne(ModelField.User.loginName === loginName) match {
      case Some(user) if user.passwordHash == hash(password) => true
      case _                                                 => false
    }
  }

  private def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString
}
