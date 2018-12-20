package flux.react.uielements.media

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.LoggingUtils.logExceptions
import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.MountedImpure
import japgolly.scalajs.react.internal.Box
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.raw.HTMLAudioElement
import org.scalajs.dom.{console, html}

private[media] object RawMusicPlayer {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderBackend[Backend]
    .componentDidMount(scope =>
      LogExceptionsCallback {
        if (scope.props.playing) {
          scope.backend.htmlAudioElement match {
            case Some(e) => e.play()
            case None    =>
          }
        }
    })
    .componentDidUpdate(scope =>
      LogExceptionsCallback {
        if (scope.prevProps != scope.currentProps) {
          scope.backend.htmlAudioElement match {
            case Some(e) if scope.currentProps.playing  => e.play()
            case Some(e) if !scope.currentProps.playing => e.pause()
            case None                                   =>
          }
        }
    })
    .build

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

  // **************** Private inner types ****************//
  private case class Props private[RawMusicPlayer] (src: String,
                                                    playing: Boolean,
                                                    onEnded: () => Unit,
                                                    onPlayingChanged: Boolean => Unit,
  )
  private type State = Unit

  private type ThisCtorSummoner = CtorType.Summoner.Aux[Box[Props], Children.None, CtorType.Props]
  private type ThisMutableRef = ToScalaComponent[Props, State, Backend, ThisCtorSummoner#CT]
  private type ThisComponentU = MountedImpure[Props, State, Backend]

  private class Backend($ : BackendScope[Props, State]) {
    val audioRef = Ref[html.Audio]

    def render(props: Props, state: State) = logExceptions {
      <.audio(
        ^.controls := true,
        ^.src := props.src,
        ^.preload := "auto",
        ^.className := "music-player",
        ^.onPlay --> LogExceptionsCallback(props.onPlayingChanged(true)),
        ^.onPause --> LogExceptionsCallback(props.onPlayingChanged(false)),
        ^.onEnded --> LogExceptionsCallback(props.onEnded()),
      ).withRef(audioRef)
    }

    def htmlAudioElement: Option[HTMLAudioElement] = audioRef.get.asCallback.runNow()
  }
}
