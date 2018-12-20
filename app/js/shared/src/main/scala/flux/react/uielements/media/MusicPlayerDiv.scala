package flux.react.uielements.media

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.Action.AddSongsToPlaylist.Placement
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.^^
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.PlayStatusStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.{JsPlaylistEntry, JsSong}

import scala.collection.immutable.Seq

final class MusicPlayerDiv(implicit playStatusStore: PlayStatusStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State())
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    component(Props())
  }

  // **************** Private inner types ****************//
  private case class Props()(implicit val router: RouterContext)
  private case class State(storeState: PlayStatusStore.State = PlayStatusStore.State.nullInstance)

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    private val musicPlayerRef = uielements.media.RawMusicPlayer.ref()

    def willMount(state: State): Callback = LogExceptionsCallback {
      playStatusStore.register(this)
      $.modState(
        state =>
          logExceptions(
            state.copy(storeState = playStatusStore.state getOrElse PlayStatusStore.State.nullInstance)))
        .runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      playStatusStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(
        state =>
          logExceptions(
            state.copy(storeState = playStatusStore.state getOrElse PlayStatusStore.State.nullInstance)))
        .runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.div(state.storeState.currentPlaylistEntry match {
        case None => "Empty playlist"
        case Some(currentPlaylistEntry) =>
          <.div(
            <.div(
              currentPlaylistEntry.song.title,
              " ",
              <.button(
                ^.className := "btn btn-success btn-sm",
                <.i(^.className := "fa fa-fast-forward"),
                <.i(^.className := "fa fa-stop"),
                " = ",
                state.storeState.stopAfterCurrentSong.toString
              ),
            ),
            <.div(
              uielements.media.RawMusicPlayer(
                ref = musicPlayerRef,
                src = s"/media/${currentPlaylistEntry.song.relativePath}"
              )
            )
          )
      })
    }
  }
}
