package app.flux.react.uielements.media

import scala.collection.immutable.Seq
import app.flux.router.AppPages
import app.flux.stores.media.ArtistDetailStoreFactory
import app.models.media.JsArtist
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class ArtistDiv(implicit artistDetailStoreFactory: ArtistDetailStoreFactory)
    extends HydroReactComponent {

  // **************** API ****************//
  def apply(artist: JsArtist, key: Any)(implicit router: RouterContext): VdomElement = {
    component.withKey(key.toString).apply(Props(artist))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = artistDetailStoreFactory.get(props.artist.id)
      StateStoresDependency(store, _.copy(maybeStoreState = store.state))
    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(artist: JsArtist)(implicit val router: RouterContext)
  protected case class State(maybeStoreState: Option[ArtistDetailStoreFactory.State] = None)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      <.div(
        ^.className := "artist-div",
        <.div(
          ^.className := "main-info",
          Bootstrap.FontAwesomeIcon("user"),
          " ",
          router.anchorWithHrefTo(AppPages.Artist(props.artist.id))(props.artist.name),
        ),
        <.div(
          ^.className := "extra-info",
          <<.ifThen(state.maybeStoreState)(albumAndSongsCount),
        ),
      )
    }

    private def albumAndSongsCount(storeState: ArtistDetailStoreFactory.State): VdomElement = {
      def countIfNonEmpty(iconClass: String, seq: Seq[_]): Option[VdomElement] = {
        if (seq.nonEmpty) {
          Some[VdomElement](
            <.span(
              ^.className := "count",
              Bootstrap.Icon(iconClass),
              " ",
              seq.size,
            )
          )
        } else {
          None
        }
      }

      <.span(
        <<.ifThen(countIfNonEmpty("glyphicon glyphicon-cd", storeState.albums))(identity),
        <<.ifThen(countIfNonEmpty("glyphicon glyphicon-music", storeState.songsWithoutAlbum))(identity),
      )
    }
  }
}
