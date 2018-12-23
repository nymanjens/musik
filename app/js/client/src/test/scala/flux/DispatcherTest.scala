package flux

import common.testing.TestObjects
import common.testing.TestObjects.testUserPrototype
import flux.action.Action
import flux.action.Actions
import utest._

import scala.async.Async.{async, await}
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

object DispatcherTest extends TestSuite {

  override def tests = TestSuite {
    val dispatcher: Dispatcher.Impl = new Dispatcher.Impl()
    val testAction = Actions.UpsertUser(testUserPrototype)

    "dispatches actions to listeners, including Done action" - async {
      val dispatchedActions: mutable.Buffer[Action] = mutable.Buffer()
      dispatcher.registerAsync(action => {
        dispatchedActions += action
        Future.successful((): Unit)
      })

      await(dispatcher.dispatch(testAction))

      dispatchedActions ==> mutable.Buffer(testAction, Actions.Done(testAction))
    }

    "does not allow dispatching during the sync part of a callback" - async {
      var dispatched = false
      dispatcher.registerAsync(action => {
        try {
          dispatcher.dispatch(testAction)
          throw new java.lang.AssertionError("expected IllegalArgumentException")
        } catch {
          case e: IllegalArgumentException => // expected
        }
        dispatched = true
        Future.successful((): Unit)
      })

      await(dispatcher.dispatch(testAction))

      dispatched ==> true
    }
  }
}
