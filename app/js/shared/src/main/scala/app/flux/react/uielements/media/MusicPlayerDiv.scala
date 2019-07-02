package app.flux.react.uielements.media

import hydro.flux.react.ReactVdomUtils.<<
import app.flux.react.uielements
import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsPlaylistEntry
import app.models.media.JsSong
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

final class MusicPlayerDiv(implicit playStatusStore: PlayStatusStore) extends HydroReactComponent {

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      playStatusStore,
      _.copy(storeState = playStatusStore.state getOrElse PlayStatusStore.State.nullInstance))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()(implicit val router: RouterContext)
  protected case class State(storeState: PlayStatusStore.State = PlayStatusStore.State.nullInstance)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    private val musicPlayerRef = uielements.media.RawMusicPlayer.ref()

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router
      implicit val _: State = state

      val stopAfterCurrentSong = state.storeState.stopAfterCurrentSong
      val isRemoteControl = state.storeState.isRemoteControl

      state.storeState.currentPlaylistEntry match {
        case None => <.span()
        case Some(playlistEntry) =>
          <.div(
            ^.className := "music-player-bar",
            <.div(
              ^.className := "container",
              metadata(playlistEntry),
              controlsButtons(),
              if (isRemoteControl) {
                remoteControlButtons()
              } else {
                <.span(
                  uielements.media.RawMusicPlayer(
                    ref = musicPlayerRef,
                    src = playlistEntry.song.mediaSrc,
                    playing = state.storeState.hasStarted,
                    onEnded = () => playStatusStore.indicateSongEnded(),
                    onPlayingChanged = playing => playStatusStore.togglePlay(playing),
                  ),
                  mediaPreloader(state.storeState.nextPlaylistEntry)
                )
              },
            ),
          )
      }
    }

    private def metadata(playlistEntry: JsPlaylistEntry)(implicit state: State,
                                                         router: RouterContext): VdomElement = {
      <.div(
        ^.className := "metadata",
        <.span(
          ^.className := "title",
          playlistEntry.song.title,
        ),
        ^^.ifDefined(playlistEntry.song.artist) { artist =>
          router.anchorWithHrefTo(AppPages.Artist(artist.id))(
            ^.className := "artist",
            artist.name,
          )
        },
        router.anchorWithHrefTo(AppPages.Album(playlistEntry.song.album.id))(
          ^.className := "album",
          playlistEntry.song.album.title,
        ),
      )
    }
    private def controlsButtons()(implicit state: State): VdomElement = {
      val stopAfterCurrentSong = state.storeState.stopAfterCurrentSong
      val isRemoteControl = state.storeState.isRemoteControl

      <.div(
        ^.className := "controls",
        Bootstrap.Button(Variant.primary)(
          ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.advanceEntriesInPlaylist(step = -1)),
          Bootstrap.FontAwesomeIcon("step-backward"),
        ),
        " ",
        Bootstrap.Button(Variant.primary)(
          ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.advanceEntriesInPlaylist(step = +1)),
          Bootstrap.FontAwesomeIcon("step-forward"),
        ),
        " ",
        Bootstrap.Button(Variant.primary)(
          ^^.ifThen(stopAfterCurrentSong)(^.className := "active"),
          ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.toggleStopAfterCurrentSong()),
          Bootstrap.FontAwesomeIcon("fast-forward"),
          Bootstrap.FontAwesomeIcon("stop"),
          " ",
          Bootstrap.FontAwesomeIcon(if (stopAfterCurrentSong) "check-square-o" else "square-o")(
            ^.style := js.Dictionary("width" -> "0.8em"),
          )
        ),
        " ",
        Bootstrap.Button(Variant.primary)(
          ^^.ifThen(isRemoteControl)(^.className := "active"),
          ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.toggleRemoteControl()),
          Bootstrap.Glyphicon("headphones"),
          " ",
          Bootstrap.FontAwesomeIcon(if (isRemoteControl) "check-square-o" else "square-o")(
            ^.style := js.Dictionary("width" -> "0.8em"),
          )
        ),
      )
    }

    private def remoteControlButtons()(implicit state: State): VdomElement = {
      <.div(
        ^.className := "remote-control-buttons",
        Bootstrap.Button(Variant.default)(
          ^.onClick --> LogExceptionsCallback[Unit](playStatusStore.togglePlay()),
          if (state.storeState.hasStarted) Bootstrap.FontAwesomeIcon("pause")
          else Bootstrap.FontAwesomeIcon("play"),
        )
      )
    }

    private def mediaPreloader(maybePlaylistEntry: Option[JsPlaylistEntry]): VdomNode = {
      <<.ifDefined(maybePlaylistEntry) { playlistEntry =>
        <.audio(
          ^.src := playlistEntry.song.mediaSrc,
          ^.preload := "auto",
        )
      }
    }
  }
}
