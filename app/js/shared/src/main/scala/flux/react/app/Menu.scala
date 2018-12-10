package flux.react.app

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.time.Clock
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import flux.react.uielements
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import jsfacades.Mousetrap
import models.access.EntityAccess
import models.user.User

private[app] final class Menu(implicit entityAccess: EntityAccess, user: User, clock: Clock, i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.props))
    .componentDidMount(scope => scope.backend.didMount(scope.props))
    .componentWillReceiveProps(scope => scope.backend.configureKeyboardShortcuts(scope.nextProps.router))
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private type State = Unit

  private class Backend(val $ : BackendScope[Props, State]) {
    val queryInputRef = uielements.input.TextInput.ref()

    def willMount(props: Props): Callback = LogExceptionsCallback {
      configureKeyboardShortcuts(props.router).runNow()
    }
    def didMount(props: Props): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        // TODO: Restore global search
        //case page: Page.Search => {
        //  scope.backend.queryInputRef().setValue(page.query)
        //}
        case _ =>
      }
    }

    def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      def menuItem(label: String, page: Page, iconClass: String = null): VdomElement =
        router
          .anchorWithHrefTo(page)(
            ^^.ifThen(page == props.router.currentPage) { ^.className := "active" },
            // Add underscore to force rerender to fix bug when mouse is on current menu item
            ^.key := (page.toString + (if (page == props.router.currentPage) "_" else "")),
            <.i(^.className := Option(iconClass) getOrElse page.iconClass),
            " ",
            <.span(^.dangerouslySetInnerHtml := label)
          )

      <.ul(
        ^.className := "nav",
        ^.id := "side-menu",
        // TODO: Restore global search
        //<.li(
        //  ^.className := "sidebar-search",
        //  <.form(
        //    <.div(
        //      ^.className := "input-group custom-search-form",
        //      uielements.input
        //        .TextInput(
        //          ref = queryInputRef,
        //          name = "query",
        //          placeholder = i18n("app.search"),
        //          classes = Seq("form-control")),
        //      <.span(
        //        ^.className := "input-group-btn",
        //        <.button(
        //          ^.className := "btn btn-default",
        //          ^.tpe := "submit",
        //          ^.onClick ==> { (e: ReactEventFromInput) =>
        //            LogExceptionsCallback {
        //              e.preventDefault()
        //
        //              queryInputRef().value match {
        //                case Some(query) =>
        //                // TODO: Fix
        //                //props.router.setPage(Page.Search(query))
        //                case None =>
        //              }
        //            }
        //          },
        //          <.i(^.className := "fa fa-search")
        //        )
        //      )
        //    ))
        //),
        <.li(
          menuItem("<u>H</u>ome", Page.Home),
          menuItem("<u>P</u>laylist", Page.Playlist),
          menuItem("<u>A</u>rtists", Page.Artists)
        )
      )
    }

    def configureKeyboardShortcuts(implicit router: RouterContext): Callback = LogExceptionsCallback {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bindGlobal(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      def bindToPage(shortcut: String, page: Page): Unit =
        bind(shortcut, () => {
          router.setPage(page)
        })

      bind("shift+alt+f", () => queryInputRef().focus())
      bindToPage("shift+alt+h", Page.Home)
      bindToPage("shift+alt+p", Page.Playlist)
      bindToPage("shift+alt+a", Page.Artists)
    }
  }
}
