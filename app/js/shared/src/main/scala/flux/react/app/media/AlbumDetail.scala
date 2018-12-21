package flux.react.app.media

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.common.HydroReactComponent
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.AlbumDetailStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class AlbumDetail(implicit pageHeader: uielements.PageHeader,
                                     songDiv: uielements.media.SongDiv,
                                     albumDetailStoreFactory: AlbumDetailStoreFactory)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(albumId: Long, router: RouterContext): VdomElement = {
    component(Props(albumId, router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected def createBackend = new Backend(_)
  override protected def initialState = State()
  override protected val stateStoresDependencies =
    _.addDependencyFromProps(props => albumDetailStoreFactory.get(props.albumId))(store =>
      _.copy(maybeStoreState = store.state))

  // **************** Private inner types ****************//
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
              songDiv(song, key = song.id)
            }.toVdomArray
        }
      )
    }
  }
}
