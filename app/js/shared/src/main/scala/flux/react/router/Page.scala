package flux.react.router

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import common.I18n
import models.access.EntityAccess
import models.media.Artist

import scala.concurrent.Future

sealed trait Page {
  def title(implicit i18n: I18n, entityAccess: EntityAccess): Future[String]
  def iconClass: String
}
object Page {

  sealed abstract class PageBase(titleKey: String, override val iconClass: String) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful(titleSync)
    def titleSync(implicit i18n: I18n) = i18n(titleKey)
  }

  case object Root extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful("Root")
    override def iconClass = ""
  }

  // **************** User management views **************** //
  case object UserProfile extends PageBase("app.user-profile", iconClass = "fa fa-user fa-fw")
  case object UserAdministration extends PageBase("app.user-administration", iconClass = "fa fa-cogs fa-fw")

  // **************** Media views **************** //
  case object Home extends PageBase("app.home", iconClass = "fa fa-home fa-fw")
  case object Playlist extends PageBase("app.playlist", iconClass = "icon-list")
  case object Artists extends PageBase("app.artists", iconClass = "fa fa-group fa-fw")

  case class Artist(artistId: Long) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      entityAccess.newQuery[models.media.Artist]().findById(artistId).map(_.name)
    override def iconClass = "fa fa-user fa-fw"
  }
  case class Album(albumId: Long) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      entityAccess.newQuery[models.media.Album]().findById(albumId).map(_.title)
    override def iconClass = "glyphicon glyphicon-cd"
  }
}
