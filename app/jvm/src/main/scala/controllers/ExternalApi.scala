package controllers

import com.google.inject.Inject
import common.time.Clock
import models.access.JvmEntityAccess
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class ExternalApi @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  playConfiguration: play.api.Configuration,
                                  entityAccess: JvmEntityAccess)
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

  def rescanMediaLibraryAsync(): Future[_] = Future {
    ???
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }
}
