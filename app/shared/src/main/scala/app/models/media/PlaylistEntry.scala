package app.models.media

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import app.common.OrderToken
import app.models.Entity
import app.models.access.DbQueryImplicits._
import app.models.access.EntityAccess
import app.models.access.ModelField
import app.models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future

case class PlaylistEntry(songId: Long, orderToken: OrderToken, userId: Long, idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object PlaylistEntry {
  def tupled = (this.apply _).tupled

  def getOrderedSeq()(implicit user: User, entityAccess: EntityAccess): Future[Seq[PlaylistEntry]] = async {
    await(entityAccess.newQuery[PlaylistEntry]().filter(ModelField.PlaylistEntry.userId === user.id).data())
      .sortWith(lt = (e1, e2) => {
        e1.orderToken compare e2.orderToken match {
          case 0 => e1.id < e2.id
          case i => i < 0
        }
      })
  }
}
