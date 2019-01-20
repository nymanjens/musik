package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsPlaylistEntry
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class PlaylistEntryDiv(dispatcher: Dispatcher, playStatusStore: PlayStatusStore)
    extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(playlistEntry: JsPlaylistEntry, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(playlistEntry))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(playlistEntry: JsPlaylistEntry)(implicit val router: RouterContext)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router
      val song = props.playlistEntry.song

      <.div(
        ^.className := "playlist-entry-div",
        <.div(
          ^.className := "main-info",
          Bootstrap.Glyphicon("music"),
          " ",
          song.title,
          " ",
          Bootstrap.Button()(
            ^.onClick --> {
              playStatusStore.play(playlistEntryId = props.playlistEntry.id)
              Callback.empty
            },
            Bootstrap.FontAwesomeIcon("play-circle-o")
          ),
          " ",
          Bootstrap.Button()(
            ^.onClick --> {
              dispatcher.dispatch(AppActions.RemoveEntriesFromPlaylist(Seq(props.playlistEntry.id)))
              Callback.empty
            },
            Bootstrap.FontAwesomeIcon("times-circle-o"),
          )
        ),
        <.div(
          ^.className := "extra-info",
          <<.ifThen(song.artist) { artist =>
            <.span(
              ^.className := "artist",
              Bootstrap.FontAwesomeIcon("user"),
              " ",
              router.anchorWithHrefTo(AppPages.Artist(artist.id))(artist.name),
            )
          },
          <.span(
            ^.className := "album",
            Bootstrap.Glyphicon("cd"),
            " ",
            router.anchorWithHrefTo(AppPages.Album(song.album.id))(song.album.title),
          )
        ),
      )
    }
  }
}
