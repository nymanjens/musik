package app.models.media

import app.common.OrderToken
import app.models.access.ModelFields
import app.models.modification.EntityType
import app.models.user.User
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.EntityAccess

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PlaylistEntry(songId: Long, orderToken: OrderToken, userId: Long, idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object PlaylistEntry {
  implicit val Type: EntityType[PlaylistEntry] = EntityType()

  def tupled = (this.apply _).tupled

  def getOrderedSeq()(implicit user: User, entityAccess: EntityAccess): Future[Seq[PlaylistEntry]] = async {
    await(entityAccess.newQuery[PlaylistEntry]().filter(ModelFields.PlaylistEntry.userId === user.id).data())
      .sortWith(lt = (e1, e2) => {
        e1.orderToken compare e2.orderToken match {
          case 0 => e1.id < e2.id
          case i => i < 0
        }
      })
  }
}
