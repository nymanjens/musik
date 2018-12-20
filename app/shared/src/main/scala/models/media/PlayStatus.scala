package models.media

import models.Entity
import models.access.DbQueryImplicits._
import models.access.{EntityAccess, ModelField}
import models.user.User

import scala.concurrent.Future

case class PlayStatus(currentPlaylistEntryId: Long,
                      hasStarted: Boolean,
                      stopAfterCurrentSong: Boolean,
                      userId: Long,
                      idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object PlayStatus {
  def tupled = (this.apply _).tupled

  def get()(implicit user: User, entityAccess: EntityAccess): Future[Option[PlayStatus]] =
    entityAccess.newQuery[PlayStatus]().findOne(ModelField.PlayStatus.userId === user.id)
}
