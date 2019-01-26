package app.models.media

import hydro.common.OrderToken
import app.models.access.ModelFields
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.EntityAccess
import hydro.models.Entity.LastUpdateTime

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PlaylistEntry(songId: Long,
                         orderToken: OrderToken,
                         userId: Long,
                         override val idOption: Option[Long] = None,
                         override val lastUpdateTime: LastUpdateTime)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object PlaylistEntry {
  implicit val Type: EntityType[PlaylistEntry] = EntityType()

  implicit val ordering: Ordering[PlaylistEntry] = Ordering.fromLessThan { (e1, e2) =>
    e1.orderToken compare e2.orderToken match {
      case 0 => e1.id < e2.id
      case i => i < 0
    }
  }

  def tupled = (this.apply _).tupled

  def getOrderedSeq()(implicit user: User, entityAccess: EntityAccess): Future[Seq[PlaylistEntry]] = async {
    await(entityAccess.newQuery[PlaylistEntry]().filter(ModelFields.PlaylistEntry.userId === user.id).data()).sorted
  }
}
