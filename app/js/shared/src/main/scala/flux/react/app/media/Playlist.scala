package flux.react.app.media

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.ReactVdomUtils.^^
import flux.react.common.HydroReactComponent
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.StateStore
import flux.stores.media.{PlayStatusStore, PlaylistStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.{<, _}
import models.media.JsPlaylistEntry

import scala.collection.immutable.Seq

private[app] final class Playlist(implicit pageHeader: uielements.PageHeader,
                                  playlistStore: PlaylistStore,
                                  playStatusStore: PlayStatusStore)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(playlistStore, _.copy(maybeEntries = playlistStore.state.map(_.entries)))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(maybeEntries: Option[Seq[JsPlaylistEntry]] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        state.maybeEntries match {
          case None =>
            <.div("Loading...")
          case Some(entries) =>
            entries.map { entry =>
              <.div(
                ^.key := entry.id,
                s"- ${entry.song.trackNumber} ${entry.song.title} (artist: ${entry.song.artist.map(_.name) getOrElse "-"})",
                " ",
                <.a(
                  ^^.classes("btn", "btn-default", "btn-xs"),
                  ^.onClick --> playCallback(entry),
                  <.i(^.className := "fa fa-play-circle-o")
                )
              )
            }.toVdomArray
        }
      )
    }

    private def playCallback(playlistEntry: JsPlaylistEntry): Callback = LogExceptionsCallback {
      playStatusStore.play(playlistEntryId = playlistEntry.id)
    }
  }
}
