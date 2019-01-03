package app.flux

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.user.User
import hydro.flux.action.Module
import hydro.flux.router.Page
import japgolly.scalajs.react.extra.router.Router

final class ClientAppModule(implicit getInitialDataResponse: GetInitialDataResponse,
                            scalaJsApiClient: ScalaJsApiClient) {

  // Unpack arguments
  implicit private val user: User = getInitialDataResponse.user

  // Create and unpack common modules
  private val commonTimeModule = new hydro.common.time.Module
  implicit private val clock = commonTimeModule.clock
  private val commonModule = new hydro.common.Module
  implicit private val i18n = commonModule.i18n

  // Create and unpack Models Access module
  val modelsAccessModule = new app.models.access.Module
  implicit val entityAccess = modelsAccessModule.entityAccess
  implicit val entityModificationPushClientFactory = modelsAccessModule.entityModificationPushClientFactory

  // Create and unpack Flux action module
  private val fluxActionModule = new Module
  implicit private val dispatcher = fluxActionModule.dispatcher

  // Create and unpack Flux store module
  private val fluxStoresModule = new app.flux.stores.Module
  implicit private val globalMessagesStore = fluxStoresModule.globalMessagesStore
  implicit private val pageLoadingStateStore = fluxStoresModule.pageLoadingStateStore
  implicit private val pendingModificationsStore = fluxStoresModule.pendingModificationsStore
  implicit private val applicationIsOnlineStore = fluxStoresModule.applicationIsOnlineStore
  implicit private val userStore = fluxStoresModule.userStore
  implicit private val playlistStore = fluxStoresModule.playlistStore
  implicit private val playStatusStore = fluxStoresModule.playStatusStore
  implicit private val allArtistsStore = fluxStoresModule.allArtistsStore
  implicit private val albumDetailStoreFactory = fluxStoresModule.albumDetailStoreFactory
  implicit private val artistDetailStoreFactory = fluxStoresModule.artistDetailStoreFactory

  // Create other Flux modules
  implicit private val reactAppModule = new app.flux.react.app.Module
  implicit private val routerModule = new app.flux.router.Module

  val router: Router[Page] = routerModule.router
}
