package flux.react.app.media

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.Action.AddSongsToPlaylist.Placement
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.media.{PlayStatusStore, PlaylistStore}
import flux.stores.{StateStore, UserStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.{<, _}
import models.media.{JsPlaylistEntry, PlaylistEntry}
import models.user.User

import scala.collection.immutable.Seq
import scala.scalajs.js

private[app] final class Playlist(implicit pageHeader: uielements.PageHeader,
                                  playlistStore: PlaylistStore,
                                  playStatusStore: PlayStatusStore) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[State](State(maybeEntries = None))
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
  private case class State(maybeEntries: Option[Seq[JsPlaylistEntry]])

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

    def willMount(state: State): Callback = LogExceptionsCallback {
      playlistStore.register(this)
      $.modState(state => logExceptions(state.copy(maybeEntries = playlistStore.state.map(_.entries))))
        .runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      playlistStore.deregister(this)
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.copy(maybeEntries = playlistStore.state.map(_.entries))))
        .runNow()
    }

    def render(props: Props, state: State): VdomElement = logExceptions {
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
