package app.flux.stores

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import hydro.common.I18n
import app.flux.stores.media._
import app.flux.stores.media.helpers.ComplexQueryFilterFactory
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.LocalDatabaseHasBeenLoadedStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.JsEntityAccess

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   dispatcher: Dispatcher,
                   clock: Clock,
                   scalaJsApiClient: ScalaJsApiClient,
                   hydroPushSocketClientFactory: HydroPushSocketClientFactory,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit val userStore = new UserStore
  implicit val globalMessagesStore = new GlobalMessagesStore
  implicit val pageLoadingStateStore = new PageLoadingStateStore
  implicit val pendingModificationsStore = new PendingModificationsStore
  implicit val applicationIsOnlineStore = new ApplicationIsOnlineStore
  implicit val localDatabaseHasBeenLoadedStore = new LocalDatabaseHasBeenLoadedStore

  implicit private val complexQueryFilterFactory = new ComplexQueryFilterFactory

  implicit val playlistStore = new PlaylistStore
  implicit val playStatusStore = new PlayStatusStore
  implicit val allArtistsStore = new AllArtistsStore
  implicit val albumDetailStoreFactory = new AlbumDetailStoreFactory
  implicit val artistDetailStoreFactory = new ArtistDetailStoreFactory
  implicit val complexQueryStoreFactory = new ComplexQueryStoreFactory
}
