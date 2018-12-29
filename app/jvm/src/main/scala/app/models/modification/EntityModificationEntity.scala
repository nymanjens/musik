package app.models.modification

import java.time.Instant

import hydro.common.time.LocalDateTime
import hydro.models.Entity
import app.models.access.JvmEntityAccess
import app.models.user.User

/**
  * Symbolises a modification to an entity.
  *
  * EntityModificationEntity entities are immutable and are assumed to be relatively short-lived, especially after
  * code updates to related models.
  */
case class EntityModificationEntity(userId: Long,
                                    modification: EntityModification,
                                    instant: Instant,
                                    idOption: Option[Long] = None)
    extends Entity {
  require(userId > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def user(implicit entityAccess: JvmEntityAccess): User = entityAccess.newQuerySync[User]().findById(userId)
}

object EntityModificationEntity {
  def tupled = (this.apply _).tupled
}
