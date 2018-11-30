package models.media

import models.Entity

case class Album(artistId: Long, title: String, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
