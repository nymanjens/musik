package flux.react.uielements.media

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.Action.AddSongsToPlaylist.Placement
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.^^
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.media.PlayStatusStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsSong

import scala.collection.immutable.Seq

final class MusicPlayerDiv(implicit playStatusStore: PlayStatusStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    component(Props())
  }

  // **************** Private inner types ****************//
  private case class Props()(implicit val router: RouterContext)
  private type State = Unit

  private class Backend($ : BackendScope[Props, State]) {

    private val musicPlayerRef = uielements.media.RawMusicPlayer.ref()

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.div(
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
