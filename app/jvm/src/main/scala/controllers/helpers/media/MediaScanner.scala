package controllers.helpers.media

import java.io.IOException
import java.nio.file.{Files, Path, Paths}

import com.google.common.io.MoreFiles
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import controllers.helpers.media.MediaScanner.{AddedAndRemovedMedia, MediaFile}
import models.access.JvmEntityAccess

import scala.collection.JavaConverters._
import scala.collection.immutable.{ListMap, Seq}
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.{FieldKey, TagException}
import org.jaudiotagger.audio.exceptions._

import scala.collection.immutable.Seq
import scala.concurrent.duration.{FiniteDuration, _}

final class MediaScanner @Inject()(implicit playConfiguration: play.api.Configuration) {
  private val supportedExtensions: Seq[String] = Seq("mp3", "wav", "ogg", "opus", "flac", "wma", "mp4", "m4a")
  private lazy val mediaFolder: Path = Paths.get(
    playConfiguration
      .get[String]("app.media.mediaFolder")
      .replaceFirst("^~", System.getProperty("user.home")))
  private val defaultDuration: FiniteDuration = 3.minutes

  def scanAddedAndRemovedMedia(oldRelativePaths: Set[String]): AddedAndRemovedMedia = {

    val scannedRelativePaths: Seq[String] = {
      for {
        path <- MoreFiles.fileTraverser().depthFirstPreOrder(mediaFolder).asScala
        if !Files.isDirectory(path)
        if supportedExtensions contains getLowercaseExtension(path)
      } yield mediaFolder.relativize(path).toString
    }.toVector

    val addedMedia: Seq[MediaFile] = for {
      relativePath <- scannedRelativePaths
      if !(oldRelativePaths contains relativePath)
    } yield {
      val path = mediaFolder resolve relativePath
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

    AddedAndRemovedMedia(
      added = addedMedia,
      removedRelativePaths = oldRelativePaths.filterNot(scannedRelativePaths.toSet).toVector)
  }

  private def getLowercaseExtension(path: Path): String = {
    Splitter.on('.').split(path.getFileName.toString).last.toLowerCase
  }
}
object MediaScanner {

  case class AddedAndRemovedMedia(added: Seq[MediaFile], removedRelativePaths: Seq[String])

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
