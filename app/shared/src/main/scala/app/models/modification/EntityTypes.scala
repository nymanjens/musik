package app.models.modification

import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.user.User
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

object EntityTypes {

  lazy val all: Seq[EntityType.any] =
    Seq(User.Type, Song.Type, Album.Type, Artist.Type, PlaylistEntry.Type, PlayStatus.Type)
}
