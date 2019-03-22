package app.flux.react.app.media

import app.flux.react.uielements
import app.flux.stores.media.AlbumDetailStoreFactory
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class AlbumDetail(implicit pageHeader: PageHeader,
                                     enqueueableSongDiv: uielements.media.EnqueueableSongDiv,
                                     albumDetailStoreFactory: AlbumDetailStoreFactory)
    extends HydroReactComponent {

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
        pageHeader(router.currentPage),
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
  }
}
