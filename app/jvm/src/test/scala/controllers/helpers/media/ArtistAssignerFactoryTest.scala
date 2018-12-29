package controllers.helpers.media

import com.google.inject.Guice
import com.google.inject.Inject
import app.common.testing.JvmTestObjects._
import app.common.testing.TestObjects._
import app.common.testing._
import app.models.access.JvmEntityAccess
import app.models.media.Artist
import app.models.modification.EntityModification
import app.models.user.User
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ArtistAssignerFactoryTest extends HookedSpecification {

  implicit private val robotUser: User = testUser

  @Inject private val entityAccess: JvmEntityAccess = null

  @Inject private val artistAssignerFactory: ArtistAssignerFactory = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "fromDbAndMediaFiles().lookupName()" in {
    "with empty db" in new WithApplication {
      val assigner = artistAssignerFactory.fromDbAndMediaFiles(
        Seq(
          mediaFile(artist = "The Red Birds"),
          mediaFile(artist = "Socks & The Blue Birds"),
          mediaFile(artist = "Socks and The Red Birds"),
          mediaFile(artist = "Socks and The Red Birds"),
          mediaFile(artist = "Socks"),
          mediaFile(albumartist = "Socks"),
          mediaFile(albumartist = "Socks")
        ))

      assigner.canonicalArtistName("the red birds") mustEqual "The Red Birds"
      assigner.canonicalArtistName("Socks and The Red Birds") mustEqual "Socks"
      assigner.canonicalArtistName("Socks and The Hungry Cat") mustEqual "Socks"
    }

    "with non-empty db" in new WithApplication {
      entityAccess.persistEntityModifications(
        EntityModification.createAddWithRandomId(artist("Socks & Sweaters")))
      val assigner = artistAssignerFactory.fromDbAndMediaFiles(
        Seq(
          mediaFile(artist = "The Red Birds"),
          mediaFile(artist = "Socks")
        ))

      assigner.canonicalArtistName("the red birds") mustEqual "The Red Birds"
      assigner.canonicalArtistName("Socks and The Red Birds") mustEqual "Socks & Sweaters"
    }
  }

  "lookupName" in {
    ArtistAssignerFactory.lookupName("socks") mustEqual "socks"
    ArtistAssignerFactory.lookupName("Socks") mustEqual "socks"
    ArtistAssignerFactory.lookupName("The Red__--$$ BIRDS") mustEqual "redbirds"

    ArtistAssignerFactory.lookupName("Socks (tribute) others") mustEqual "socks"
    ArtistAssignerFactory.lookupName("Socks f/ birds") mustEqual "socks"
    ArtistAssignerFactory.lookupName("Socks x/ birds") mustEqual "socksx"
    ArtistAssignerFactory.lookupName("Socks & birds") mustEqual "socks"
    ArtistAssignerFactory.lookupName("Socks and birds") mustEqual "socks"
    ArtistAssignerFactory.lookupName("Socks, birds") mustEqual "socks"
    ArtistAssignerFactory.lookupName("The Socks") mustEqual "socks"

    ArtistAssignerFactory.lookupName("The & Socks") mustEqual "socks"
    ArtistAssignerFactory.lookupName("a/ Socks") mustEqual "asocks"
    ArtistAssignerFactory.lookupName("1( Socks") mustEqual "1socks"
  }

  private def artist(name: String): Artist = testArtist.copy(name = name, idOption = None)
}
