package models.media

import common.OrderToken
import models.Entity

case class PlayStatus(currentPlaylistEntryId: Long,
                      hasStarted: Boolean,
                      stopAfterCurrentSong: Boolean,
                      userId: Long,
                      idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
