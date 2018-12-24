package flux.react.app

import common.I18n
import common.time.Clock
import flux.react.uielements
import flux.react.uielements.media.MusicPlayerDiv
import flux.stores._
import flux.stores.media._
import hydro.flux.action.Dispatcher
import hydro.stores.{ApplicationIsOnlineStore, PageLoadingStateStore, UserStore}
import models.access.JsEntityAccess
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   pageHeader: uielements.PageHeader,
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
  private val hydroSbadminUielementsModule = new hydro.flux.react.uielements.sbadmin.Module
  private val userManagementModule = new hydro.flux.react.uielements.sbadmin.usermanagement.Module
  private val mediaModule = new flux.react.app.media.Module

  implicit private lazy val globalMessages = hydroSbadminUielementsModule.globalMessages
  implicit private lazy val pageLoadingSpinner = hydroSbadminUielementsModule.pageLoadingSpinner
  implicit private lazy val applicationDisconnectedIcon =
    hydroSbadminUielementsModule.applicationDisconnectedIcon
  implicit private lazy val pendingModificationsCounter =
    hydroSbadminUielementsModule.pendingModificationsCounter

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
