package app.controllers.helpers.media

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import hydro.common.GuavaReplacement.Splitter
import com.google.common.io.MoreFiles
import com.google.inject.Inject
import app.controllers.helpers.media.MediaScanner.AddedAndRemovedMedia
import app.controllers.helpers.media.MediaScanner.MediaFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions._
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

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

    val newRelativePaths = scannedRelativePaths.filterNot(oldRelativePaths)

    println(s"  Starting to scan ${newRelativePaths.size} non-indexed files.")
    val progressPrinter = new ProgressPrinter(total = newRelativePaths.size, printEveryNSteps = 100)

    val addedMedia: Seq[MediaFile] =
      for (relativePath <- newRelativePaths) yield {
        progressPrinter.proceedAndMaybePrint(relativePath)

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
              _: InvalidAudioFrameException | _ : UnsupportedOperationException =>
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

  private class ProgressPrinter(total: Int, printEveryNSteps: Int) {
    private var currentStep = 0

    def proceedAndMaybePrint(someStringToPrint: String): Unit = {
      if (currentStep % printEveryNSteps == 0) {
        println(s"  Scanning file ${currentStep + 1} of $total: $someStringToPrint")
      }
      currentStep += 1
    }
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
