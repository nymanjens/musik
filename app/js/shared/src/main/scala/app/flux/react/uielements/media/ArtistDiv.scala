package app.flux.react.uielements.media

import scala.collection.immutable.Seq
import app.flux.router.AppPages
import app.flux.stores.media.ArtistDetailStoreFactory
import app.models.media.JsArtist
import hydro.common.ScalaUtils.ifThenOption
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

    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      GeneralMusicDivs.musicalObjectWithButtons(
        icon = Bootstrap.FontAwesomeIcon("user"),
        title = router.anchorWithHrefTo(AppPages.Artist(props.artist.id))(props.artist.name),
        extraPiecesOfInfo = state.maybeStoreState.map(albumAndSongsCount) getOrElse Seq(),
      )(
        ^.className := "artist-div",
      )
    }

    private def albumAndSongsCount(storeState: ArtistDetailStoreFactory.State): Seq[VdomTag] = {
      def countIfNonEmpty(icon: VdomTag, seq: Seq[_]): Option[VdomTag] = {
        if (seq.nonEmpty) {
          Some[VdomTag](
            <.span(
              icon,
              " ",
              seq.size,
            )
          )
        } else {
          None
        }
      }

      Seq() ++
        countIfNonEmpty(Bootstrap.Glyphicon("cd"), storeState.albums) ++
        countIfNonEmpty(Bootstrap.Glyphicon("music"), storeState.songsWithoutAlbum)

    }
  }
}
