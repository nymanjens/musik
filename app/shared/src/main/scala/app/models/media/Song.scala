package app.models.media

import hydro.models.modification.EntityType
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
  override def lastUpdateTime = throw new RuntimeException("Can never be updated")
}
object Song {
  implicit val Type: EntityType[Song] = EntityType()

  def tupled = (this.apply _).tupled
}
