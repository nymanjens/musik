package app.flux.stores.media

import app.flux.stores.media.AllArtistsStore.State
import app.models.media.Artist
import app.models.media._
import app.models.modification.EntityModification
import app.models.user.User
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.JsEntityAccess

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
    entityModification.entityType == Artist.Type
}
object AllArtistsStore {
  case class State(artists: Seq[JsArtist])
}
