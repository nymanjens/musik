package flux.react.uielements.media

import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsSong

object SongDiv {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        router.anchorWithHrefTo(Page.Home)(
          ^^.classes("btn", "btn-default", "btn-xl"),
          ^.role := "button",
          " ",
          props.song.title
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(song: JsSong, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(song))
  }

  // **************** Private inner types ****************//
  private case class Props(song: JsSong)(implicit val router: RouterContext)
}
