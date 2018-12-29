package app.models.media

import app.common.OrderToken
import app.models.access.EntityAccess
import app.models.media

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsPlaylistEntry(song: JsSong, orderToken: OrderToken, id: Long)

object JsPlaylistEntry {
  def fromEntity(playlistEntry: PlaylistEntry)(implicit entityAccess: EntityAccess): Future[JsPlaylistEntry] =
    async {
      val song = await(JsSong.fromEntityId(playlistEntry.songId))
      media.JsPlaylistEntry(
        song = song,
        orderToken = playlistEntry.orderToken,
        id = playlistEntry.id
      )
    }

  def fromEntityId(id: Long)(implicit entityAccess: EntityAccess): Future[JsPlaylistEntry] = async {
    val entity = await(entityAccess.newQuery[PlaylistEntry]().findById(id))
    await(fromEntity(entity))
  }
}
