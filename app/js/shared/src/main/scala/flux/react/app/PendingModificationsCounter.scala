package flux.react.app

import common.LoggingUtils.logExceptions
import flux.react.common.HydroReactComponent
import flux.stores.PendingModificationsStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class PendingModificationsCounter(
    implicit pendingModificationsStore: PendingModificationsStore)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component()
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      pendingModificationsStore,
      _.copy(numberOfModifications = pendingModificationsStore.state.numberOfModifications))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(numberOfModifications: Int = 0)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.numberOfModifications match {
        case 0 =>
          <.span()
        case numberOfModifications =>
          <.span(
            ^.className := "navbar-brand pending-modifications",
            <.i(^.className := "glyphicon-hourglass"),
            " ",
            numberOfModifications)
      }
    }
  }
}
