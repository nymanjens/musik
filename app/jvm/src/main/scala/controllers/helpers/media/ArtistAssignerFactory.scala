package controllers.helpers.media

import com.google.inject.Inject
import controllers.helpers.media.MediaScanner.MediaFile
import models.access.JvmEntityAccess

final class ArtistAssignerFactory @Inject()(implicit entityAccess: JvmEntityAccess) {

  def fromMediaFiles(mediaFiles: Seq[MediaFile]): ArtistAssigner = {
    // Also load all artists from entityAccess
    ???
  }
}

object ArtistAssignerFactory {
  final class ArtistAssigner {
    def canonicalArtistName(artistName: String): String = ???
  }
}
