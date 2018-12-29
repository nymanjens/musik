package app.flux.react.uielements

import app.common.I18n
import app.flux.react.uielements.media.MusicPlayerDiv
import app.flux.react.uielements.media.SongDiv
import app.flux.stores.media.PlayStatusStore
import hydro.flux.action.Dispatcher
import app.models.access.EntityAccess

final class Module(implicit i18n: I18n,
                   entityAccess: EntityAccess,
                   dispatcher: Dispatcher,
                   playStatusStore: PlayStatusStore) {

  implicit lazy val songDiv = new SongDiv
  implicit lazy val musicPlayerDiv = new MusicPlayerDiv
}
