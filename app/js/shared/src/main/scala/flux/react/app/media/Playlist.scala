package flux.react.app.media

import common.CollectionUtils
import common.CollectionUtils.ifThenSeq
import common.LoggingUtils.LogExceptionsCallback
import common.LoggingUtils.logExceptions
import flux.action.Actions
import hydro.flux.action.StandardActions
import flux.react.ReactVdomUtils.^^
import flux.router.RouterContext
import flux.react.uielements
import hydro.flux.stores.StateStore
import flux.stores.media.PlayStatusStore
import flux.stores.media.PlaylistStore
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.<
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsPlaylistEntry

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
                  <.a(
                    ^^.classes("btn", "btn-default", "btn-xs"),
                    ^.onClick --> LogExceptionsCallback[Unit](
                      playStatusStore.play(playlistEntryId = entry.id)),
                    <.i(^.className := "fa fa-play-circle-o")
                  ),
                  <.a(
                    ^^.classes("btn", "btn-default", "btn-xs"),
                    ^.onClick --> LogExceptionsCallback[Unit](
                      dispatcher.dispatch(Actions.RemoveEntriesFromPlaylist(Seq(entry.id)))),
                    <.i(^.className := "fa fa-times-circle-o")
                  )
                )
            }.toVdomArray
        }
      )
    }
  }
}
