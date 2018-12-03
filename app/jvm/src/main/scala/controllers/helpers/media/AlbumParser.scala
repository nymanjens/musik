package controllers.helpers.media

import com.google.inject.Inject
import controllers.helpers.media.AlbumParser.ParsedAlbum
import controllers.helpers.media.ArtistAssignerFactory.ArtistAssigner
import controllers.helpers.media.MediaScanner.MediaFile

import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.Seq

final class AlbumParser @Inject()() {

  def parse(mediaFiles: Seq[MediaFile], artistAssigner: ArtistAssigner): Seq[ParsedAlbum] = ???

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
