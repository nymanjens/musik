package app.controllers

import app.common.RelativePaths.joinPaths
import app.models.access.JvmEntityAccess
import app.models.media.Album
import app.models.media.Song
import com.google.inject.Inject
import app.controllers.helpers.media.AlbumParser
import app.controllers.helpers.media.ArtistAssignerFactory
import app.controllers.helpers.media.MediaScanner
import app.controllers.helpers.media.StoredMediaSyncer
import hydro.common.time.Clock
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
  def rescanMediaLibrary(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    rescanMediaLibraryAsync()

    Ok(s"""
         |OK
         |
         |Parsing has started asynchronously.
         |
         |View progress with the following command:
         |
         |  tail -f /tmp/musik-logs
       """.stripMargin.trim)
  }

  private def rescanMediaLibraryAsync(): Future[_] = Future {
    try {

      val oldRelativePaths = {
        val albumIdToRelativePath =
          entityAccess.newQuerySync[Album]().data().map(album => album.id -> album.relativePath).toMap
        entityAccess
          .newQuerySync[Song]()
          .data()
          .map(song => joinPaths(albumIdToRelativePath(song.albumId), song.filename))
      }
      println(s"  Found ${oldRelativePaths.size} existing songs.")

      val addedAndRemovedMedia =
        mediaScanner.scanAddedAndRemovedMedia(oldRelativePaths = oldRelativePaths.toSet)
      println(
        s"  Found ${oldRelativePaths.size} existing songs, " +
          s"${addedAndRemovedMedia.added.size} added files " +
          s"and ${addedAndRemovedMedia.removedRelativePaths.size} removed files.")

      val artistAssigner = artistAssignerFactory.fromDbAndMediaFiles(addedAndRemovedMedia.added)
      val parsedAlbums = albumParser.parse(addedAndRemovedMedia.added, artistAssigner)
      println(s"  Parsed ${parsedAlbums.size} albums.")

      storedMediaSyncer.addEntitiesFromParsedAlbums(parsedAlbums)
      storedMediaSyncer.removeEntitiesFromRelativeSongPaths(addedAndRemovedMedia.removedRelativePaths)
      println(s"  Done! Added ${parsedAlbums.size} albums.")
    } catch {
      case throwable: Throwable =>
        println(s"  Caught exception: $throwable")
        throwable.printStackTrace()
    }
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }
}
