package flux.react.app

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import common.LoggingUtils.logExceptions
import common.time.Clock
import flux.react.ReactVdomUtils.^^
import flux.router.Page
import flux.router.RouterContext
import flux.react.uielements
import flux.stores.media.PlayStatusStore
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.input.TextInput
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import jsfacades.Mousetrap
import models.access.EntityAccess
import models.user.User

private[app] final class Menu(implicit entityAccess: EntityAccess,
                              user: User,
                              clock: Clock,
                              i18n: I18n,
                              playStatusStore: PlayStatusStore)
    extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)

  protected class Backend(val $ : BackendScope[Props, State])
      extends BackendBase($)
      with WillMount
      with DidMount
      with WillReceiveProps {
    val queryInputRef = TextInput.ref()

    override def willMount(props: Props, state: State): Callback = configureKeyboardShortcuts(props.router)

    override def didMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.router.currentPage match {
        // TODO: Restore global search
        //case page: Page.Search => {
        //  scope.backend.queryInputRef().setValue(page.query)
        //}
        case _ =>
      }
    }

    override def willReceiveProps(currentProps: Props, nextProps: Props, state: State): Callback =
      configureKeyboardShortcuts(nextProps.router)

    override def render(props: Props, state: State) = logExceptions {
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
      def bindGlobal(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bindGlobal(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      def bindGlobalToPage(shortcut: String, page: Page): Unit =
        bindGlobal(shortcut, () => {
          router.setPage(page)
        })

      bindGlobal("shift+alt+f", () => queryInputRef().focus())
      bindGlobalToPage("shift+alt+h", Page.Home)
      bindGlobalToPage("shift+alt+p", Page.Playlist)
      bindGlobalToPage("shift+alt+a", Page.Artists)

      bind("space", () => playStatusStore.togglePlay())
      bind("ctrl+left", () => playStatusStore.advanceEntriesInPlaylist(step = -1))
      bind("ctrl+right", () => playStatusStore.advanceEntriesInPlaylist(step = +1))
      bind("ctrl+shift+space", () => playStatusStore.toggleStopAfterCurrentSong())
    }
  }
}
