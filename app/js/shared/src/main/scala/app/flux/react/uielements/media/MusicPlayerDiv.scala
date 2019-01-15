package app.flux.react.uielements.media

import app.flux.react.uielements
import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

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

      val stopAfterCurrentSong = state.storeState.stopAfterCurrentSong

      state.storeState.currentPlaylistEntry match {
        case None => <.span()
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
                  router.anchorWithHrefTo(AppPages.Artist(artist.id))(
                    ^.className := "artist",
                    artist.name,
                  )
                },
                router.anchorWithHrefTo(AppPages.Album(playlistEntry.song.album.id))(
                  ^.className := "album",
                  playlistEntry.song.album.title,
                ),
              ),
              <.div(
                ^.className := "controls",
                Bootstrap.Button(Variant.primary)(
                  ^.onClick --> LogExceptionsCallback[Unit](
                    playStatusStore.advanceEntriesInPlaylist(step = -1)),
                  Bootstrap.FontAwesomeIcon("step-backward"),
                ),
                " ",
                Bootstrap.Button(Variant.primary)(
                  ^.onClick --> LogExceptionsCallback[Unit](
                    playStatusStore.advanceEntriesInPlaylist(step = +1)),
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
      }
    }
  }
}
