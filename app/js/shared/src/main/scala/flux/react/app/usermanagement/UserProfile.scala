package flux.react.app.usermanagement

import common.I18n
import flux.react.router.RouterContext
import flux.react.uielements
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class UserProfile(implicit i18n: I18n,
                                     pageHeader: uielements.PageHeader,
                                     updatePasswordForm: UpdatePasswordForm) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) => {
      implicit val router = props.router
      <.span(
        pageHeader(router.currentPage),
        <.div(
          ^.className := "row",
          updatePasswordForm()
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
