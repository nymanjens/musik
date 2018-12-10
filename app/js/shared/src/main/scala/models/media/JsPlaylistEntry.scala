package models.media

import models.access.EntityAccess
import models.media

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsPlaylistEntry(song: JsSong, id: Long)

object JsPlaylistEntry {
  def fromEntity(playlistEntry: PlaylistEntry)(implicit entityAccess: EntityAccess): Future[JsPlaylistEntry] =
    async {
      val song = await(JsSong.fromEntityId(playlistEntry.songId))
      media.JsPlaylistEntry(
        song = song,
        id = playlistEntry.id
      )
    }
}
