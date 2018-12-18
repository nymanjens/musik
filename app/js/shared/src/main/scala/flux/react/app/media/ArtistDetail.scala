package flux.react.app.media

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.ArtistDetailStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[app] final class ArtistDetail(implicit i18n: I18n,
                                      artistDetailStoreFactory: ArtistDetailStoreFactory) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(maybeStoreState = None))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.props, scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount(scope.props))
    .build

  // **************** API ****************//
  def apply(artistId: Long, router: RouterContext): VdomElement = {
    component(Props(artistId, router))
  }

  // **************** Private inner types ****************//
  private case class Props(artistId: Long, router: RouterContext) {
    lazy val store: artistDetailStoreFactory.Store = artistDetailStoreFactory.get(artistId)
  }
  private case class State(maybeStoreState: Option[ArtistDetailStoreFactory.State])

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(props: Props, state: State): Callback = LogExceptionsCallback {
      props.store.register(this)
      $.modState(state => logExceptions(state.copy(maybeStoreState = props.store.state))).runNow()
    }

    def willUnmount(props: Props): Callback = LogExceptionsCallback {
      props.store.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.copy(maybeStoreState = $.props.runNow().store.state))).runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        uielements.PageHeader(router.currentPage),
        state.maybeStoreState match {
          case None =>
            <.div("Loading...")
          case Some(storeState) =>
            storeState.albums.map { album =>
              <.div(album.toString)
            }.toVdomArray
            storeState.songsWithoutAlbum.map { song =>
              <.div(song.toString)
            }.toVdomArray
        }
      )
    }
  }
}
