package flux.react.uielements

import common.I18n
import models.access.EntityAccess

final class Module(implicit i18n: I18n, entityAccess: EntityAccess) {

  implicit lazy val pageHeader = new PageHeader
}
