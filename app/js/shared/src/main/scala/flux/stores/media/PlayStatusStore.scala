package flux.stores.media

import flux.action.Dispatcher
import flux.stores.AsyncEntityDerivedStateStore
import flux.stores.media.PlayStatusStore.State
import models.access.DbQueryImplicits._
import models.access.{JsEntityAccess, ModelField}
import models.media.{JsPlaylistEntry, PlayStatus, PlaylistEntry}
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class PlayStatusStore(implicit entityAccess: JsEntityAccess, user: User, dispatcher: Dispatcher)
    extends AsyncEntityDerivedStateStore[State] {

  private var playWasEverStartedInThisSession: Boolean = false

  entityAccess.registerListener(EntityAccessListener)

  // **************** Public mutating API **************** //
  def play(playlistEntryId: Long): Future[Unit] = {
    upsertPlayStatus(currentPlaylistEntryId = playlistEntryId, hasStarted = true)
  }

  def togglePlay(): Future[Unit] = async {
    val state = await(stateFuture)
    await(upsertPlayStatus(hasStarted = !state.hasStarted))
  }

  private def upsertPlayStatus(currentPlaylistEntryId: java.lang.Long = null,
                               hasStarted: java.lang.Boolean = null,
                               stopAfterCurrentSong: java.lang.Boolean = null): Future[Unit] = async {
    def nonNullOrElse(bool: java.lang.Boolean, fallback: Boolean): Boolean = bool match {
      case null                    => fallback
      case java.lang.Boolean.TRUE  => true
      case java.lang.Boolean.FALSE => false
    }
    val maybePlayStatus =
      await(entityAccess.newQuery[PlayStatus]().findOne(ModelField.PlayStatus.userId, user.id))
    val modifications = maybePlayStatus match {
      case Some(playStatus) => Seq()
      case None if currentPlaylistEntryId != null =>
        Seq(
          EntityModification.createAddWithRandomId(PlayStatus(
            currentPlaylistEntryId = currentPlaylistEntryId,
            hasStarted = nonNullOrElse(hasStarted, fallback = false),
            stopAfterCurrentSong = nonNullOrElse(stopAfterCurrentSong, fallback = false),
            userId = user.id
          )))
      case _ => Seq()
    }
    await(entityAccess.persistModifications(modifications))
  }

  // **************** Implementation of base class methods **************** //
  override protected def calculateState(): Future[State] = async {
    val maybePlayStatus =
      await(entityAccess.newQuery[PlayStatus]().findOne(ModelField.PlayStatus.userId, user.id))
    val currentPlaylistEntryId = maybePlayStatus.map(_.currentPlaylistEntryId) match {
      case Some(id) => Some(id)
      case None =>
        await(
          entityAccess.newQuery[PlaylistEntry]().filter(ModelField.PlaylistEntry.userId === user.id).data())
          .sortBy(e => (e.orderToken, e.id))
          .headOption
          .map(_.id)
    }
    val currentPlaylistEntry =
      if (currentPlaylistEntryId.isDefined)
        Some(await(JsPlaylistEntry.fromEntityId(currentPlaylistEntryId.get)))
      else None
    State(
      currentPlaylistEntry = currentPlaylistEntry,
      hasStarted = maybePlayStatus.map(_.hasStarted && playWasEverStartedInThisSession) getOrElse false,
      stopAfterCurrentSong = maybePlayStatus.map(_.stopAfterCurrentSong) getOrElse false
    )
  }

  override protected def modificationImpactsState(entityModification: EntityModification,
                                                  state: State): Boolean =
    entityModification.entityType == EntityType.PlayStatusType ||
      entityModification.entityType == EntityType.PlaylistEntryType

  object EntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit =
      async {
        val userId = user.id
        modifications.collect {
          case EntityModification.Add(PlayStatus(_, /* hasStarted */ true, _, `userId`, _)) |
              EntityModification.Update(PlayStatus(_, /* hasStarted */ true, _, `userId`, _)) =>
            playWasEverStartedInThisSession = true
        }
      }
  }
}
object PlayStatusStore {
  case class State(currentPlaylistEntry: Option[JsPlaylistEntry],
                   hasStarted: Boolean,
                   stopAfterCurrentSong: Boolean)
}
