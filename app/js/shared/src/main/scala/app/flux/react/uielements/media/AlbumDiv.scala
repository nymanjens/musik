package app.flux.react.uielements.media

import app.flux.router.AppPages
import app.models.media.JsAlbum
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap

object AlbumDiv {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        router.anchorWithHrefTo(AppPages.Album(props.album.id))(
          ^^.classes("btn", "btn-default", "btn-xl"),
          ^.role := "button",
          " ",
          props.album.title
        )
      )
    })
    .build

  // **************** API ****************//
  def apply(album: JsAlbum, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(album))
  }

  // **************** Private inner types ****************//
  private case class Props(album: JsAlbum)(implicit val router: RouterContext)
}
