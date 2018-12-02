package models.media

import models.Entity

import scala.concurrent.duration.FiniteDuration

case class Song(filename: String,
                title: String,
                albumId: Long,
                artistId: Option[Long],
                trackNumber: Int,
                duration: FiniteDuration,
                year: Option[Int],
                disc: Int,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
