package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable

object Bootstrap {

  def Row(children: TagMod*): VdomElement = divWithClassName("row", children)

  def Col(sm: Int = -1, md: Int = -1, lg: Int = -1, smOffset: Int = -1, mdOffset: Int = -1)(
      children: TagMod*): VdomElement = {
    val classes = mutable.Buffer[String]()
    if (sm != -1) classes += s"col-sm-$sm"
    else if (md != -1) classes += s"col-md-$md"
    else if (lg != -1) classes += s"col-lg-$lg"
    else if (smOffset != -1) classes += s"col-sm-offset-$smOffset"
    else if (mdOffset != -1) classes += s"col-md-offset-$mdOffset"
    divWithClasses(classes, children)
  }

  def Panel(children: TagMod*): VdomElement = divWithClassName("panel panel-default", children)
  def PanelHeading(children: TagMod*): VdomElement = divWithClassName("panel-heading", children)
  def PanelBody(children: TagMod*): VdomElement = divWithClassName("panel-body", children)

  // TODO: BUTTON

  private def divWithClassName(className: String, children: Seq[TagMod]): VdomElement = {
    val allTagMods = (^.className := className) +: children
    <.div(allTagMods: _*)
  }
  private def divWithClasses(classes: Iterable[String], children: Seq[TagMod]): VdomElement = {
    val allTagMods = ^^.classes(classes) +: children
    <.div(allTagMods: _*)
  }
}
