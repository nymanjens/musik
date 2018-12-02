package models.media

import models.Entity

import scala.concurrent.duration.FiniteDuration

case class Song(relativePath: String,
                albumId: Long,
                title: String,
                trackNumber: Int,
                duration: FiniteDuration,
                year: Option[Int],
                disc: Int,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
