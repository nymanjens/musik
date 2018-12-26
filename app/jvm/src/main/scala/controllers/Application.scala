package controllers

import java.nio.file.Files
import java.nio.file.Paths

import akka.stream.scaladsl.StreamConverters
import app.api.Picklers._
import app.api.ScalaJsApiServerFactory
import com.google.inject.Inject
import hydro.common.time.Clock
import controllers.helpers.AuthenticatedAction
import app.models.access.JvmEntityAccess
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

  def index() = AuthenticatedAction { implicit user => implicit request =>
    Redirect(controllers.routes.Application.reactAppRoot())
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }
  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

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
