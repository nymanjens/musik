package flux.react.uielements.media

import common.LoggingUtils.LogExceptionsCallback
import flux.action.Actions
import flux.action.Actions.AddSongsToPlaylist.Placement
import flux.react.ReactVdomUtils.^^
import flux.router.RouterContext
import hydro.flux.action.Dispatcher
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsSong

import scala.collection.immutable.Seq

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
