package common.websocket

import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import common.ScalaUtils.toPromise
import org.scalajs.dom._

import scala.async.Async.{async, await}
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[websocket] final class SerialWebsocketClient(websocketPath: String) {
  require(!websocketPath.startsWith("/"))

  var openWebsocketPromise: Option[Promise[BinaryWebsocketClient]] = None
  val responseMessagePromises: mutable.Buffer[Promise[ByteBuffer]] = mutable.Buffer()

  def sendAndReceive(request: ByteBuffer): Future[ByteBuffer] = async {
    val lastMessagePromise: Option[Promise[_]] = responseMessagePromises.lastOption
    val thisMessagePromise: Promise[ByteBuffer] = Promise()
    responseMessagePromises += thisMessagePromise

    // Wait until websocket is initialized and previous request is done
    val websocket = await(getOrOpenWebsocket().future)
    if (lastMessagePromise.isDefined) {
      await(lastMessagePromise.get.future)
    }

    logExceptions {
      websocket.send(request)
    }

    await(thisMessagePromise.future)
  }

  def backlogSize: Int = responseMessagePromises.size

  private def getOrOpenWebsocket(): Promise[BinaryWebsocketClient] = {
    if (openWebsocketPromise.isEmpty) {
      openWebsocketPromise = Some(
        toPromise(
          BinaryWebsocketClient
            .open(
              name = name,
              websocketPath = websocketPath,
              onMessageReceived = onMessageReceived,
              onError = () =>
                responseMessagePromises.headOption.map(
                  _.tryFailure(new RuntimeException("Error from WebSocket"))),
              onClose = () => {
                openWebsocketPromise = None
                responseMessagePromises.headOption.map(
                  _.tryFailure(new RuntimeException("WebSocket was closed")))
              }
            )))
    }
    openWebsocketPromise.get
  }

  private def onMessageReceived(bytes: ByteBuffer): Unit = {
    responseMessagePromises.headOption match {
      case Some(promise) if promise.isCompleted =>
        throw new AssertionError("First promise in responseMessagePromises is completed. This is a bug!")
      case Some(promise) =>
        responseMessagePromises.remove(0)
        promise.success(bytes)
      case None =>
        console.log(s"  [$name] Warning: Received message without request")
    }
  }

  private val websocketNumber: Int = {
    val result = SerialWebsocketClient.nextWebsocketNumber
    SerialWebsocketClient.nextWebsocketNumber += 1
    result
  }
  private def name: String = s"SerialWebSocket-$websocketNumber"
}
object SerialWebsocketClient {
  private var nextWebsocketNumber: Int = 1
}
