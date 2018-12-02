package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import java.io.{BufferedWriter, File, FileWriter}

import org.json.simple.JSONObject
import java.nio.file.{Files, Path, Paths}

import scala.collection.JavaConverters._
import com.google.common.io.MoreFiles
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import common.time.Clock
import models.access.JvmEntityAccess
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.collection.immutable.{ListMap, Seq}
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

import scala.collection.{SortedMap, mutable}
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
