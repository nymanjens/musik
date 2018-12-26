package app.flux.react.uielements.media

import common.LoggingUtils.LogExceptionsCallback
import common.LoggingUtils.logExceptions
import app.flux.react.ReactVdomUtils.^^
import app.flux.router.Page
import app.flux.router.RouterContext
import app.flux.react.uielements
import app.flux.stores.media.PlayStatusStore
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
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
                  router.anchorWithHrefTo(Page.Artist(artist.id))(
                    ^.className := "artist",
                    artist.name,
                  )
                },
                router.anchorWithHrefTo(Page.Album(playlistEntry.song.album.id))(
                  ^.className := "album",
                  playlistEntry.song.album.title,
                ),
              ),
              <.div(
                ^.className := "controls",
                <.button(
                  ^.className := "btn btn-primary",
                  ^.onClick --> LogExceptionsCallback[Unit](
                    playStatusStore.advanceEntriesInPlaylist(step = -1)),
                  <.i(^.className := "fa fa-step-backward"),
                ),
                " ",
                <.button(
                  ^.className := "btn btn-primary",
                  ^.onClick --> LogExceptionsCallback[Unit](
                    playStatusStore.advanceEntriesInPlaylist(step = +1)),
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
      }
    }
  }
}
