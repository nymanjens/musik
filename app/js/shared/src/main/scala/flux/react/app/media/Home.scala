package flux.react.app.media

import scala.collection.immutable.Seq
import common.{Formatting, I18n}
import common.LoggingUtils.logExceptions
import flux.action.Dispatcher
import flux.react.router.RouterContext
import flux.react.uielements
import flux.react.uielements.WaitForFuture
import flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.media.{Album, Artist, Song, PlaylistEntry, PlayStatus}

private[app] final class Home(implicit i18n: I18n,
                              entityAccess: EntityAccess,
                              pageHeader: uielements.PageHeader,
                              dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router = router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private type State = Unit

  private class Backend($ : BackendScope[Props, State]) {

    private val musicPlayerRef = uielements.media.RawMusicPlayer.ref()

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        <.div(
          <.button(
            ^.className := "btn btn-default",
            ^.onClick ==> { _ =>
              musicPlayerRef().pause()
              Callback.empty
            },
            "Pause"
          )
        ),
        <.div(
          uielements.media.RawMusicPlayer(
            ref = musicPlayerRef,
            src = "/test"
          )
        )
      )
    }
  }
}
