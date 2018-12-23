package flux.react.uielements.media

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.media.JsArtist

object ArtistDiv {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        router.anchorWithHrefTo(Page.Artist(props.artist.id))(
          ^^.classes("btn", "btn-default", "btn-xl"),
          ^.role := "button",
          " ",
          props.artist.name
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(artist: JsArtist, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(artist))
  }

  // **************** Private inner types ****************//
  private case class Props(artist: JsArtist)(implicit val router: RouterContext)
}
