package flux.react.app.media

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.AllArtistsStore
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsArtist

import scala.collection.immutable.Seq

private[app] final class AllArtists(implicit pageHeader: uielements.PageHeader,
                                    allArtistsStore: AllArtistsStore)
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
              uielements.media.ArtistDiv(artist, key = artist.id)
            }.toVdomArray
        }
      )
    }
  }
}
