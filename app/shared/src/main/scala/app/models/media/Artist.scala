package app.models.media

import app.models.Entity

case class Artist(name: String, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
