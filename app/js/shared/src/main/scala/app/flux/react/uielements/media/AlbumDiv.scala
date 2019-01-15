package app.flux.react.uielements.media

import app.flux.router.AppPages
import app.models.media.JsAlbum
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object AlbumDiv {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.div(
        Bootstrap.Button(size = Size.xl, tag = router.anchorWithHrefTo(AppPages.Album(props.album.id)))(
          props.album.title,
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
