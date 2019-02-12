package app.api

import app.api.Picklers._
import app.api.ScalaJsApi._
import app.common.testing.TestObjects._
import app.common.testing._
import hydro.common.testing._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import boopickle.Default._
import boopickle.Pickler
import app.api.Picklers._
import org.junit.runner._
import org.specs2.runner._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class PicklersTest extends HookedSpecification {

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testSong))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testAlbum))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testArtist))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testPlaylistEntry))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testPlayStatus))
  }

  "GetInitialDataResponse" in {
    testPickleAndUnpickle[GetInitialDataResponse](testGetInitialDataResponse)
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
