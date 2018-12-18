package flux.react.app.media

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.media.{PlaylistStore, AllArtistsStore, AlbumDetailStoreFactory, ArtistDetailStoreFactory}
import models.access.JsEntityAccess

final class Module(implicit i18n: I18n,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   entityAccess: JsEntityAccess,
                   playlistStore: PlaylistStore,
                   allArtistsStore: AllArtistsStore,
                   albumDetailStoreFactory: AlbumDetailStoreFactory,
                   artistDetailStoreFactory: ArtistDetailStoreFactory) {

  implicit lazy val home = new Home
  implicit lazy val playlist = new Playlist
  implicit lazy val allArtists = new AllArtists
}
