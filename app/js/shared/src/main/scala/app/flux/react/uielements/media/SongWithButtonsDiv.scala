package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.router.AppPages
import app.models.media.JsAlbum
import app.models.media.JsSong
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[media] object SongWithButtonsDiv {

  // **************** API ****************//
  def apply(router: RouterContext,
            song: JsSong,
            buttons: VdomTag,
            songTitleSpan: VdomTag = <.span,
            showArtist: Boolean = true,
            showAlbum: Boolean = true): VdomTag = {
    <.div(
      ^.className := "song-with-buttons",
      <.div(
        ^.className := "main-info",
        Bootstrap.Glyphicon("music"),
        " ",
        songTitleSpan(song.title),
      ),
      buttons(
        ^.className := "buttons",
      ),
      <.div(
        ^.className := "extra-info",
        <<.ifThen(showArtist) {
          <<.ifThen(song.artist) { artist =>
            <.span(
              ^.className := "artist",
              Bootstrap.FontAwesomeIcon("user"),
              " ",
              router.anchorWithHrefTo(AppPages.Artist(artist.id))(artist.name),
            )
          }
        },
        <<.ifThen(showAlbum) {
          <.span(
            ^.className := "album",
            Bootstrap.Glyphicon("cd"),
            " ",
            router.anchorWithHrefTo(AppPages.Album(song.album.id))(song.album.title),
          )
        }
      ),
    )
  }
}
