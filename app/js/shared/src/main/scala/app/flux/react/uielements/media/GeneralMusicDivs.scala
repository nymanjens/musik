package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.router.AppPages
import app.models.media.JsAlbum
import app.models.media.JsSong
import hydro.common.ScalaUtils
import hydro.common.ScalaUtils.ifThenOption
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.concurrent.duration._

private[media] object GeneralMusicDivs {

  // **************** Public API ****************//
  def musicalObjectWithButtons(icon: VdomNode,
                               title: VdomTag,
                               titleSuffix: Option[VdomNode] = None,
                               extraPiecesOfInfo: Seq[VdomTag] = Seq(),
                               buttons: Option[VdomTag] = None,
                               isCurrentObject: Boolean = false,
                               isNowPlaying: Boolean = false,
  ): VdomTag = {
    <.div(
      ^.className := "musical-object-with-buttons",
      ^^.ifThen(isCurrentObject)(^.className := "current-object"),
      <.div(
        ^.className := "main-info",
        icon,
        " ",
        title(
          ^.className := "title",
        ),
        <<.ifThen(isNowPlaying) {
          Bootstrap.Glyphicon("volume-up") {
            ^.className := "now-playing-indicator"
          }
        },
      ),
      <<.ifThen(buttons) { buttonsTag =>
        buttonsTag(
          ^.className := "buttons",
        )
      },
      <.div(
        ^.className := "extra-info",
        extraPiecesOfInfo.zipWithIndex.map {
          case (extraPieceOfInfo, index) =>
            extraPieceOfInfo(
              ^.className := "piece-of-info",
              ^.key := index,
            )
        }.toVdomArray
      )
    )
  }

  def songWithButtons(router: RouterContext,
                      song: JsSong,
                      buttons: VdomTag,
                      songTitleSpan: VdomTag = <.span,
                      isCurrentSong: Boolean,
                      isNowPlaying: Boolean,
                      showArtist: Boolean = true,
                      showAlbum: Boolean = true): VdomTag = {
    val extraPiecesOfInfo: Seq[VdomTag] = Seq() ++
      song.artist.flatMap { artist =>
        ifThenOption(showArtist) {
          <.span(
            Bootstrap.FontAwesomeIcon("user"),
            " ",
            router.anchorWithHrefTo(AppPages.Artist(artist.id))(artist.name),
          )
        }
      } ++
      ifThenOption(showAlbum) {
        <.span(
          Bootstrap.Glyphicon("cd"),
          " ",
          router.anchorWithHrefTo(AppPages.Album(song.album.id))(song.album.title),
        )
      } :+
      <.span(
        Bootstrap.Glyphicon("time"),
        " ",
        durationToShortString(song.duration),
      )

    musicalObjectWithButtons(
      icon = Bootstrap.Glyphicon("music"),
      title = songTitleSpan(song.title),
      extraPiecesOfInfo = extraPiecesOfInfo,
      buttons = Some(buttons),
      isCurrentObject = isCurrentSong,
      isNowPlaying = isNowPlaying,
    )(
      ^.className := "song-with-buttons",
    )
  }

  // **************** private helper methods ****************//
  private def durationToShortString(duration: FiniteDuration): String = {
    val minutes = duration.toMinutes
    val secondsInMinute = (duration - minutes.minutes).toSeconds
    "%d:%02d".format(minutes, secondsInMinute)
  }
}
