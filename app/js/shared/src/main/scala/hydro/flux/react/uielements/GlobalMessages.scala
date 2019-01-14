package hydro.flux.react.uielements

import app.flux.stores.GlobalMessagesStore
import app.flux.stores.GlobalMessagesStore.Message
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

import scala.scalajs.js

final class GlobalMessages(implicit globalMessagesStore: GlobalMessagesStore) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component((): Unit)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(globalMessagesStore, _.copy(maybeMessage = globalMessagesStore.state))

  // **************** Implementation of HydroReactComponent types ****************//
  protected type Props = Unit
  protected case class State(maybeMessage: Option[GlobalMessagesStore.Message] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      state.maybeMessage match {
        case None => <.span()
        case Some(message) =>
          Bootstrap.Alert(variant = Variant.info)(
            ^.style := js.Dictionary("marginTop" -> "20px"),
            <.span(
              Bootstrap.Icon(iconClassName(message.messageType))(
                ^.style := js.Dictionary("marginRight" -> "11px"),
              ),
              " "),
            message.string
          )
      }
    }

    private def iconClassName(messageType: Message.Type): String = messageType match {
      case Message.Type.Working => "fa fa-circle-o-notch fa-spin"
      case Message.Type.Success => "fa fa-check"
      case Message.Type.Failure => "fa fa-warning"
    }
  }
}
