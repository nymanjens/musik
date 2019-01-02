package app.models.modification

import app.models.media._
import app.models.user.User
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

object EntityTypes {

  lazy val all: Seq[EntityType.any] =
    Seq(User.Type, Song.Type, Album.Type, Artist.Type, PlaylistEntry.Type, PlayStatus.Type)
}
