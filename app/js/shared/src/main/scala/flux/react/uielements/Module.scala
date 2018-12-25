package flux.react.uielements

import common.I18n
import flux.react.uielements.media.MusicPlayerDiv
import flux.react.uielements.media.SongDiv
import flux.stores.media.PlayStatusStore
import hydro.flux.action.Dispatcher
import models.access.EntityAccess

final class Module(implicit i18n: I18n,
                   entityAccess: EntityAccess,
                   dispatcher: Dispatcher,
                   playStatusStore: PlayStatusStore) {

  implicit lazy val songDiv = new SongDiv
  implicit lazy val musicPlayerDiv = new MusicPlayerDiv
}
