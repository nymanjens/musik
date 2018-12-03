package controllers.helpers.media

import java.io.IOException
import java.nio.file.{Files, Path, Paths}

import com.google.common.io.MoreFiles
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import controllers.helpers.media.AlbumParser.ParsedAlbum
import controllers.helpers.media.MediaScanner.{AddedAndRemovedMedia, MediaFile}
import models.access.JvmEntityAccess

import scala.collection.immutable.Seq
import scala.concurrent.duration.{FiniteDuration, _}

final class StoredMediaSyncer @Inject()(implicit entityAccess: JvmEntityAccess) {

  def addEntitiesFromParsedAlbums(albums: Seq[ParsedAlbum]): Unit = ???

  def removeEntitiesFromRelativeSongPaths(relativeSongPaths: Seq[String]): Unit = ???
}
