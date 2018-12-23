package flux.react.uielements

import common.I18n
import flux.router.Page
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess

final class PageHeader(implicit i18n: I18n, entityAccess: EntityAccess) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      <.h1(
        ^.className := "page-header",
        <.i(^.className := props.iconClass),
        " ",
        props.title,
      )
    }
    .build
  private val waitForFuture = new WaitForFuture[String]

  // **************** API ****************//
  def apply(page: Page, title: String = null): VdomElement = {
    def newComponent(title: String): VdomElement =
      component(Props(title = title, iconClass = page.iconClass))()
    if (title != null) {
      newComponent(title = title)
    } else {
      waitForFuture(futureInput = page.title, waitingElement = newComponent(title = "")) { titleFromPage =>
        newComponent(title = titleFromPage)
      }
    }
  }

  // **************** Private inner types ****************//
  private case class Props(title: String, iconClass: String)(implicit val i18n: I18n)
}
