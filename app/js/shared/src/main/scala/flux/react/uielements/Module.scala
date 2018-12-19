package flux.react.uielements

import common.I18n
import flux.action.Dispatcher
import flux.react.uielements.media.SongDiv
import models.access.EntityAccess

final class Module(implicit i18n: I18n, entityAccess: EntityAccess, dispatcher: Dispatcher) {

  implicit lazy val pageHeader = new PageHeader
  implicit lazy val songDiv = new SongDiv
}
