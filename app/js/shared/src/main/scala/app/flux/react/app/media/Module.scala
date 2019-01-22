package app.flux.react.app.media

import hydro.common.I18n
import app.flux.react.uielements
import app.flux.react.uielements.media.ArtistDiv
import app.flux.react.uielements.media.MusicPlayerDiv
import app.flux.react.uielements.media.PlaylistEntryDiv
import app.flux.stores.media.AlbumDetailStoreFactory
import app.flux.stores.media.AllArtistsStore
import app.flux.stores.media.ArtistDetailStoreFactory
import app.flux.stores.media.PlayStatusStore
import app.flux.stores.media.PlaylistStore
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.PageHeader
import hydro.models.access.JsEntityAccess

final class Module(implicit i18n: I18n,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   entityAccess: JsEntityAccess,
                   pageHeader: PageHeader,
                   songDiv: uielements.media.SongDiv,
                   musicPlayerDiv: MusicPlayerDiv,
                   artistDiv: ArtistDiv,
                   playlistEntryDiv: PlaylistEntryDiv,
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
