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

  val fullySyncedLocally: Seq[EntityType.any] = Seq(User.Type, PlaylistEntry.Type, PlayStatus.Type)
  val partiallySynced: Seq[EntityType.any] = Seq(Song.Type, Album.Type, Artist.Type)

  val locallyPersisted: Seq[EntityType.any] = fullySyncedLocally ++ partiallySynced
  def all: Seq[EntityType.any] = locallyPersisted
}
