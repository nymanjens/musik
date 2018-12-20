package flux.react.uielements.media

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.PlayStatusStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

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

      val stopAfterCurrentSong = state.storeState.stopAfterCurrentSong

      <.div(state.storeState.currentPlaylistEntry match {
        case None => "Empty playlist"
        case Some(playlistEntry) =>
          <.div(
            ^.className := "music-player-bar",
            <.div(
              ^.className := "container",
              <.div(
                ^.className := "metadata",
                <.span(
                  ^.className := "title",
                  playlistEntry.song.title,
                ),
                ^^.ifThen(playlistEntry.song.artist) { artist =>
                  <.span(
                    ^.className := "artist",
                    artist.name,
                  )
                },
                <.span(
                  ^.className := "album",
                  playlistEntry.song.album.title,
                ),
              ),
              <.div(
                ^.className := "controls",
                <.button(
                  ^.className := "btn btn-primary",
                  <.i(^.className := "fa fa-step-backward"),
                ),
                " ",
                <.button(
                  ^.className := "btn btn-primary",
                  <.i(^.className := "fa fa-step-forward"),
                ),
                " ",
                <.button(
                  ^^.classes(
                    Seq("btn", "btn-primary") ++ (if (stopAfterCurrentSong) Seq("active") else Seq())),
                  ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.toggleStopAfterCurrentSong()),
                  <.i(^.className := "fa fa-fast-forward"),
                  <.i(^.className := "fa fa-stop"),
                  " ",
                  <.i(
                    ^.className := (if (stopAfterCurrentSong) "fa fa-check-square-o" else "fa fa-square-o"),
                    ^.style := js.Dictionary("width" -> "0.8em"),
                  ),
                ),
              ),
              uielements.media.RawMusicPlayer(
                ref = musicPlayerRef,
                src = s"/media/${playlistEntry.song.relativePath}",
                playing = state.storeState.hasStarted,
                onEnded = () => playStatusStore.indicateSongEnded(),
                onPlayingChanged = playing => playStatusStore.togglePlay(playing),
              ),
            ),
          )
      })
    }
  }
}
