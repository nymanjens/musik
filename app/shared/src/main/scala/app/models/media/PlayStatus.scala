package app.models.media

import app.models.Entity
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.EntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.ExecutionContext.Implicits.global
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

  def get(verifyConsistency: Boolean = true)(implicit user: User,
                                             entityAccess: EntityAccess): Future[Option[PlayStatus]] = async {
    val maybePlayStatus =
      await(entityAccess.newQuery[PlayStatus]().findOne(ModelFields.PlayStatus.userId === user.id))
    if (verifyConsistency) {
      maybePlayStatus match {
        case Some(playStatus) =>
          await(
            entityAccess
              .newQuery[PlaylistEntry]()
              .findOne(ModelFields.PlaylistEntry.id === playStatus.currentPlaylistEntryId)) match {
            case Some(_) => maybePlayStatus
            case None =>
              println(
                s"Warning: Encountered PlayStatus that points to a playlist entry that doesn't exist: $playStatus")
              None
          }
        case None => None
      }
    } else {
      maybePlayStatus
    }
  }
}
