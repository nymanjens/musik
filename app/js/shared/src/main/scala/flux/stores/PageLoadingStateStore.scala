package flux.stores

import flux.action.Actions
import hydro.flux.action.StandardActions
import flux.stores.PageLoadingStateStore.State
import hydro.flux.action.{Action, Dispatcher}

final class PageLoadingStateStore(implicit dispatcher: Dispatcher) extends StateStore[State] {
  dispatcher.registerPartialSync(dispatcherListener)

  private var _state: State = State(isLoading = false)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private dispatcher methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case StandardActions.SetPageLoadingState(isLoading) =>
      setState(State(isLoading = isLoading))
  }

  // **************** Private state helper methods ****************//
  private def setState(state: State): Unit = {
    val originalState = _state
    _state = state
    if (_state != originalState) {
      invokeStateUpdateListeners()
    }
  }
}

object PageLoadingStateStore {
  case class State(isLoading: Boolean)
}
