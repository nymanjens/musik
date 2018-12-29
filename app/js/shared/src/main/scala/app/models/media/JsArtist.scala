package app.models.media

import hydro.models.access.EntityAccess

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class JsArtist(name: String, id: Long)
object JsArtist {
  def fromEntityId(artistId: Long)(implicit entityAccess: EntityAccess): Future[JsArtist] = async {
    val artist = await(entityAccess.newQuery[Artist]().findById(artistId))
    fromEntity(artist)
  }

  def fromEntity(artist: Artist): JsArtist = JsArtist(name = artist.name, id = artist.id)
}
