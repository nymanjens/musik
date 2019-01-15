package app.flux.react.app.media

import hydro.common.I18n
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import hydro.models.access.EntityAccess
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class Home(implicit i18n: I18n,
                              entityAccess: EntityAccess,
                              pageHeader: PageHeader,
                              dispatcher: Dispatcher)
    extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router = router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.div("Home page")
    }
  }
}
