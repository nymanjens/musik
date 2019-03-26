package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsAlbum
import app.models.media.JsSong
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class AlbumDiv(implicit dispatcher: Dispatcher, playStatusStore: PlayStatusStore)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(album: JsAlbum, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(album))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
//    .withStateStoresDependencyFromProps { props =>
//      StateStoresDependency(
//        playStatusStore,
//        oldState => {
//          val maybeResult: Option[State] = for {
//            state <- playStatusStore.state
//            currentEntry <- state.currentPlaylistEntry
//          } yield {
//            val isCurrentSong = currentEntry.song.id == props.song.id
//            oldState.copy(isCurrentSong = isCurrentSong, isNowPlaying = isCurrentSong && state.hasStarted)
//          }
//
//          maybeResult getOrElse oldState
//        }
//      )
//    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(album: JsAlbum)(implicit val router: RouterContext)
  protected case class State(isCurrentSong: Boolean = false, isNowPlaying: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      val buttons = <.div(
        Bootstrap.Glyphicon("plus-sign")(
//          ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AfterCurrentSong)
        ),
        " ",
        Bootstrap.Glyphicon("circle-arrow-down")(
//          ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AtEnd)
        )
      )

      GeneralMusicDivs.musicalObjectWithButtons(
        icon = Bootstrap.Glyphicon("cd"),
        title = router.anchorWithHrefTo(AppPages.Album(props.album.id))(props.album.title),
        extraPiecesOfInfo = Seq(),
        buttons = Some(buttons),
      )(
        ^.className := "album-div",
      )
    }

    // **************** Private helper methods ****************//
    private def addToPlaylistCallback(song: JsSong, placement: Placement): Callback = LogExceptionsCallback {
      dispatcher.dispatch(AppActions.AddSongsToPlaylist(songIds = Seq(song.id), placement = placement))
    }
  }

}
