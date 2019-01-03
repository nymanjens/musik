package app.flux.react.uielements.media

import app.flux.router.AppPages
import app.models.media.JsArtist
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object ArtistDiv {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        router.anchorWithHrefTo(AppPages.Artist(props.artist.id))(
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
