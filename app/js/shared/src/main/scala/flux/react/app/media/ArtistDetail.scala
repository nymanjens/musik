package flux.react.app.media

import hydro.flux.react.uielements.PageHeader
import common.LoggingUtils.logExceptions
import flux.router.RouterContext
import flux.react.uielements
import flux.stores.media.ArtistDetailStoreFactory
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class ArtistDetail(implicit pageHeader: PageHeader,
                                      songDiv: uielements.media.SongDiv,
                                      artistDetailStoreFactory: ArtistDetailStoreFactory)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(artistId: Long, router: RouterContext): VdomElement = {
    component(Props(artistId, router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = artistDetailStoreFactory.get(props.artistId)
      StateStoresDependency(store, _.copy(maybeStoreState = store.state))
    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(artistId: Long, router: RouterContext)
  protected case class State(maybeStoreState: Option[ArtistDetailStoreFactory.State] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        state.maybeStoreState match {
          case None =>
            <.div("Loading...")
          case Some(storeState) =>
            <.div(
              storeState.albums.map { album =>
                uielements.media.AlbumDiv(album, key = album.id)
              }.toVdomArray,
              storeState.songsWithoutAlbum.map { song =>
                songDiv(song, key = song.id)
              }.toVdomArray
            )
        }
      )
    }
  }
}
