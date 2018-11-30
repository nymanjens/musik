package flux.react.app.media

import common.I18n
import common.LoggingUtils.logExceptions
import flux.action.Dispatcher
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess

private[app] final class Home(implicit entityAccess: EntityAccess, i18n: I18n, dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private type State = Unit

  private class Backend($ : BackendScope[Props, State]) {

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        uielements.PageHeader(router.currentPage),
        <.div("Hello world!")
      )
    }
  }
}
