package flux.router

import common.I18n
import flux.action.Dispatcher
import japgolly.scalajs.react.extra.router._
import models.access.EntityAccess

final class Module(implicit reactAppModule: flux.react.app.Module,
                   dispatcher: Dispatcher,
                   i18n: I18n,
                   entityAccess: EntityAccess) {

  implicit lazy val router: Router[Page] = (new RouterFactory).createRouter()
}
