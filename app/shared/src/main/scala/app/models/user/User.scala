package app.models.user

import hydro.models.modification.EntityType
import hydro.models.Entity

case class User(loginName: String,
                passwordHash: String,
                name: String,
                isAdmin: Boolean,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  implicit val Type: EntityType[User] = EntityType()

  def tupled = (this.apply _).tupled
}
