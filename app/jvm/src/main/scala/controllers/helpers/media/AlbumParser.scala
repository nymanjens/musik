package controllers.helpers.media

import com.google.inject.Inject
import common.CollectionUtils.getMostCommonStringIgnoringCase
import common.{CollectionUtils, RelativePaths}
import controllers.helpers.media.AlbumParser.{ParsedAlbum, ParsedSong}
import controllers.helpers.media.ArtistAssignerFactory.ArtistAssigner
import controllers.helpers.media.MediaScanner.MediaFile

import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.Seq
import scala.util.matching.Regex

final class AlbumParser @Inject()() {

  def parse(mediaFiles: Seq[MediaFile], artistAssigner: ArtistAssigner): Seq[ParsedAlbum] = {
    val albumRelativePathToFiles = mediaFiles.groupBy(f => RelativePaths.getFolderPath(f.relativePath))
    for ((albumRelativePath, albumFiles) <- albumRelativePathToFiles.toVector) yield {
      val albumTitle = albumFiles.flatMap(_.album) match {
        case Seq()      => RelativePaths.getFilename(albumRelativePath)
        case albumNames => getMostCommonStringIgnoringCase(albumNames)
      }

      val canonicalArtistName = {
        def mostCommonCanonical(artistNames: Seq[String]): Option[String] = artistNames match {
          case Seq() => None
          case names => Some(getMostCommonStringIgnoringCase(names.map(artistAssigner.canonicalArtistName)))
        }
        mostCommonCanonical(albumFiles.flatMap(_.albumartist))
          .orElse(mostCommonCanonical(albumFiles.flatMap(_.artist)))
      }

      ParsedAlbum(
        relativePath = albumRelativePath,
        title = albumTitle,
        canonicalArtistName = canonicalArtistName,
        songs = albumFiles.map(file => parseSong(file, albumFiles, artistAssigner))
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
      title = file.title getOrElse filename,
      canonicalArtistName = file.artist map artistAssigner.canonicalArtistName,
      trackNumber =
        if (allSongsHaveUniqueTrackNumber) parseFirstInt(file.trackNumber.get).get else trackNumberFromIndex,
      duration = file.duration,
      year = file.year flatMap parseFirstInt,
      disc = file.disc flatMap parseFirstInt getOrElse 1
    )
  }

  private def parseFirstInt(string: String): Option[Int] = {
    raw"\d+".r.findFirstIn(string).map(_.toInt)
  }
}
object AlbumParser {
  case class ParsedAlbum(relativePath: String,
                         title: String,
                         canonicalArtistName: Option[String],
                         songs: Seq[ParsedSong])
  case class ParsedSong(filename: String,
                        title: String,
                        canonicalArtistName: Option[String],
                        trackNumber: Int,
                        duration: FiniteDuration,
                        year: Option[Int],
                        disc: Int)
}
