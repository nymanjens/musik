package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.flux.router.AppPages
import app.flux.stores.media.AlbumDetailStoreFactory
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsAlbum
import app.models.media.JsSong
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class AlbumDiv(implicit dispatcher: Dispatcher,
                     albumDetailStoreFactory: AlbumDetailStoreFactory,
                     playStatusStore: PlayStatusStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(album: JsAlbum, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(album))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      StateStoresDependency(
        albumDetailStoreFactory.get(props.album.id),
        _ => getNewStateFromStores(props)
      )
    }
    .withStateStoresDependencyFromProps { props =>
      StateStoresDependency(
        playStatusStore,
        _ => getNewStateFromStores(props)
      )
    }

  private def getNewStateFromStores(props: Props): State = {
    val albumDetailState = albumDetailStoreFactory.get(props.album.id).state
    val playStatusState = playStatusStore.state

    val songs = albumDetailState.map(_.songs) getOrElse Seq()
    val isCurrentAlbum = {
      for {
        state <- playStatusState
        currentEntry <- state.currentPlaylistEntry
      } yield songs.exists(_.id == currentEntry.song.id)
    } getOrElse false
    val isNowPlaying = isCurrentAlbum && playStatusState.get.hasStarted

    State(
      songs = songs,
      isCurrentAlbum = isCurrentAlbum,
      isNowPlaying = isNowPlaying,
    )
  }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(album: JsAlbum)(implicit val router: RouterContext)
  protected case class State(songs: Seq[JsSong] = Seq(),
                             isCurrentAlbum: Boolean = false,
                             isNowPlaying: Boolean = false)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      val buttons = <.div(
        Bootstrap.Glyphicon("plus-sign")(
          ^.onClick --> addSongsToPlaylistCallback(
            albumId = props.album.id,
            placement = Placement.AfterCurrentSong)
        ),
        " ",
        Bootstrap.Glyphicon("circle-arrow-down")(
          ^.onClick --> addSongsToPlaylistCallback(albumId = props.album.id, placement = Placement.AtEnd)
        )
      )

      val yearInfo: Option[VdomTag] = props.album.year map { year =>
        <.span(
          Bootstrap.FontAwesomeIcon("microphone"),
          " ",
          year
        )
      }
      val pathInfo: VdomTag =
        <.span(
          Bootstrap.FontAwesomeIcon("thumb-tack"),
          " ",
          props.album.relativePath
        )

      GeneralMusicDivs.musicalObjectWithButtons(
        icon = Bootstrap.Glyphicon("cd"),
        title = router.anchorWithHrefTo(AppPages.Album(props.album.id))(props.album.title),
        extraPiecesOfInfo = Seq() ++ yearInfo :+ pathInfo,
        buttons = Some(buttons),
        isCurrentObject = state.isCurrentAlbum,
        isNowPlaying = state.isNowPlaying,
      )(
        ^.className := "album-div",
      )
    }

    // **************** Private helper methods ****************//
    private def addSongsToPlaylistCallback(albumId: Long, placement: Placement): Callback =
      Callback.future(async {
        // Force wait until the state has been loaded
        val albumState = await(albumDetailStoreFactory.get(albumId).stateFuture)
        await(
          dispatcher.dispatch(
            AppActions.AddSongsToPlaylist(songIds = albumState.songs.map(_.id), placement = placement)))
        Callback.empty
      })

  }

}
