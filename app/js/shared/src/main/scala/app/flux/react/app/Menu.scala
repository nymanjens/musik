package app.flux.react.app

import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.flux.react.uielements.SbadminMenu
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.RouterContext
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class Menu(implicit playStatusStore: PlayStatusStore, sbadminMenu: SbadminMenu) {

  // **************** API ****************//
  def apply(implicit router: RouterContext): VdomElement = {
    sbadminMenu(
      menuItems = Seq(
        Seq(
          MenuItem("<u>H</u>ome", AppPages.Home, shortcuts = Seq("shift+alt+h")),
          MenuItem("<u>P</u>laylist", AppPages.Playlist, shortcuts = Seq("shift+alt+p")),
          MenuItem("<u>A</u>rtists", AppPages.Artists, shortcuts = Seq("shift+alt+a")),
        ),
      ),
      enableSearch = false,
      router = router,
      configureAdditionalKeyboardShortcuts = () => configureAdditionalKeyboardShortcuts(),
    )
  }

  private def configureAdditionalKeyboardShortcuts()(implicit router: RouterContext): Unit = {
    def bind(shortcut: String, runnable: () => Unit): Unit = {
      Mousetrap.bind(shortcut, e => {
        e.preventDefault()
        runnable()
      })
    }

    bind("space", () => playStatusStore.togglePlay())
    bind("ctrl+left", () => playStatusStore.advanceEntriesInPlaylist(step = -1))
    bind("ctrl+right", () => playStatusStore.advanceEntriesInPlaylist(step = +1))
    bind("ctrl+shift+space", () => playStatusStore.toggleStopAfterCurrentSong())
  }
}
