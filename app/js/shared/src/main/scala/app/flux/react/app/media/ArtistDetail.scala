package app.flux.react.app.media

import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import app.flux.react.uielements
import app.flux.stores.media.ArtistDetailStoreFactory
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class ArtistDetail(implicit pageHeader: PageHeader,
                                      albumDiv: uielements.media.AlbumDiv,
                                      enqueueableSongDiv: uielements.media.EnqueueableSongDiv,
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

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        state.maybeStoreState match {
          case None =>
            <.div("Loading...")
          case Some(storeState) =>
            <.div(
              <<.ifThen(storeState.albums.nonEmpty) {
                <.h2("Albums")
              },
              storeState.albums.map { album =>
                albumDiv(album, key = album.id)
              }.toVdomArray,
              <<.ifThen(storeState.songsWithoutAlbum.nonEmpty) {
                <.h2("Songs")
              },
              storeState.songsWithoutAlbum.map { song =>
                enqueueableSongDiv(song, showArtist = false, showAlbum = true, key = song.id)
              }.toVdomArray
            )
        }
      )
    }
  }
}
