package controllers.helpers.media

import app.common.CollectionUtils.getMostCommonStringIgnoringCase
import app.common.GuavaReplacement.Iterables.getOnlyElement
import app.common.ScalaUtils.visibleForTesting
import app.models.access.JvmEntityAccess
import app.models.media.Artist
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.inject.Inject
import controllers.helpers.media.ArtistAssignerFactory.ArtistAssigner
import controllers.helpers.media.MediaScanner.MediaFile

import scala.collection.immutable.Seq

final class ArtistAssignerFactory @Inject()(implicit entityAccess: JvmEntityAccess) {

  def fromDbAndMediaFiles(mediaFiles: Seq[MediaFile]): ArtistAssigner = {
    val storedLookupToCanonicalNameMap = {
      val allArtists = entityAccess.newQuerySync[Artist]().data()
      allArtists.map(_.name).groupBy(ArtistAssignerFactory.lookupName).mapValues(getOnlyElement)
    }

    val newLookupToCanonicalNameMap = {
      val allArtists = mediaFiles.flatMap(media => media.artist ++ media.albumartist)
      val newArtistNames = allArtists.filter(artist =>
        !storedLookupToCanonicalNameMap.contains(ArtistAssignerFactory.lookupName(artist)))
      newArtistNames.groupBy(ArtistAssignerFactory.lookupName).mapValues(getMostCommonStringIgnoringCase)
    }

    new ArtistAssigner(storedLookupToCanonicalNameMap ++ newLookupToCanonicalNameMap)
  }
}

object ArtistAssignerFactory {

  private val lookupNameCharMatcher: CharMatcher = {
    val digits = CharMatcher.inRange('0', '9')
    val letters = CharMatcher.inRange('a', 'z') or CharMatcher.inRange('A', 'Z')
    digits or letters
  }

  @visibleForTesting private[media] def lookupName(artistName: String): String = {
    var interim = artistName.toLowerCase
    for (split <- Seq("(", " f/", "/", "&", " and ", ", ")) {
      val firstPart = Splitter.on(split).splitToList(interim).get(0)
      if (firstPart.length >= 5) {
        interim = firstPart
      }
    }
    interim = interim.stripPrefix("the ")
    lookupNameCharMatcher.retainFrom(interim)
  }

  final class ArtistAssigner @visibleForTesting private[media] (
      lookupToCanonicalNameMap: Map[String, String]) {

    def canonicalArtistName(artistName: String): String = lookupToCanonicalNameMap(lookupName(artistName))
  }
}
