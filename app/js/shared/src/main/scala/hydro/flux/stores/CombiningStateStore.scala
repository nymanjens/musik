package hydro.flux.stores

import scala.collection.immutable.Seq

private final class CombiningStateStore[InputStateA, InputStateB, OutputState](
    storeA: StateStore[InputStateA],
    storeB: StateStore[InputStateB],
    combinerFunction: (InputStateA, InputStateB) => OutputState)
    extends StateStore[OutputState] {
  require(storeA.stateUpdateListeners.isEmpty, "Combining should happen on a newly created store")
  require(storeB.stateUpdateListeners.isEmpty, "Combining should happen on a newly created store")

  override def state: OutputState = {
    combinerFunction(storeA.state, storeB.state)
  }

  override protected def onStateUpdateListenersChange(): Unit = {
    for (inputStore <- Seq(storeA, storeB)) {
      if (this.stateUpdateListeners.isEmpty) {
        inputStore.deregister(InputStoreListener)
      } else {
        if (!(inputStore.stateUpdateListeners contains InputStoreListener)) {
          inputStore.register(InputStoreListener)
        }
      }
    }
  }

  object InputStoreListener extends StateStore.Listener {
    override def onStateUpdate(): Unit = {
      CombiningStateStore.this.invokeStateUpdateListeners()
    }
  }
}

object CombiningStateStore {
  def apply[InputStateA, InputStateB, OutputState](
      storeA: StateStore[InputStateA],
      storeB: StateStore[InputStateB],
      combinerFunction: (InputStateA, InputStateB) => OutputState): StateStore[OutputState] = {
    new CombiningStateStore(storeA, storeB, combinerFunction)
  }
}
