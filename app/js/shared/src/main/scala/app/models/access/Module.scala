package app.models.access

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.modification.EntityTypes
import app.models.user.User
import hydro.common.time.Clock
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.HybridRemoteDatabaseProxy
import hydro.models.access.JsEntityAccess
import hydro.models.access.JsEntityAccessImpl
import hydro.models.access.LocalDatabaseImpl
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction

import scala.collection.immutable.Seq

final class Module(implicit user: User,
                   clock: Clock,
                   scalaJsApiClient: ScalaJsApiClient,
                   getInitialDataResponse: GetInitialDataResponse) {

  implicit private val secondaryIndexFunction = Module.secondaryIndexFunction
  implicit private val entitySyncLogic = new MusikEntitySyncLogic()

  implicit val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
    new HydroPushSocketClientFactory()

  implicit val entityAccess: JsEntityAccess = {
    val webWorkerModule = new hydro.models.access.webworker.Module()
    implicit val localDatabaseWebWorkerApiStub = webWorkerModule.localDatabaseWebWorkerApiStub
    val localDatabaseFuture = LocalDatabaseImpl.create(separateDbPerCollection = true)
    implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabaseFuture)
    val entityAccess = new JsEntityAccessImpl()

    entityAccess.startCheckingForModifiedEntityUpdates()

    entityAccess
  }
}
object Module {
  val secondaryIndexFunction: SecondaryIndexFunction = SecondaryIndexFunction({
    case User.Type          => Seq()
    case Song.Type          => Seq(ModelFields.Song.albumId, ModelFields.Album.artistId)
    case Album.Type         => Seq(ModelFields.Album.artistId)
    case Artist.Type        => Seq()
    case PlaylistEntry.Type => Seq()
    case PlayStatus.Type    => Seq()
  })
}
