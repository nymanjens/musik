package flux.stores.media

import flux.stores.media.AllArtistsStore.State
import hydro.flux.stores.AsyncEntityDerivedStateStore
import models.access.DbQuery.Sorting
import models.access.JsEntityAccess
import models.access.ModelField
import models.media._
import models.modification.EntityModification
import models.modification.EntityType
import models.user.User

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AllArtistsStore(implicit entityAccess: JsEntityAccess, user: User)
    extends AsyncEntityDerivedStateStore[State] {

  // **************** Implementation of base class methods **************** //
  override protected def calculateState(): Future[State] = async {
    val artists =
      await(entityAccess.newQuery[Artist]().data())
        .sortWith((a, b) => (a.name compareToIgnoreCase b.name) < 0)
    State(artists = artists.map(JsArtist.fromEntity))
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == EntityType.ArtistType
}
object AllArtistsStore {
  case class State(artists: Seq[JsArtist])
}
