package app.flux.stores

import app.flux.stores.PendingModificationsStore.State
import app.models.modification.EntityModification
import app.models.user.User
import hydro.flux.stores.StateStore
import hydro.models.access.JsEntityAccess

import scala.collection.immutable.Seq
import scala.collection.mutable

final class PendingModificationsStore(implicit jsEntityAccess: JsEntityAccess) extends StateStore[State] {
  jsEntityAccess.registerListener(JsEntityAccessListener)

  private var _state: State = State(numberOfModifications = 0)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private state helper methods ****************//
  private def setState(state: State): Unit = {
    val originalState = _state
    _state = state
    if (_state != originalState) {
      invokeStateUpdateListeners()
    }
  }

  // **************** Private inner types ****************//
  object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit =
      onAnyChange()

    override def pendingModificationsPersistedLocally(): Unit = onAnyChange()

    private def onAnyChange(): Unit = {
      if (jsEntityAccess.pendingModifications.persistedLocally) {
        setState(
          State(
            numberOfModifications = getModificationsSize(jsEntityAccess.pendingModifications.modifications)))
      } else {
        setState(State(numberOfModifications = 0))
      }
    }

    private def getModificationsSize(modifications: Seq[EntityModification]): Int = {
      val affectedTransactionGroupIds = mutable.Set[Long]()
      var nonTransactionEditCount = 0

      for (modification <- modifications) modification.entityType match {
        case User.Type => nonTransactionEditCount += 1
        case _         => nonTransactionEditCount += 1
      }

      affectedTransactionGroupIds.size + nonTransactionEditCount
    }
  }
}
object PendingModificationsStore {

  /** numberOfModifications: Number of pending modifications that have been persisted locally. */
  case class State(numberOfModifications: Int)
}
