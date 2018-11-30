package flux.react.app.media

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.react.app.document.{DesktopTaskList, DocumentAdministration, TaskEditor}
import flux.stores.document.{AllDocumentsStore, DocumentSelectionStore, DocumentStoreFactory}
import models.access.JsEntityAccess

final class Module(implicit i18n: I18n, dispatcher: Dispatcher, clock: Clock, entityAccess: JsEntityAccess) {

  implicit lazy val home = new Home
}
