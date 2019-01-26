package app.models.media

import hydro.models.modification.EntityType
import hydro.models.Entity

case class Artist(name: String, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
  override def lastUpdateTime = throw new RuntimeException("Can never be updated")
}
object Artist {
  implicit val Type: EntityType[Artist] = EntityType()

  def tupled = (this.apply _).tupled
}
