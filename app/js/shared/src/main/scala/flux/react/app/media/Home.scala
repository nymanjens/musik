package flux.react.app.media

import scala.collection.immutable.Seq
import common.I18n
import common.LoggingUtils.logExceptions
import flux.action.Dispatcher
import flux.react.router.RouterContext
import flux.react.uielements
import flux.react.uielements.WaitForFuture
import flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.media.{Album, Artist, Song}

private[app] final class Home(implicit entityAccess: EntityAccess, i18n: I18n, dispatcher: Dispatcher) {

  private val waitForFutureArtists = new WaitForFuture[Seq[Artist]]
  private val waitForFutureAlbums = new WaitForFuture[Seq[Album]]
  private val waitForFutureSongs = new WaitForFuture[Seq[Song]]

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    waitForFutureArtists(entityAccess.newQuery[Artist]().data()) { allArtists =>
      waitForFutureAlbums(entityAccess.newQuery[Album]().data()) { allAlbums =>
        waitForFutureSongs(entityAccess.newQuery[Song]().data()) { allSongs =>
          component(
            Props(allArtists = allArtists, allAlbums = allAlbums, allSongs = allSongs, router = router))
        }
      }
    }
  }

  // **************** Private inner types ****************//
  private case class Props(allArtists: Seq[Artist],
                           allAlbums: Seq[Album],
                           allSongs: Seq[Song],
                           router: RouterContext)
  private type State = Unit

  private class Backend($ : BackendScope[Props, State]) {

    def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        uielements.PageHeader(router.currentPage),
        <.div("Artist:"), {
          for (artist <- props.allArtists)
            yield <.div(^.key := s"artist-${artist.id}", "- ", artist.toString)
        }.toVdomArray,
        <.div("Albums:"), {
          for (album <- props.allAlbums)
            yield <.div(^.key := s"album-${album.id}", "- ", album.toString)
        }.toVdomArray,
        <.div("Songs:"), {
          for (song <- props.allSongs)
            yield <.div(^.key := s"song-${song.id}", "- ", song.toString)
        }.toVdomArray
      )
    }
  }
}
