package app.models.user

import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.Entity.LastUpdateTime

case class User(loginName: String,
                passwordHash: String,
                name: String,
                isAdmin: Boolean,
                override val idOption: Option[Long] = None,
                override val lastUpdateTime: LastUpdateTime = LastUpdateTime.NeverUpdated)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  implicit val Type: EntityType[User] = EntityType()

  def tupled = (this.apply _).tupled
}
