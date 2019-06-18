package hydro.flux.stores

import scala.collection.immutable.Seq

/**
  * Abstract base class for any store that exposes a single listenable state.
  *
  * @tparam State An immutable type that contains all state maintained by this store
  */
abstract class StateStore[State] {

  private var _stateUpdateListeners: Seq[StateStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API: To override ****************//
  def state: State

  // **************** Public API: Final ****************//
  final def register(listener: StateStore.Listener): Unit = {
    checkNotCallingListeners()

    _stateUpdateListeners = _stateUpdateListeners :+ listener
    onStateUpdateListenersChange()
  }

  final def deregister(listener: StateStore.Listener): Unit = {
    checkNotCallingListeners()

    _stateUpdateListeners = _stateUpdateListeners.filter(_ != listener)
    onStateUpdateListenersChange()
  }

  // **************** Protected methods to override ****************//
  protected def onStateUpdateListenersChange(): Unit = {}

  // **************** Protected helper methods ****************//
  protected final def invokeStateUpdateListeners(): Unit = {
    checkNotCallingListeners()
    isCallingListeners = true
    _stateUpdateListeners.foreach(_.onStateUpdate())
    isCallingListeners = false
  }

  final def stateUpdateListeners: Seq[StateStore.Listener] = _stateUpdateListeners

  protected final def checkNotCallingListeners(): Unit = {
    require(!isCallingListeners, "checkNotCallingListeners(): But isCallingListeners is true")
  }
}

object StateStore {
  trait Listener {
    def onStateUpdate(): Unit
  }
}
