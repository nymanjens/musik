package common.testing

import scala.collection.immutable.Seq
import common.OrderToken
import models.document.Document.{DetachedCursor, DetachedSelection, IndexedCursor, IndexedSelection}
import models.document.{Task, TextWithMarkup}
import common.testing.TestObjects._
import common.time.LocalDateTime
import models.document.{Document, Task}

object JsTestObjects {

  val taskA = newTask("Task A", orderToken = orderTokenA, indentation = 2)
  val taskB = newTask("Task B", orderToken = orderTokenB, indentation = 3)
  val taskC = newTask("Task C", orderToken = orderTokenC)
  val taskD = newTask("Task D", orderToken = orderTokenD)
  val taskE = newTask("Task E", orderToken = orderTokenE)
  val testTask = taskA

  val testSelection = IndexedSelection.singleton(IndexedCursor(2, 123))
  val testDetachedCursor = DetachedCursor(taskA, 12938)
  val testDetachedSelection = DetachedSelection(testDetachedCursor, testDetachedCursor)

  def newTask(contentString: String = null,
              content: TextWithMarkup = null,
              orderToken: OrderToken = orderTokenB,
              indentation: Int = 1,
              collapsed: Boolean = false,
              delayedUntil: Option[LocalDateTime] = Some(testDate),
              tags: Seq[String] = Seq("test-tag")): Task = {
    require(content != null || contentString != null)
    Task.withRandomId(
      content = Option(content) getOrElse TextWithMarkup(contentString),
      orderToken = orderToken,
      indentation = indentation,
      collapsed = collapsed,
      delayedUntil = delayedUntil,
      tags = tags
    )
  }

  def newDocument(tasks: Task*): Document =
    new Document(id = 12873, name = "test document", orderToken = orderTokenA, tasks = Seq(tasks: _*))
}
