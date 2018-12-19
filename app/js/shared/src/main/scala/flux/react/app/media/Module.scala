package flux.react.app.media

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.react.uielements
import flux.stores.media.{
  AlbumDetailStoreFactory,
  AllArtistsStore,
  ArtistDetailStoreFactory,
  PlaylistStore,
  PlayStatusStore
}
import models.access.JsEntityAccess

final class Module(implicit i18n: I18n,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   entityAccess: JsEntityAccess,
                   pageHeader: uielements.PageHeader,
                   songDiv: uielements.media.SongDiv,
                   playlistStore: PlaylistStore,
                   playStatusStore: PlayStatusStore,
                   allArtistsStore: AllArtistsStore,
                   albumDetailStoreFactory: AlbumDetailStoreFactory,
                   artistDetailStoreFactory: ArtistDetailStoreFactory) {

  implicit lazy val home = new Home
  implicit lazy val playlist = new Playlist
  implicit lazy val allArtists = new AllArtists
  implicit lazy val artistDetail = new ArtistDetail
  implicit lazy val albumDetail = new AlbumDetail
}
