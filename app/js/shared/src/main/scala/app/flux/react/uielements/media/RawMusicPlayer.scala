package app.flux.react.uielements.media

import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap
import org.scalajs.dom.console
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLAudioElement

private[media] object RawMusicPlayer extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(ref: Reference,
            src: String,
            playing: Boolean,
            onEnded: () => Unit,
            onPlayingChanged: Boolean => Unit,
  ): VdomElement = {
    val props = Props(src = src, playing = playing, onEnded = onEnded, onPlayingChanged = onPlayingChanged)
    ref.mutableRef.component(props)
  }

  def ref(): Reference = new Reference(Ref.toScalaComponent(component))

  // **************** Public inner types ****************//
  final class Reference private[RawMusicPlayer] (private[RawMusicPlayer] val mutableRef: ThisMutableRef) {
    def apply(): Proxy = new Proxy(
      () => mutableRef.get.asCallback.runNow().flatMap(_.backend.htmlAudioElement)
    )
  }

  final class Proxy(private val inputProvider: () => Option[HTMLAudioElement]) {
    def pause(): Unit = {
      inputProvider() match {
        case Some(input) =>
          input.pause()
        case None => console.log("Warning: Audio tag not found")
      }
    }
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props private[RawMusicPlayer] (src: String,
                                                      playing: Boolean,
                                                      onEnded: () => Unit,
                                                      onPlayingChanged: Boolean => Unit,
  )

  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisMutableRef = ToScalaComponent[Props, State, Backend, ThisCtorSummoner#CT]
  private type ThisComponentU = MountedImpure[Props, State, Backend]

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with DidUpdate {
    val audioRef = Ref[html.Audio]

    override def didMount(props: Props, state: Unit): Callback = LogExceptionsCallback {
      if (props.playing) {
        htmlAudioElement match {
          case Some(e) => e.play()
          case None    =>
        }
      }
    }

    override def didUpdate(prevProps: Props,
                           currentProps: Props,
                           prevState: Unit,
                           currentState: Unit): Callback = LogExceptionsCallback {
      if (prevProps != currentProps) {
        htmlAudioElement match {
          case Some(e) if currentProps.playing  => e.play()
          case Some(e) if !currentProps.playing => e.pause()
          case None                             =>
        }
      }
    }

    override def render(props: Props, state: State) = logExceptions {
      <.audio(
        ^.controls := true,
        ^.src := props.src,
        ^.preload := "auto",
        ^.onPlay --> LogExceptionsCallback(props.onPlayingChanged(true)),
        ^.onPause --> LogExceptionsCallback {
          val element = htmlAudioElement.get
          val pauseBecauseEnded = element.duration - element.currentTime < 0.01
          if (!pauseBecauseEnded) {
            props.onPlayingChanged(false)
          }
        },
        ^.onEnded --> LogExceptionsCallback(props.onEnded()),
      ).withRef(audioRef)
    }

    def htmlAudioElement: Option[HTMLAudioElement] = audioRef.get.asCallback.runNow()
  }
}
