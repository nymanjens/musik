package models.media

import models.Entity

case class Album(relativePath: String, title: String, artistId: Option[Long], idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
