package flux.stores

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.stores.media.{PlaylistStore, AllArtistsStore, AlbumDetailStoreFactory, ArtistDetailStoreFactory}
import models.access.{EntityModificationPushClientFactory, JsEntityAccess}
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   scalaJsApiClient: ScalaJsApiClient,
                   entityModificationPushClientFactory: EntityModificationPushClientFactory,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit val userStore = new UserStore
  implicit val globalMessagesStore = new GlobalMessagesStore
  implicit val pageLoadingStateStore = new PageLoadingStateStore
  implicit val pendingModificationsStore = new PendingModificationsStore
  implicit val applicationIsOnlineStore = new ApplicationIsOnlineStore

  implicit val playlistStore = new PlaylistStore
  implicit val allArtistsStore = new AllArtistsStore
  implicit val albumDetailStoreFactory = new AlbumDetailStoreFactory
  implicit val artistDetailStoreFactory = new ArtistDetailStoreFactory
}
