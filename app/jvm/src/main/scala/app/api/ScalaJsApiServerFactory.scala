package app.api

import app.api.Picklers._
import app.api.ScalaJsApi._
import app.api.UpdateTokens.toUpdateToken
import com.google.inject._
import app.common.PlayI18n
import hydro.common.time.Clock
import hydro.models.Entity
import hydro.models.access.DbQuery
import app.models.access.JvmEntityAccess
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.user.User
import app.models.user.Users
import hydro.api.PicklableDbQuery

import scala.collection.immutable.Seq

final class ScalaJsApiServerFactory @Inject()(implicit clock: Clock,
                                              entityAccess: JvmEntityAccess,
                                              i18n: PlayI18n) {

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        user = user,
        i18nMessages = i18n.allI18nMessages,
        nextUpdateToken = toUpdateToken(clock.nowInstant)
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = toUpdateToken(clock.nowInstant)
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.newQuerySync()(entityType).data()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      // check permissions
      for (modification <- modifications) {
        if (modification.entityType != EntityType.PlayStatusType) {
          require(
            !modification.isInstanceOf[EntityModification.Update[_]],
            "Update modifications are not allowed by remote clients " +
              "(see EntityModification.Update documentation)"
          )
        }
        require(
          modification.entityType != EntityType.UserType,
          "Please modify users by calling upsertUser() instead")
      }

      entityAccess.persistEntityModificationsAsync(modifications) // Don't wait for it to finish
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].data(query)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)
      }
      internal
    }

    override def upsertUser(userProto: UserPrototype): Unit = {
      def requireNonEmpty(s: Option[String]): Unit = {
        require(s.isDefined, "field is missing")
        require(s.get.nonEmpty, "field contains an empty string")
      }
      def requireNonEmptyIfSet(s: Option[String]): Unit = {
        if (s.isDefined) {
          require(s.get.nonEmpty, "field contains an empty string")
        }
      }

      userProto.id match {
        case None => // Add user
          requireNonEmpty(userProto.loginName)
          requireNonEmpty(userProto.plainTextPassword)
          requireNonEmpty(userProto.name)

          // Check permissions
          require(user.isAdmin, "Only an admin can add users")

          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(
              Users.createUser(
                loginName = userProto.loginName.get,
                password = userProto.plainTextPassword.get,
                name = userProto.name.get,
                isAdmin = userProto.isAdmin getOrElse false
              )))

        case Some(id) => // Update user
          requireNonEmptyIfSet(userProto.loginName)
          requireNonEmptyIfSet(userProto.plainTextPassword)
          requireNonEmptyIfSet(userProto.name)

          // Check permissions
          require(user.isAdmin || id == user.id, "Changing an other user's password")

          val existingUser = entityAccess.newQuerySync[User]().findById(id)
          val updatedUser = {
            var result = existingUser.copy(
              loginName = userProto.loginName getOrElse existingUser.loginName,
              name = userProto.name getOrElse existingUser.name,
              isAdmin = userProto.isAdmin getOrElse existingUser.isAdmin
            )
            if (userProto.plainTextPassword.isDefined) {
              result = Users.copyUserWithPassword(result, userProto.plainTextPassword.get)
            }
            result
          }

          entityAccess.persistEntityModifications(EntityModification.createUpdate(updatedUser))
      }
    }
  }
}
