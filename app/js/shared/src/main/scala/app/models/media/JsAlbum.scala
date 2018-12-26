package app.models.media

import app.models.access.EntityAccess
import app.models.media

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsAlbum(relativePath: String, title: String, artist: Option[JsArtist], year: Option[Int], id: Long)
object JsAlbum {
  def fromEntityId(albumId: Long)(implicit entityAccess: EntityAccess): Future[JsAlbum] = async {
    val album = await(entityAccess.newQuery[Album]().findById(albumId))
    await(fromEntity(album))
  }

  def fromEntity(album: Album)(implicit entityAccess: EntityAccess): Future[JsAlbum] = async {
    val artist =
      if (album.artistId.isDefined) Some(await(JsArtist.fromEntityId(album.artistId.get))) else None
    media.JsAlbum(
      relativePath = album.relativePath,
      title = album.title,
      artist = artist,
      year = album.year,
      id = album.id
    )
  }

  def fromEntities(album: Album, artist: JsArtist): JsAlbum = {
    require(album.artistId == Some(artist.id))
    media.JsAlbum(
      relativePath = album.relativePath,
      title = album.title,
      artist = Some(artist),
      year = album.year,
      id = album.id
    )
  }
}
