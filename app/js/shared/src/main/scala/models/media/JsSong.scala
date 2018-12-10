package models.media

import models.access.EntityAccess
import models.media

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsSong(filename: String,
                  title: String,
                  album: JsAlbum,
                  artist: Option[JsArtist],
                  trackNumber: Int,
                  duration: FiniteDuration,
                  year: Option[Int],
                  disc: Int,
                  id: Long)

object JsSong {
  def fromEntityId(songId: Long)(implicit entityAccess: EntityAccess): Future[JsSong] = async {
    val song = await(entityAccess.newQuery[Song]().findById(songId))
    await(fromEntity(song))
  }

  def fromEntity(song: Song)(implicit entityAccess: EntityAccess): Future[JsSong] = async {
    val album = await(JsAlbum.fromEntityId(song.albumId))
    val artist =
      if (song.artistId.isDefined) Some(await(JsArtist.fromEntityId(song.artistId.get))) else None
    media.JsSong(
      filename = song.filename,
      title = song.title,
      album = album,
      artist = artist,
      trackNumber = song.trackNumber,
      duration = song.duration,
      year = song.year,
      disc = song.disc,
      id = song.id
    )
  }
}
