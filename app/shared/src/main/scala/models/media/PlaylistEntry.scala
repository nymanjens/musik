package models.media

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import common.OrderToken
import models.Entity
import models.access.DbQueryImplicits._
import models.access.{EntityAccess, ModelField}
import models.user.User

import scala.async.Async.{async, await}
import scala.concurrent.Future

case class PlaylistEntry(songId: Long, orderToken: OrderToken, userId: Long, idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}
object PlaylistEntry {
  def tupled = (this.apply _).tupled

  def getOrderedSeq()(implicit user: User, entityAccess: EntityAccess): Future[Seq[PlaylistEntry]] = async {
    implicit val orderTokenOrdering: Ordering[OrderToken] = implicitly[Ordering[OrderToken]] // Fix for build error
    val pairOrdering: Ordering[(OrderToken, Long)] = implicitly[Ordering[(OrderToken, Long)]] // Fix for build error
    await(entityAccess.newQuery[PlaylistEntry]().filter(ModelField.PlaylistEntry.userId === user.id).data())
      .sortBy(e => (e.orderToken, e.id))(pairOrdering)
  }
}
