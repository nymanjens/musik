package app.models.media

import hydro.models.modification.EntityType
import hydro.models.Entity

case class Album(relativePath: String,
                 title: String,
                 artistId: Option[Long],
                 year: Option[Int],
                 idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object Album {
  implicit val Type: EntityType[Album] = EntityType()

  def tupled = (this.apply _).tupled
}
