package app.controllers.helpers.media

import hydro.common.CollectionUtils.getMostCommonString
import hydro.common.GuavaReplacement.Splitter
import app.common.RelativePaths
import com.google.inject.Inject
import app.controllers.helpers.media.AlbumParser.ParsedAlbum
import app.controllers.helpers.media.AlbumParser.ParsedSong
import app.controllers.helpers.media.ArtistAssignerFactory.ArtistAssigner
import app.controllers.helpers.media.MediaScanner.MediaFile

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

final class AlbumParser @Inject()() {

  def parse(mediaFiles: Seq[MediaFile], artistAssigner: ArtistAssigner): Seq[ParsedAlbum] = {
    val albumRelativePathToFiles = mediaFiles.groupBy(f => RelativePaths.getFolderPath(f.relativePath))
    for ((albumRelativePath, albumFiles) <- albumRelativePathToFiles.toVector) yield {
      val albumTitle = {
        def fallback = RelativePaths.getFilename(albumRelativePath)
        albumFiles.flatMap(_.album) match {
          case Seq() => fallback
          case albumNames =>
            getDominantStringIgnoringCase(albumNames, minimalFraction = 0.6) getOrElse fallback
        }
      }

      val canonicalArtistName = {
        def dominantCanonical(artistNames: Seq[String]): Option[String] = artistNames match {
          case Seq() => None
          case names =>
            getDominantStringIgnoringCase(
              names.map(artistAssigner.canonicalArtistName),
              minimalFraction = 0.6)
        }
        dominantCanonical(albumFiles.flatMap(_.albumartist))
          .orElse(dominantCanonical(albumFiles.flatMap(_.artist)))
      }

      val year = {
        val years = albumFiles.flatMap(file => file.year flatMap parseFirstInt)
        years.distinct match {
          case Seq(y) => Some(y)
          case _      => None
        }
      }

      ParsedAlbum(
        relativePath = albumRelativePath,
        title = albumTitle,
        canonicalArtistName = canonicalArtistName,
        songs = albumFiles.map(file => parseSong(file, albumFiles, artistAssigner)),
        year = year
      )
    }
  }

  private def parseSong(file: MediaFile,
                        allSongsInAlbum: Seq[MediaFile],
                        artistAssigner: ArtistAssigner): ParsedSong = {
    val filename = RelativePaths.getFilename(file.relativePath)
    def trackNumberFromIndex: Int = allSongsInAlbum.map(_.relativePath).sorted.indexOf(file.relativePath) + 1
    val allSongsHaveUniqueTrackNumber: Boolean = {
      if (allSongsInAlbum.forall(_.trackNumber.isDefined)) {
        val allTrackNumbers = allSongsInAlbum.map(f => parseFirstInt(f.trackNumber.get))
        allTrackNumbers.forall(_.isDefined) && allTrackNumbers.distinct.size == allTrackNumbers.size
      } else {
        false
      }
    }

    ParsedSong(
      filename = filename,
      title = file.title getOrElse withoutExtension(filename),
      canonicalArtistName = file.artist map artistAssigner.canonicalArtistName,
      trackNumber =
        if (allSongsHaveUniqueTrackNumber) parseFirstInt(file.trackNumber.get).get else trackNumberFromIndex,
      duration = file.duration,
      disc = file.disc flatMap parseFirstInt getOrElse 1
    )
  }

  private def parseFirstInt(string: String): Option[Int] = {
    raw"\d+".r.findFirstIn(string).map(_.toInt)
  }

  private def getDominantStringIgnoringCase(strings: Iterable[String],
                                            minimalFraction: Double): Option[String] = {
    require(strings.nonEmpty)
    val lowercaseStrings = strings.toVector.map(_.toLowerCase)
    val mostCommonLowerCaseString = getMostCommonString(lowercaseStrings)
    val mostCommonFraction = 1.0 * lowercaseStrings.count(_ == mostCommonLowerCaseString) / lowercaseStrings.size
    if (mostCommonFraction >= minimalFraction) {
      Some(getMostCommonString(strings.filter(_.toLowerCase == mostCommonLowerCaseString)))
    } else {
      None
    }
  }

  private def withoutExtension(filename: String): String = {
    Splitter.on('.').split(filename) match {
      case Seq(single) => single
      case parts =>
        parts.slice(0, parts.size - 1).mkString(".").trim match {
          case "" => filename
          case s  => s
        }
    }
  }
}
object AlbumParser {
  case class ParsedAlbum(relativePath: String,
                         title: String,
                         canonicalArtistName: Option[String],
                         songs: Seq[ParsedSong],
                         year: Option[Int])
  case class ParsedSong(filename: String,
                        title: String,
                        canonicalArtistName: Option[String],
                        trackNumber: Int,
                        duration: FiniteDuration,
                        disc: Int)
}
