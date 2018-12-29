package app.models.media

import hydro.models.Entity

case class Artist(name: String, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
