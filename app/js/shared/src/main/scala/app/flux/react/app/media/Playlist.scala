package app.flux.react.app.media

import hydro.common.CollectionUtils.ifThenSeq
import app.flux.action.AppActions
import app.flux.stores.media.PlayStatusStore
import app.flux.stores.media.PlaylistStore
import app.models.media.JsPlaylistEntry
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.<
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

import scala.collection.immutable.Seq

private[app] final class Playlist(implicit pageHeader: PageHeader,
                                  dispatcher: Dispatcher,
                                  playlistStore: PlaylistStore,
                                  playStatusStore: PlayStatusStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(playlistStore, _.copy(maybeEntries = playlistStore.state.map(_.entries)))
    .withStateStoresDependency(
      playStatusStore,
      _.copy(playStatusStoreState = playStatusStore.state getOrElse PlayStatusStore.State.nullInstance))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(maybeEntries: Option[Seq[JsPlaylistEntry]] = None,
                             playStatusStoreState: PlayStatusStore.State = PlayStatusStore.State.nullInstance)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        state.maybeEntries match {
          case None =>
            <.div("Loading...")
          case Some(entries) =>
            entries.map {
              entry =>
                val isCurrentSong = state.playStatusStoreState.currentPlaylistEntry == Some(entry)
                <.div(
                  ^.key := entry.id,
                  ^^.classes("playlist-entry" +: ifThenSeq(isCurrentSong, "active")),
                  s"- ${entry.song.trackNumber} ${entry.song.title} (artist: ${entry.song.artist.map(_.name) getOrElse "-"})",
                  " ",
                  Bootstrap.Button(size = Size.xs)(
                    ^.onClick --> LogExceptionsCallback[Unit](
                      playStatusStore.play(playlistEntryId = entry.id)),
                    Bootstrap.FontAwesomeIcon("play-circle-o")
                  ),
                  Bootstrap.Button(size = Size.xs)(
                    ^.onClick --> LogExceptionsCallback[Unit](
                      dispatcher.dispatch(AppActions.RemoveEntriesFromPlaylist(Seq(entry.id)))),
                    Bootstrap.FontAwesomeIcon("times-circle-o")
                  )
                )
            }.toVdomArray
        }
      )
    }
  }
}
