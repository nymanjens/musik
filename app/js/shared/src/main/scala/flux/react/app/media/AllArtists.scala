package flux.react.app.media

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.AllArtistsStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsArtist

import scala.collection.immutable.Seq

private[app] final class AllArtists(implicit pageHeader: uielements.PageHeader,
                                    allArtistsStore: AllArtistsStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(maybeArtists = None))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(maybeArtists: Option[Seq[JsArtist]])

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      allArtistsStore.register(this)
      $.modState(state => logExceptions(state.copy(maybeArtists = allArtistsStore.state.map(_.artists))))
        .runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      allArtistsStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.copy(maybeArtists = allArtistsStore.state.map(_.artists))))
        .runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
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
