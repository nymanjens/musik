package flux.react.common

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.LoggingUtils.logExceptions

import scala.collection.immutable.Seq
import flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable

abstract class HydroReactComponent {

  // **************** Protected types to be overridden ****************//
  protected type Props
  protected type State
  protected type Backend <: BackendBase

  // **************** Protected methods to be overridden ****************//
  protected def createBackend: BackendScope[Props, State] => Backend
  protected def initialState: State
  protected def componentName: String = getClass.getSimpleName
  protected def stateStoresDependencies: StateStoresDependencyBuilder => Unit = _ => {}

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
    if (stateStoresDependenciesFromProps.nonEmpty) {
      step4 = step4
        .componentWillMount { scope =>
          logExceptions {
            for (StateStoresDependency(store, _) <- getStateStoresDependencies(scope.props)) {
              store.register(scope.backend)
            }
            scope.backend.updateStateFromStoresCallback(scope.props)
          }
        }
        .componentWillReceiveProps { scope =>
          logExceptions {
            var anythingChanged = false
            for {
              (StateStoresDependency(oldStore, _), StateStoresDependency(newStore, _)) <- getStateStoresDependencies(
                scope.currentProps) zip getStateStoresDependencies(scope.nextProps)
              if oldStore != newStore
            } {
              oldStore.deregister(scope.backend)
              newStore.register(scope.backend)
              anythingChanged = true
            }
            if (anythingChanged) {
              scope.backend.updateStateFromStoresCallback(scope.nextProps)
            } else {
              Callback.empty
            }
          }
        }
        .componentWillUnmount { scope =>
          LogExceptionsCallback {
            for (StateStoresDependency(store, _) <- getStateStoresDependencies(scope.props)) {
              store.deregister(scope.backend)
            }
          }
        }
    }
    step4.build
  }

  // **************** Private helper methods ****************//
  private lazy val stateStoresDependenciesFromProps: Seq[Props => StateStoresDependency] = {
    val resultBuilder = new StateStoresDependencyBuilder
    stateStoresDependencies(resultBuilder)
    resultBuilder.build
  }
  private def getStateStoresDependencies(props: Props): Seq[StateStoresDependency] = {
    stateStoresDependenciesFromProps.map(_.apply(props))
  }

  // **************** Protected types ****************//
  abstract class BackendBase($ : BackendScope[Props, State]) extends StateStore.Listener {
    def render(props: Props, state: State): VdomElement

    override final def onStateUpdate() = {
      $.props.flatMap(updateStateFromStoresCallback).runNow()
    }

    private[HydroReactComponent] def updateStateFromStoresCallback(props: Props): Callback = {
      $.modState(oldState =>
        logExceptions {
          var state = oldState
          for (StateStoresDependency(_, stateUpdate) <- getStateStoresDependencies(props)) {
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

  private case class StateStoresDependency(store: StateStore[_], stateUpdate: State => State)
  class StateStoresDependencyBuilder {
    private val dependencyProviders = mutable.Buffer[Props => StateStoresDependency]()
    def addDependency(store: StateStore[_], stateUpdate: State => State): StateStoresDependencyBuilder = {
      dependencyProviders += (_ => StateStoresDependency(store, stateUpdate))
      this
    }
    def addDependencyFromProps[Store <: StateStore[_]](storeFromProps: Props => Store)(
        stateUpdate: Store => State => State): StateStoresDependencyBuilder = {
      dependencyProviders += { props =>
        val store = storeFromProps(props)
        StateStoresDependency(store, stateUpdate(store))
      }
      this
    }

    private[HydroReactComponent] def build: Seq[Props => StateStoresDependency] = dependencyProviders.toVector
  }
}
object HydroReactComponent {
  abstract class Stateless extends HydroReactComponent {
    type State = Unit
    override final def initialState: State = (): Unit
  }
}
