package app.models.media

import app.models.modification.EntityType
import hydro.models.Entity

import scala.concurrent.duration.FiniteDuration

case class Song(filename: String,
                title: String,
                albumId: Long,
                artistId: Option[Long],
                trackNumber: Int,
                duration: FiniteDuration,
                disc: Int,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object Song {
  implicit val Type: EntityType[Song] = EntityType()

  def tupled = (this.apply _).tupled
}
