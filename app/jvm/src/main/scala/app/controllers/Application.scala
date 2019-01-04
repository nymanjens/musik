package app.controllers

import java.nio.file.Files
import java.nio.file.Paths

import akka.stream.scaladsl.StreamConverters
import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.controllers.helpers.AuthenticatedAction
import hydro.common.time.Clock
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.utils.UriEncoding

final class Application @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  entityAccess: JvmEntityAccess,
                                  scalaJsApiServerFactory: ScalaJsApiServerFactory,
                                  playConfiguration: play.api.Configuration,
                                  env: play.api.Environment)
    extends AbstractController(components)
    with I18nSupport {

  def mediaAssets(relativePath: String) = AuthenticatedAction { implicit user => implicit request =>
    val mediaFolderPath =
      Paths.get(
        playConfiguration
          .get[String]("app.media.mediaFolder")
          .replaceFirst("^~", System.getProperty("user.home")))

    val assetPath = mediaFolderPath resolve UriEncoding.decodePath(relativePath, "utf-8")

    if (!Files.exists(assetPath)) {
      NotFound(s"Could not find $assetPath")
    } else if (Files.isDirectory(assetPath)) {
      NotFound(s"Could not find $assetPath")
    } else {
      require(assetPath.toRealPath() startsWith mediaFolderPath, assetPath)

      val connection = assetPath.toFile.toURI.toURL.openConnection()
      val stream = connection.getInputStream
      val source = StreamConverters.fromInputStream(() => stream)
      RangeResult
        .ofSource(
          entityLength = stream.available(), // TODO: This may not be entirely accurate
          source = source,
          rangeHeader = request.headers.get(RANGE),
          fileName = None,
          contentType = None // TODO: Set content type
        )
    }
  }
}
