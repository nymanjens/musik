package app.models.media

import hydro.common.OrderToken
import app.models.media
import hydro.models.access.EntityAccess

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsPlaylistEntry(song: JsSong, orderToken: OrderToken, userId: Long, id: Long) {
  def toEntity: PlaylistEntry =
    PlaylistEntry(songId = song.id, orderToken = orderToken, userId = userId, idOption = Some(id))
}

object JsPlaylistEntry {
  implicit val ordering: Ordering[JsPlaylistEntry] = Ordering.by(_.toEntity)

  def fromEntity(playlistEntry: PlaylistEntry)(implicit entityAccess: EntityAccess): Future[JsPlaylistEntry] =
    async {
      val song = await(JsSong.fromEntityId(playlistEntry.songId))
      media.JsPlaylistEntry(
        song = song,
        orderToken = playlistEntry.orderToken,
        userId = playlistEntry.userId,
        id = playlistEntry.id
      )
    }

  def fromEntityId(id: Long)(implicit entityAccess: EntityAccess): Future[JsPlaylistEntry] = async {
    val entity = await(entityAccess.newQuery[PlaylistEntry]().findById(id))
    await(fromEntity(entity))
  }
}
