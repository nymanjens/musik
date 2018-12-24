package flux.react.uielements.input.bootstrap

import common.I18n
import flux.react.ReactVdomUtils.^^
import flux.react.uielements.input.bootstrap.InputComponent.Props
import flux.react.uielements.input.bootstrap.InputComponent.ValueTransformer
import flux.react.uielements.input.InputBase
import flux.react.uielements.input.InputValidator
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

object TextInput {

  private val component = InputComponent.create[Value, ExtraProps](
    name = getClass.getSimpleName,
    inputRenderer = (classes: Seq[String],
                     name: String,
                     valueString: String,
                     onChange: String => Callback,
                     extraProps: ExtraProps) => {
      <.input(
        ^.tpe := extraProps.inputType,
        ^^.classes(classes),
        ^.name := name,
        ^.value := valueString,
        ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value)),
        ^.autoFocus := extraProps.focusOnMount,
        ^.disabled := extraProps.disabled
      )
    }
  )

  // **************** API ****************//
  def apply(ref: Reference,
            name: String,
            label: String,
            inputType: String = "text",
            defaultValue: String = "",
            required: Boolean = false,
            showErrorMessage: Boolean = false,
            additionalValidator: InputValidator[String] = InputValidator.alwaysValid,
            inputClasses: Seq[String] = Seq(),
            focusOnMount: Boolean = false,
            disabled: Boolean = false,
            listener: InputBase.Listener[String] = InputBase.Listener.nullInstance)(
      implicit i18n: I18n): VdomElement = {
    val props = Props(
      label = label,
      name = name,
      defaultValue = defaultValue,
      required = required,
      showErrorMessage = showErrorMessage,
      additionalValidator = additionalValidator,
      inputClasses = inputClasses,
      listener = listener,
      valueTransformer = ValueTransformer.nullInstance,
      extra = ExtraProps(inputType = inputType, focusOnMount = focusOnMount, disabled = disabled)
    )
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[TextInput] (
      private[TextInput] val mutableRef: InputComponent.ThisMutableRef[Value, ExtraProps])
      extends InputComponent.Reference(mutableRef)

  case class ExtraProps private[TextInput] (inputType: String, focusOnMount: Boolean, disabled: Boolean)

  // **************** Private inner types ****************//
  private type Value = String
}
