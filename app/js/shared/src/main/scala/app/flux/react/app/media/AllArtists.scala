package app.flux.react.app.media

import app.flux.react.uielements.media.ArtistDiv
import app.flux.stores.media.AllArtistsStore
import app.models.media.JsArtist
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class AllArtists(implicit pageHeader: PageHeader,
                                    allArtistsStore: AllArtistsStore,
                                    artistDiv: ArtistDiv)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(allArtistsStore, _.copy(maybeArtists = allArtistsStore.state.map(_.artists)))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(maybeArtists: Option[Seq[JsArtist]] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        state.maybeArtists match {
          case None =>
            <.div("Loading...")
          case Some(artists) =>
            artists.map { artist =>
              artistDiv(artist, key = artist.id)
            }.toVdomArray
        }
      )
    }
  }
}
