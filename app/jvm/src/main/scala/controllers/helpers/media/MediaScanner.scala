package controllers.helpers.media

import java.io.IOException
import java.nio.file.{Files, Path, Paths}

import com.google.common.io.MoreFiles
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import models.access.JvmEntityAccess

import scala.collection.immutable.Seq
import scala.concurrent.duration.{FiniteDuration, _}

final class MediaScanner @Inject()(implicit
                                   playConfiguration: play.api.Configuration,
                                   entityAccess: JvmEntityAccess) {
  private val supportedExtensions: Seq[String] = Seq("mp3", "wav", "ogg", "opus", "flac", "wma", "mp4", "m4a")
  private val mediaFolder: Path = Paths.get(
    playConfiguration
      .get[String]("app.media.mediaFolder")
      .replaceFirst("^~", System.getProperty("user.home")))
  private val defaultDuration: FiniteDuration = 3.minutes

  def scanAllMedia(): Seq[MediaFile] = {
    {
      for {
        path <- MoreFiles.fileTraverser().depthFirstPreOrder(mediaFolder).asScala
        if !Files.isDirectory(path)
        if supportedExtensions contains getLowercaseExtension(path)
      } yield {
        val relativePath = mediaFolder.relativize(path).toString
        try {
          val audioFile = AudioFileIO.read(path.toFile)
          val maybeTag = Option(audioFile.getTag)
          val audioHeader = audioFile.getAudioHeader

          def getFirstInTag(fieldKey: FieldKey): Option[String] = maybeTag flatMap { tag =>
            tag.getFirst(fieldKey) match {
              case "" => None
              case v  => Some(v)
            }
          }

          MediaFile(
            relativePath = relativePath,
            title = getFirstInTag(FieldKey.TITLE),
            album = getFirstInTag(FieldKey.ALBUM),
            artist = getFirstInTag(FieldKey.ARTIST),
            trackNumber = getFirstInTag(FieldKey.TRACK),
            duration = audioHeader.getTrackLength.seconds,
            year = getFirstInTag(FieldKey.YEAR),
            disc = getFirstInTag(FieldKey.DISC_NO),
            albumartist = getFirstInTag(FieldKey.ALBUM_ARTIST)
          )
        } catch {
          case _: CannotReadException | _: IOException | _: TagException | _: ReadOnlyFileException |
              _: InvalidAudioFrameException =>
            MediaFile(
              relativePath = relativePath,
              title = None,
              album = None,
              artist = None,
              trackNumber = None,
              duration = defaultDuration,
              year = None,
              disc = None,
              albumartist = None
            )
        }
      }
    }.toVector
  }

  private def getLowercaseExtension(path: Path): String = {
    Splitter.on('.').split(path.getFileName.toString).last.toLowerCase
  }
}
object MediaScanner {
  case class MediaFile(relativePath: String,
                       title: Option[String],
                       album: Option[String],
                       artist: Option[String],
                       trackNumber: Option[String],
                       duration: FiniteDuration,
                       year: Option[String],
                       disc: Option[String],
                       albumartist: Option[String])
}
