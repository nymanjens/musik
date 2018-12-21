package flux.react.common

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.LoggingUtils.logExceptions

import scala.collection.immutable.Seq
import flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^._

abstract class HydroReactComponent {

  // **************** Protected types to be overridden ****************//
  protected type Props
  protected type State
  protected type Backend <: BackendBase

  // **************** Protected methods to be overridden ****************//
  protected def createBackend: BackendScope[Props, State] => Backend
  protected def initialState: State
  protected def componentName: String = getClass.getSimpleName
  protected def stateStoresDependencies: Seq[StateStoresDependency] = Seq()

  // **************** Protected final methods ****************//
  protected lazy val component = {
    val step1: Builder.Step1[Props] = ScalaComponent.builder[Props](componentName)
    val step2: Builder.Step2[Props, State] =
      if (this.isInstanceOf[HydroReactComponent.Stateless])
        step1.stateless.asInstanceOf[Builder.Step2[Props, State]]
      else step1.initialState[State](initialState)
    val step3: Builder.Step3[Props, State, Backend] = step2.backend(createBackend)
    var step4: Builder.Step4[Props, Children.None, State, Backend] =
      step3.renderPS((scope, props, state) => scope.backend.render(props, state))

    val dummyBackend = createBackend(null)
    if (dummyBackend.isInstanceOf[WillMount]) {
      step4 = step4
        .componentWillMount(scope =>
          scope.backend.asInstanceOf[WillMount].willMount(scope.props, scope.state))
    }
    if (dummyBackend.isInstanceOf[WillUnmount]) {
      step4 = step4
        .componentWillUnmount(scope =>
          scope.backend.asInstanceOf[WillUnmount].willUnmount(scope.props, scope.state))
    }
    if (stateStoresDependencies.nonEmpty) {
      step4 = step4
        .componentWillMount { scope =>
          for (StateStoresDependency(store, _) <- stateStoresDependencies) {
            store.register(scope.backend)
          }
          scope.backend.updateStateFromStoresCallback
        }
        .componentWillUnmount { scope =>
          LogExceptionsCallback {
            for (StateStoresDependency(store, _) <- stateStoresDependencies) {
              store.deregister(scope.backend)
            }
          }
        }
    }
    step4.build
  }

  // **************** Protected final types ****************//
  abstract class BackendBase($ : BackendScope[Props, State]) extends StateStore.Listener {
    def render(props: Props, state: State): VdomElement

    override final def onStateUpdate() = {
      updateStateFromStoresCallback.runNow()
    }

    private[HydroReactComponent] def updateStateFromStoresCallback: Callback = {
      $.modState(oldState =>
        logExceptions {
          var state = oldState
          for (StateStoresDependency(_, stateUpdate) <- stateStoresDependencies) {
            state = stateUpdate(state)
          }
          state
      })
    }
  }
  trait WillMount {
    def willMount(props: Props, state: State): Callback
  }
  trait WillUnmount {
    def willUnmount(props: Props, state: State): Callback
  }

  case class StateStoresDependency(store: StateStore[_], stateUpdate: State => State)
}
object HydroReactComponent {
  abstract class Stateless extends HydroReactComponent {
    type State = Unit
    override final def initialState: State = (): Unit
  }
}
