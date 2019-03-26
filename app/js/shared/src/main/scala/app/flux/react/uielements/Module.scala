package app.flux.react.uielements

import app.flux.react.uielements.media.AlbumDiv
import app.flux.react.uielements.media.ArtistDiv
import hydro.common.I18n
import app.flux.react.uielements.media.MusicPlayerDiv
import app.flux.react.uielements.media.PlaylistEntryDiv
import app.flux.react.uielements.media.EnqueueableSongDiv
import app.flux.stores.media.ArtistDetailStoreFactory
import app.flux.stores.media.PlayStatusStore
import hydro.flux.action.Dispatcher
import hydro.models.access.EntityAccess

final class Module(implicit i18n: I18n,
                   entityAccess: EntityAccess,
                   dispatcher: Dispatcher,
                   playStatusStore: PlayStatusStore,
                   artistDetailStoreFactory: ArtistDetailStoreFactory) {

  implicit lazy val enqueueableSongDiv = new EnqueueableSongDiv
  implicit lazy val musicPlayerDiv = new MusicPlayerDiv
  implicit lazy val artistDiv = new ArtistDiv
  implicit lazy val albumDiv = new AlbumDiv
  implicit lazy val playlistEntryDiv = new PlaylistEntryDiv
}
