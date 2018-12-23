package flux.react.uielements.media

import scala.collection.immutable.Seq
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.Actions.AddSongsToPlaylist.Placement
import flux.action.Actions
import flux.react.ReactVdomUtils.^^
import flux.router.{Page, RouterContext}
import flux.stores.media.PlaylistStore
import hydro.flux.action.Dispatcher
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsSong

final class SongDiv(implicit dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        <.a(
          ^^.classes("btn", "btn-default", "btn-xl"),
          ^.role := "button",
          ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AtEnd),
          props.song.title
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(song: JsSong, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(song))
  }

  // **************** Private methods ****************//
  private def addToPlaylistCallback(song: JsSong, placement: Placement): Callback = LogExceptionsCallback {
    dispatcher.dispatch(Actions.AddSongsToPlaylist(songIds = Seq(song.id), placement = placement))
  }

  // **************** Private inner types ****************//
  private case class Props(song: JsSong)(implicit val router: RouterContext)
}
