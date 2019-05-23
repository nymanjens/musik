package app.flux.react.app.media

import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.flux.react.uielements
import app.flux.stores.media.AlbumDetailStoreFactory
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.async.Async.async
import scala.async.Async.await
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[app] final class AlbumDetail(implicit pageHeader: PageHeader,
                                     enqueueableSongDiv: uielements.media.EnqueueableSongDiv,
                                     albumDetailStoreFactory: AlbumDetailStoreFactory,
                                     dispatcher: Dispatcher,
                                     i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(albumId: Long, router: RouterContext): VdomElement = {
    component(Props(albumId, router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = albumDetailStoreFactory.get(props.albumId)
      StateStoresDependency(store, _.copy(maybeStoreState = store.state))
    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(albumId: Long, router: RouterContext)
  protected case class State(maybeStoreState: Option[AlbumDetailStoreFactory.State] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader.withExtension(router.currentPage) {
          <.span(
            ^.className := "buttons",
            Bootstrap.Glyphicon("plus-sign")(
              ^.onClick --> addSongsToPlaylistCallback(
                albumId = props.albumId,
                placement = Placement.AfterCurrentSong)
            ),
            " ",
            Bootstrap.Glyphicon("circle-arrow-down")(
              ^.onClick --> addSongsToPlaylistCallback(albumId = props.albumId, placement = Placement.AtEnd)
            ),
          )
        },
        state.maybeStoreState match {
          case None =>
            <.div("Loading...")
          case Some(storeState) =>
            storeState.songs.map { song =>
              enqueueableSongDiv(song, showArtist = true, showAlbum = false, key = song.id)
            }.toVdomArray
        }
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
