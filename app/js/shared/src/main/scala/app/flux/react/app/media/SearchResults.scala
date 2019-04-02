package app.flux.react.app.media

import app.flux.react.uielements
import app.flux.stores.media.ComplexQueryStoreFactory
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class SearchResults(implicit pageHeader: PageHeader,
                                       artistDiv: uielements.media.ArtistDiv,
                                       albumDiv: uielements.media.AlbumDiv,
                                       enqueueableSongDiv: uielements.media.EnqueueableSongDiv,
                                       complexQueryStoreFactory: ComplexQueryStoreFactory,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(query: String, router: RouterContext): VdomElement = {
    component(Props(query, router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = complexQueryStoreFactory.get(props.query)
      StateStoresDependency(store, _.copy(maybeStoreState = store.state))
    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(query: String, router: RouterContext)
  protected case class State(maybeStoreState: Option[ComplexQueryStoreFactory.State] = None)

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
              <.h2("Artists"),
              storeState.artists.map { artist =>
                artistDiv(artist, key = artist.id)
              }.toVdomArray,
              <<.ifThen(storeState.artists.isEmpty)(noMatches),
              <.h2("Albums"),
              storeState.albums.map { album =>
                albumDiv(album, key = album.id)
              }.toVdomArray,
              <<.ifThen(storeState.albums.isEmpty)(noMatches),
              <.h2("Songs"),
              storeState.songs.map { song =>
                enqueueableSongDiv(song, showArtist = true, showAlbum = true, key = song.id)
              }.toVdomArray,
              <<.ifThen(storeState.songs.isEmpty)(noMatches),
            )
        }
      )
    }

    private def noMatches: VdomElement = {
      <.span("No matches")
    }
  }
}
