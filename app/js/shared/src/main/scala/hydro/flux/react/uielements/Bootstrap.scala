package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable

object Bootstrap {

  def Row: VdomTag = <.div(^.className := "row")

  def Col(sm: Int = -1, md: Int = -1, lg: Int = -1, smOffset: Int = -1, mdOffset: Int = -1): VdomTag = {
    val classes = mutable.Buffer[String]()
    if (sm != -1) classes += s"col-sm-$sm"
    else if (md != -1) classes += s"col-md-$md"
    else if (lg != -1) classes += s"col-lg-$lg"
    else if (smOffset != -1) classes += s"col-sm-offset-$smOffset"
    else if (mdOffset != -1) classes += s"col-md-offset-$mdOffset"
    <.div(^^.classes(classes))
  }

  def Button(variant: Variant = Variant.default,
             size: Size = null,
             block: Boolean = false,
             tag: VdomTag = <.button): VdomTag = {
    val classes = mutable.Buffer[String]()
    classes += "btn"
    classes += s"btn-${variant.name}"
    if (size != null) {
      classes += s"btn-${size.name}"
    }
    if (block) {
      classes += s"btn-block"
    }
    tag(^^.classes(classes))
  }

  def Icon(className: String): VdomTag = <.i(^.className := className)
  def FontAwesomeIcon(name: String): VdomTag = Icon(s"fa fa-$name fa-fw")

  def Panel(variant: Variant = Variant.default): VdomTag =
    <.div(^.className := s"panel panel-${variant.name}")
  def PanelHeading: VdomTag = <.div(^.className := "panel-heading")
  def PanelBody: VdomTag = <.div(^.className := "panel-body")

  def NavbarBrand(tag: VdomTag = <.span): VdomTag = tag(^.className := "navbar-brand")

  case class Variant private (name: String)
  object Variant {
    val default = Variant("default")
    val primary = Variant("primary")
    val secondary = Variant("secondary")
    val success = Variant("success")
    val danger = Variant("danger")
    val warning = Variant("warning")
    val info = Variant("info")
    val link = Variant("link")
  }

  case class Size private (name: String)
  object Size {
    val xs = Size("xs")
    val sm = Size("sm")
    val lg = Size("lg")
    val xl = Size("xl")
    val huge = Size("huge")
  }
}
