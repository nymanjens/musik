package models.media

import models.Entity

import scala.concurrent.duration.FiniteDuration

case class Song(albumId: Long,
                title: String,
                trackNumber: Int,
                duration: FiniteDuration,
                year: Int,
                disc: Int,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
