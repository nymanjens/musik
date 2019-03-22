package app.flux.react.uielements.media

import app.flux.action.AppActions
import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.models.media.JsSong
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
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

import scala.collection.immutable.Seq

final class EnqueueableSongDiv(implicit dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router

      val buttons = <.div(
        Bootstrap.Glyphicon("plus-sign")(
          ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AfterCurrentSong)
        ),
        " ",
        Bootstrap.Glyphicon("circle-arrow-down")(
          ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AtEnd)
        )
      )

      val songTitleSpan = <.a(
        ^.href := "javascript:void(0)",
        ^.onClick --> addToPlaylistCallback(props.song, placement = Placement.AtEnd))

      SongWithButtonsDiv(
        router = router,
        song = props.song,
        buttons = buttons,
        songTitleSpan = songTitleSpan,
        showArtist = props.showArtist,
        showAlbum = props.showAlbum,
      )(
        ^.className := "enqueueable-song-div",
//        ^^.ifThen(props.isCurrentSong)(^.className := "active"),
      )
    })
    .build

  // **************** API ****************//
  def apply(song: JsSong, showArtist: Boolean, showAlbum: Boolean, key: Any)(
      implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(song, showArtist, showAlbum))
  }

  // **************** Private methods ****************//
  private def addToPlaylistCallback(song: JsSong, placement: Placement): Callback = LogExceptionsCallback {
    dispatcher.dispatch(AppActions.AddSongsToPlaylist(songIds = Seq(song.id), placement = placement))
  }

  // **************** Private inner types ****************//
  private case class Props(song: JsSong, showArtist: Boolean, showAlbum: Boolean)(
      implicit val router: RouterContext)
}
