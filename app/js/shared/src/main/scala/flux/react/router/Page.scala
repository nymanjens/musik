package flux.react.router

import common.I18n
import japgolly.scalajs.react.extra.router.Path

import scala.scalajs.js

sealed trait Page {
  def title(implicit i18n: I18n): String
  def iconClass: String
}
object Page {

  sealed abstract class PageBase(titleKey: String, override val iconClass: String) extends Page {
    override def title(implicit i18n: I18n) = i18n(titleKey)
  }

  case object Root extends Page {
    override def title(implicit i18n: I18n) = "Root"
    override def iconClass = ""
  }

  // **************** User management views **************** //
  case object UserProfile extends PageBase("app.user-profile", iconClass = "fa fa-user fa-fw")
  case object UserAdministration extends PageBase("app.user-administration", iconClass = "fa fa-cogs fa-fw")

  // **************** Media views **************** //
  case object Home extends PageBase("app.home", iconClass = "fa fa-home fa-fw")
  case object Playlist extends PageBase("app.playlist", iconClass = "icon-list")
  case object Artists extends PageBase("app.artists", iconClass = "fa fa-group fa-fw")
}
