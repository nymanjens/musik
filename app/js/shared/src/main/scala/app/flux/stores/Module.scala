package app.flux.stores

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import common.I18n
import hydro.common.time.Clock
import app.flux.stores.media._
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import hydro.models.access.EntityModificationPushClientFactory
import hydro.models.access.JsEntityAccess
import app.models.user.User

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
  implicit val playStatusStore = new PlayStatusStore
  implicit val allArtistsStore = new AllArtistsStore
  implicit val albumDetailStoreFactory = new AlbumDetailStoreFactory
  implicit val artistDetailStoreFactory = new ArtistDetailStoreFactory
}
