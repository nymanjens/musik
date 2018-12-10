package models.media

import common.OrderToken
import models.access.EntityAccess
import models.media

import scala.async.Async.{async, await}
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
}
