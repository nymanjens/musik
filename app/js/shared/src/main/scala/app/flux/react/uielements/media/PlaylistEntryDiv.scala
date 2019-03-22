package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsPlaylistEntry
import hydro.flux.action.Dispatcher
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class PlaylistEntryDiv(implicit dispatcher: Dispatcher, playStatusStore: PlayStatusStore)
    extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(playlistEntry: JsPlaylistEntry, isCurrentSong: Boolean)(
      implicit router: RouterContext): VdomElement = {
    component.apply(Props(playlistEntry, isCurrentSong))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(playlistEntry: JsPlaylistEntry, isCurrentSong: Boolean)(
      implicit val router: RouterContext)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      val buttons = <.div(
        Bootstrap.FontAwesomeIcon("play-circle-o")(
          ^.onClick --> {
            playStatusStore.play(playlistEntryId = props.playlistEntry.id)
            Callback.empty
          },
        ),
        " ",
        Bootstrap.FontAwesomeIcon("times-circle-o")(
          ^.onClick --> {
            dispatcher.dispatch(AppActions.RemoveEntriesFromPlaylist(Seq(props.playlistEntry.id)))
            Callback.empty
          },
        )
      )

      SongWithButtonsDiv(router = router, song = props.playlistEntry.song, buttons = buttons)(
        ^.className := "playlist-entry-div",
        ^^.ifThen(props.isCurrentSong)(^.className := "active"),
      )
    }
  }
}
