package app.models.media

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
