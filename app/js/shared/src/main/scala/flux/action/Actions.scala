package flux.action

import scala.collection.immutable.Seq
import api.ScalaJsApi.UserPrototype
import flux.action.Actions.AddSongsToPlaylist.Placement
import hydro.flux.action.Action
import models.media.Song

import scala.async.Async.async
import scala.concurrent.Future

object Actions {

  // **************** User-related actions **************** //
  case class UpsertUser(userPrototype: UserPrototype) extends Action

  // **************** Media-related actions **************** //
  case class AddSongsToPlaylist(songIds: Seq[Long], placement: Placement) extends Action
  object AddSongsToPlaylist {
    sealed trait Placement
    object Placement {
      object AfterCurrentSong extends Placement
      object AtEnd extends Placement
    }
  }
  case class RemoveEntriesFromPlaylist(playlistEntryIds: Seq[Long]) extends Action

  // **************** Other actions **************** //
  case class SetPageLoadingState(isLoading: Boolean) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after they processed the contained action. */
  case class Done private[action] (action: Action) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after processing an action failed. */
  case class Failed private[action] (action: Action) extends Action
}
