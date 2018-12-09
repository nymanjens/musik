package models.media

import common.OrderToken
import models.Entity

import scala.concurrent.duration.FiniteDuration

case class PlaylistEntry(songId: Long, orderToken: OrderToken, userId: Long, idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
