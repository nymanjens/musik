package controllers

import java.nio.file.Paths

import com.google.inject.Inject
import app.common.RelativePaths
import app.common.RelativePaths.joinPaths
import hydro.common.time.Clock
import controllers.helpers.media.AlbumParser
import controllers.helpers.media.ArtistAssignerFactory
import controllers.helpers.media.MediaScanner
import controllers.helpers.media.StoredMediaSyncer
import app.models.access.JvmEntityAccess
import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import app.models.media.PlaylistEntry
import app.models.media.PlayStatus
import app.models.slick.SlickUtils.dbRun
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class ExternalApi @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  playConfiguration: play.api.Configuration,
                                  entityAccess: JvmEntityAccess,
                                  mediaScanner: MediaScanner,
                                  artistAssignerFactory: ArtistAssignerFactory,
                                  albumParser: AlbumParser,
                                  storedMediaSyncer: StoredMediaSyncer)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def healthCheck = Action { implicit request =>
    entityAccess.checkConsistentCaches()
    Ok("OK")
  }

  def rescanMediaLibrary(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    rescanMediaLibraryAsync()

    Ok(s"OK")
  }

  private def rescanMediaLibraryAsync(): Future[_] = Future {
    val oldRelativePaths = {
      val albumIdToRelativePath =
        entityAccess.newQuerySync[Album]().data().map(album => album.id -> album.relativePath).toMap
      entityAccess
        .newQuerySync[Song]()
        .data()
        .map(song => joinPaths(albumIdToRelativePath(song.albumId), song.filename))
    }
    val addedAndRemovedMedia =
      mediaScanner.scanAddedAndRemovedMedia(oldRelativePaths = oldRelativePaths.toSet)
    val artistAssigner = artistAssignerFactory.fromDbAndMediaFiles(addedAndRemovedMedia.added)
    val parsedAlbums = albumParser.parse(addedAndRemovedMedia.added, artistAssigner)

    storedMediaSyncer.addEntitiesFromParsedAlbums(parsedAlbums)
    storedMediaSyncer.removeEntitiesFromRelativeSongPaths(addedAndRemovedMedia.removedRelativePaths)
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }
}
