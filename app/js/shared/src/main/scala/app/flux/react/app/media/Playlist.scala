package app.flux.react.app.media

import java.lang.Math.max

import app.flux.action.AppActions
import app.flux.react.uielements.media.PlaylistEntryDiv
import app.flux.react.uielements.media.PlaylistEntryDiv.SongPlacement
import app.flux.stores.media.PlaylistStore
import app.flux.stores.media.PlayStatusStore
import app.models.media.JsPlaylistEntry
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.OrderToken
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import hydro.jsfacades.ReactBeautifulDnd
import hydro.jsfacades.ReactBeautifulDnd.OnDragEndHandler
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js

private[app] final class Playlist(implicit pageHeader: PageHeader,
                                  dispatcher: Dispatcher,
                                  playlistStore: PlaylistStore,
                                  playStatusStore: PlayStatusStore,
                                  playlistEntryDiv: PlaylistEntryDiv,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(playlistStore, _.copy(maybeEntries = playlistStore.state.map(_.entries)))
    .withStateStoresDependency(
      playStatusStore,
      _.copy(playStatusStoreState = playStatusStore.state getOrElse PlayStatusStore.State.nullInstance))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(maybeEntries: Option[Seq[JsPlaylistEntry]] = None,
                             playStatusStoreState: PlayStatusStore.State = PlayStatusStore.State.nullInstance)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader.withExtension(router.currentPage)(clearPlayedButton()),
        state.maybeEntries.map(
          entries =>
            truncatePlayedSongs(
              entries = entries,
              maybeCurrentEntry = state.playStatusStoreState.currentPlaylistEntry)) match {
          case None =>
            <.div("Loading...")
          case Some(entries) =>
            ReactBeautifulDnd.DragDropContext(onDragEndHandler = onDragEndHandler(entries))(
              ReactBeautifulDnd.Droppable(droppableId = "droppable") {
                (provided, snapshot) =>
                  <.div(
                    ^.className := "playlist",
                    rawTagMod("ref", provided.innerRef),
                    entries
                      .zip(calculateColorClasses(entries))
                      .zipWithIndex
                      .map {
                        case ((entry, colorClass), index) =>
                          val songPlacement =
                            state.playStatusStoreState.currentPlaylistEntry.map(_.id) match {
                              case Some(entryId) if entryId == entry.id => SongPlacement.Current
                              case Some(entryId) =>
                                if (index < entries.map(_.id).indexOf(entryId)) SongPlacement.BeforeCurrent
                                else SongPlacement.AfterCurrent
                              case None => SongPlacement.Unknown
                            }

                          ReactBeautifulDnd
                            .Draggable(key = entry.id, draggableId = entry.id.toString, index = index) {
                              (provided, snapshot) =>
                                <.div(toTagMods(provided.draggableProps) ++ toTagMods(
                                  provided.dragHandleProps): _*)(
                                  ^.className := "draggable",
                                  ^.className := colorClass,
                                  ^^.ifThen(snapshot.isDragging)(^.className := "dragging"),
                                  ^.key := entry.id,
                                  rawTagMod("ref", provided.innerRef),
                                  playlistEntryDiv(
                                    entry,
                                    songPlacement = songPlacement,
                                    isNowPlaying = songPlacement == SongPlacement.Current && state.playStatusStoreState.hasStarted),
                                )
                            }
                      }
                      .toVdomArray
                  )
              }
            )
        }
      )
    }

    private def clearPlayedButton(): VdomNode = {
      Bootstrap.Button(variant = Variant.default)(
        ^.className := "clear-played-button",
        ^.onClick --> Callback(dispatcher.dispatch(AppActions.RemoveAllPlayedEntriesFromPlaylist)),
        Bootstrap.Glyphicon("trash"),
        " Clear played",
      )
    }

    private def onDragEndHandler(entries: Seq[JsPlaylistEntry]): OnDragEndHandler = {
      (sourceIndex, maybeDestinationIndex) =>
        if (maybeDestinationIndex.isDefined) {
          val destinationIndex = maybeDestinationIndex.get
          val remainingEntries = entries.filterNot(_ == entries(sourceIndex))
          def maybeOrderToken(index: Int): Option[OrderToken] =
            if (remainingEntries.indices contains index) Some(remainingEntries(index).orderToken) else None
          val newOrderToken =
            OrderToken.middleBetween(maybeOrderToken(destinationIndex - 1), maybeOrderToken(destinationIndex))

          $.modState { oldState =>
            val oldStoreState = PlaylistStore.State(entries)
            val newStoreState =
              playlistStore.updateOrderTokenAndReturnState(oldStoreState, entries(sourceIndex), newOrderToken)
            oldState.copy(maybeEntries = Some(newStoreState.entries))
          }.runNow()
        }
    }

    private def calculateColorClasses(entries: Seq[JsPlaylistEntry]): Seq[String] = {
      def inner(entries: Seq[JsPlaylistEntry]): Seq[String] = {
        val colors = Seq("color-a", "color-b")
        var colorsIndex: Int = 0
        var lastAlbumId: Long = entries.headOption.map(_.song.album.id) getOrElse -1L
        for (entry <- entries) yield {
          if (entry.song.album.id != lastAlbumId) {
            lastAlbumId = entry.song.album.id
            colorsIndex = (colorsIndex + 1) % colors.size
          }
          colors(colorsIndex)
        }
      }

      // Choose colors from end to start (reverse list) so that the colors don't change when the already
      // played entries get truncated
      inner(entries.reverse).reverse
    }

    private def truncatePlayedSongs(entries: Seq[JsPlaylistEntry],
                                    maybeCurrentEntry: Option[JsPlaylistEntry]): Seq[JsPlaylistEntry] = {
      val maxAmountOfPlayedSongs = 3

      maybeCurrentEntry match {
        case None => entries
        case Some(currentEntry) =>
          entries.indexWhere(_.id == currentEntry.id) match {
            case -1           => entries
            case currentIndex => entries.drop(max(0, currentIndex - maxAmountOfPlayedSongs))
          }
      }
    }

    private def toTagMods(props: js.Dictionary[js.Object]): Seq[TagMod] = {
      val scalaProps: mutable.Map[String, js.Object] = props
      for ((name, value) <- scalaProps.toVector) yield rawTagMod(name, value)
    }

    private def rawTagMod(name: String, value: js.Object): TagMod = TagMod.fn(_.addAttr(name, value))
  }
}
