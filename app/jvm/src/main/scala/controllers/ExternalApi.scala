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
    def toJsonStringFromMap(map: Map[String, Any]): String = {
      val obj = new JSONObject(map.asJava)
      //      for ((key, value) <- map) value match {
      //        case v: Boolean => obj.put(key, (if (v) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE))
      //        case v: String  => obj.put(key, v)
      //      }
      obj.toString
    }
    def toJsonString(supportedExtension: Boolean,
                     path: Path,
                     title: String = "null",
                     album: String = "null",
                     artist: String = "null",
                     track: String = "null",
                     duration: String = "null",
                     year: String = "null",
                     disc: String = "null",
                     albumartist: String = "null"): String =
      toJsonStringFromMap(
        ListMap(
          "supported_extension" -> supportedExtension,
          "path" -> path.toString,
          "title" -> title,
          "album" -> album,
          "artist" -> artist,
          "track" -> track,
          "duration" -> duration,
          "year" -> year,
          "disc" -> disc,
          "albumartist" -> albumartist
        ))

    val supportedExtensions = Seq("mp3", "wav", "ogg", "opus", "flac", "wma", "mp4", "m4a")
    val mediaFolder = Paths.get(
      playConfiguration
        .get[String]("app.media.mediaFolder")
        .replaceFirst("^~", System.getProperty("user.home")))

    val writer = new BufferedWriter(new FileWriter(new File("/tmp/jaudiotagger.json")))
    writer.write("[\n")

    for {
      path <- MoreFiles.fileTraverser().depthFirstPreOrder(mediaFolder).asScala
      if !Files.isDirectory(path)
    } {
      val supportedExtension = supportedExtensions contains getLowercaseExtension(path)
      if (supportedExtension) {
        try {
          val audioFile = AudioFileIO.read(path.toFile)
          val tag = audioFile.getTag
          val audioHeader = audioFile.getAudioHeader

          def getFirstInTag(fieldKey: FieldKey): String = {
            if (null == tag) {
              null
            } else {
              tag.getFirst(fieldKey).toString
            }
          }
          writer.write(
            toJsonString(
              supportedExtension = supportedExtension,
              path = path,
              title = getFirstInTag(FieldKey.TITLE),
              album = getFirstInTag(FieldKey.ALBUM),
              artist = getFirstInTag(FieldKey.ARTIST),
              track = getFirstInTag(FieldKey.TRACK),
              duration = audioHeader.getTrackLength.toString,
              year = getFirstInTag(FieldKey.YEAR),
              disc = getFirstInTag(FieldKey.DISC_NO),
              albumartist = getFirstInTag(FieldKey.ALBUM_ARTIST)
            ))
          writer.write(",\n")
        } catch {
          case _: Throwable =>
            writer.write(toJsonString(supportedExtension = supportedExtension, path = path))
            writer.write(",\n")
        }
      } else {
        writer.write(toJsonString(supportedExtension = supportedExtension, path = path))
        writer.write(",\n")
      }
    }

    writer.write("]\n")
    writer.close()
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }

  private def getLowercaseExtension(path: Path): String = {
    Splitter.on('.').split(path.getFileName.toString).last.toLowerCase
  }
}
