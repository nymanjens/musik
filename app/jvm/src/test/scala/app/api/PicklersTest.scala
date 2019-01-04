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

  "EntityType" in {
    testPickleAndUnpickle[EntityType.any](User.Type)
  }

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testSong))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testAlbum))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testArtist))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testPlaylistEntry))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testPlayStatus))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Remove[User](123054))
  }

  "GetInitialDataResponse" in {
    testPickleAndUnpickle[GetInitialDataResponse](testGetInitialDataResponse)
  }

  "GetAllEntitiesResponse" in {
    testPickleAndUnpickle[GetAllEntitiesResponse](
      GetAllEntitiesResponse(
        entitiesMap = Map(User.Type -> Seq(testUserRedacted)),
        nextUpdateToken = testUpdateToken))
  }

  "ModificationsWithToken" in {
    testPickleAndUnpickle[ModificationsWithToken](
      ModificationsWithToken(modifications = Seq(testModification), nextUpdateToken = testUpdateToken))
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
