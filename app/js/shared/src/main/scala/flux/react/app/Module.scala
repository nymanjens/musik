package flux.react.app

import hydro.flux.react.uielements.PageHeader
import common.I18n
import common.time.Clock
import flux.react.uielements
import flux.react.uielements.media.MusicPlayerDiv
import flux.stores._
import flux.stores.media._
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import models.access.JsEntityAccess
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   pageHeader: PageHeader,
                   songDiv: uielements.media.SongDiv,
                   musicPlayerDiv: MusicPlayerDiv,
                   globalMessagesStore: GlobalMessagesStore,
                   pageLoadingStateStore: PageLoadingStateStore,
                   pendingModificationsStore: PendingModificationsStore,
                   applicationIsOnlineStore: ApplicationIsOnlineStore,
                   userStore: UserStore,
                   playlistStore: PlaylistStore,
                   playStatusStore: PlayStatusStore,
                   allArtistsStore: AllArtistsStore,
                   albumDetailStoreFactory: AlbumDetailStoreFactory,
                   artistDetailStoreFactory: ArtistDetailStoreFactory,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  // Configuration of submodules
  private val hydroUielementsModule = new hydro.flux.react.uielements.Module
  private val userManagementModule = new hydro.flux.react.uielements.usermanagement.Module
  private val mediaModule = new flux.react.app.media.Module

  implicit private lazy val globalMessages = hydroUielementsModule.globalMessages
  implicit private lazy val pageLoadingSpinner = hydroUielementsModule.pageLoadingSpinner
  implicit private lazy val applicationDisconnectedIcon =
    hydroUielementsModule.applicationDisconnectedIcon
  implicit private lazy val pendingModificationsCounter =
    hydroUielementsModule.pendingModificationsCounter

  implicit private lazy val menu: Menu = new Menu

  implicit lazy val layout: Layout = new Layout

  implicit lazy val userProfile = userManagementModule.userProfile
  implicit lazy val userAdministration = userManagementModule.userAdministration

  implicit lazy val home = mediaModule.home
  implicit lazy val playlist = mediaModule.playlist
  implicit lazy val allArtists = mediaModule.allArtists
  implicit lazy val artistDetail = mediaModule.artistDetail
  implicit lazy val albumDetail = mediaModule.albumDetail
}
