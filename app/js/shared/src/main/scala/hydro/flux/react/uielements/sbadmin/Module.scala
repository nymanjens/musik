package hydro.flux.react.uielements.sbadmin

import common.I18n
import common.time.Clock
import flux.action.Dispatcher
import flux.react.uielements
import flux.stores._
import models.access.JsEntityAccess
import models.user.User

final class Module(implicit i18n: I18n,
                   user: User,
                   entityAccess: JsEntityAccess,
                   globalMessagesStore: GlobalMessagesStore,
                   pageLoadingStateStore: PageLoadingStateStore,
                   pendingModificationsStore: PendingModificationsStore,
                   applicationIsOnlineStore: ApplicationIsOnlineStore,
                   userStore: UserStore,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  lazy val globalMessages: GlobalMessages = new GlobalMessages
  lazy val pageLoadingSpinner: PageLoadingSpinner = new PageLoadingSpinner
  lazy val applicationDisconnectedIcon: ApplicationDisconnectedIcon = new ApplicationDisconnectedIcon
  lazy val pendingModificationsCounter: PendingModificationsCounter = new PendingModificationsCounter
}
