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

private[media] object GeneralMusicDivs {

  // **************** Public API ****************//
  def musicalObjectWithButtons(icon: VdomNode,
                               title: VdomTag,
                               titleSuffix: Option[VdomNode],
                               extraPiecesOfInfo: Seq[VdomTag],
                               buttons: Option[VdomTag] = None,
  ): VdomTag = {
    <.div(
      ^.className := "musical-object-with-buttons",
      <.div(
        ^.className := "main-info",
        icon,
        " ",
        title(
          ^.className := "title",
        ),
        <<.ifThen(titleSuffix)(identity),
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
    def nowPlayingIndicator = Bootstrap.Glyphicon("volume-up") {
      ^.className := "now-playing-indicator"
    }
    val extraPiecesOfInfo: Seq[VdomTag] = Seq() ++
      ifThenOption(showArtist && song.artist.isDefined) {
        <.span(
          Bootstrap.FontAwesomeIcon("user"),
          " ",
          router.anchorWithHrefTo(AppPages.Artist(song.artist.get.id))(song.artist.get.name),
        )
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
      titleSuffix = ifThenOption(isNowPlaying)(nowPlayingIndicator),
      extraPiecesOfInfo = extraPiecesOfInfo,
      buttons = Some(buttons),
    )(
      ^.className := "song-with-buttons",
      ^^.ifThen(isCurrentSong)(^.className := "current-song"),
      ^^.ifThen(isNowPlaying)(^.className := "now-playing"),
    )
  }

  // **************** private helper methods ****************//
  private def durationToShortString(duration: FiniteDuration): String = {
    val minutes = duration.toMinutes
    val secondsInMinute = (duration - minutes.minutes).toSeconds
    "%d:%02d".format(minutes, secondsInMinute)
  }

  private def ifThenOption[T](boolean: Boolean)(valueIfTrue: => T): Option[T] = {
    if (boolean) Some(valueIfTrue) else None
  }
}
