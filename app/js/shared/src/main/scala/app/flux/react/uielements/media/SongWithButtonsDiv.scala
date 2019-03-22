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
import scala.concurrent.duration._

private[media] object SongWithButtonsDiv {

  // **************** API ****************//
  def apply(router: RouterContext,
            song: JsSong,
            buttons: VdomTag,
            songTitleSpan: VdomTag = <.span,
            isCurrentSong: Boolean,
            isNowPlaying: Boolean,
            showArtist: Boolean = true,
            showAlbum: Boolean = true): VdomTag = {
    <.div(
      ^.className := "song-with-buttons",
      ^^.ifThen(isCurrentSong)(^.className := "current-song"),
      ^^.ifThen(isNowPlaying)(^.className := "now-playing"),
      <.div(
        ^.className := "main-info",
        Bootstrap.Glyphicon("music"),
        " ",
        songTitleSpan(
          ^.className := "song-title",
          song.title,
        ),
        <<.ifThen(isNowPlaying) {
          Bootstrap.Glyphicon("volume-up") {
            ^.className := "now-playing-indicator"
          }
        },
      ),
      buttons(
        ^.className := "buttons",
      ),
      <.div(
        ^.className := "extra-info",
        <<.ifThen(showArtist) {
          <<.ifThen(song.artist) { artist =>
            <.span(
              ^.className := "piece-of-info",
              Bootstrap.FontAwesomeIcon("user"),
              " ",
              router.anchorWithHrefTo(AppPages.Artist(artist.id))(artist.name),
            )
          }
        },
        <<.ifThen(showAlbum) {
          <.span(
            ^.className := "piece-of-info",
            Bootstrap.Glyphicon("cd"),
            " ",
            router.anchorWithHrefTo(AppPages.Album(song.album.id))(song.album.title),
          )
        },
        <.span(
          ^.className := "piece-of-info",
          Bootstrap.Glyphicon("time"),
          " ",
          durationToShortString(song.duration),
        )
      ),
    )
  }

  // **************** private helper methods ****************//
  private def durationToShortString(duration: FiniteDuration): String = {
    val minutes = duration.toMinutes
    val secondsInMinute = (duration - minutes.minutes).toSeconds
    "%d:%02d".format(minutes, secondsInMinute)
  }
}
