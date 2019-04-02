package app.flux.stores.media.helpers

import app.models.media.Album
import app.models.media.Artist
import app.models.media.Song
import app.common.testing.TestObjects._
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.Prefix
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.QueryPart
import app.models.media.Song
import app.scala2js.AppConverters
import hydro.common.testing.FakeJsEntityAccess
import hydro.models.access.DbQuery
import hydro.models.Entity
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ComplexQueryFilterFactoryTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new app.common.testing.TestModule

    implicit val fakeEntityAccess = testModule.fakeEntityAccess

    implicit val complexQueryFilterFactory = new ComplexQueryFilterFactory

    "fromQuery()" - {
      "getSongFilter()" - {
        "empty filter" - {
          val song1 = persisted(createSong())
          val song2 = persisted(createSong())

          assertThatQuery(" ").containsExactlySongs(song1, song2)
        }

        "filter without prefix" - {
          val song1 = persisted(createSong(title = "abcd"))
          val song2 = persisted(createSong(title = "defg"))
          val song3 = persisted(createSong(title = "xxx", filename = "xbcdx.mp3"))

          assertThatQuery("BCD").containsExactlySongs(song1, song3)
        }

        "unrecognized prefix" - {
          val song1 = persisted(createSong(title = "ax:ad"))
          val song2 = persisted(createSong(title = "defg"))

          assertThatQuery("x:ad").containsExactlySongs(song1)
        }

        "filter with negation" - {
          val album1 = persisted(createAlbum(title = "berries"))
          val album2 = persisted(createAlbum(title = "apples"))
          val song1 = persisted(createSong(title = "abc1 X Y", albumId = album1.id))
          val song2A = persisted(createSong(title = "abc2A X Z", albumId = album2.id))
          val song2B = persisted(createSong(title = "abc2B X X", albumId = album2.id))

          "with prefix" - {
            "relevant to song" - {
              assertThatQuery("-song:abc2").containsExactlySongs(song1)
            }
            "relevant to album" - {
              assertThatQuery("-album:berries").containsExactlySongs(song2A, song2B)
            }
          }
          "witout prefix" - {
            assertThatQuery("-abc2").containsExactlySongs(song1)
          }
          "with quotes" - {
            assertThatQuery(""" -"X Z" """).containsExactlySongs(song1, song2B)
          }
        }
        "filter with multiple parts" - {
          val song1 = persisted(createSong(title = "AAA XBBBX"))
          val song2 = persisted(createSong(title = "CCC XDDDX"))
          val song3 = persisted(createSong(title = "AAA XEEEX"))

          assertThatQuery("aaa bbb").containsExactlySongs(song1)
        }

        "song title filter" - {
          val song1 = persisted(createSong(title = "abcd"))
          val song2 = persisted(createSong(title = "defg"))

          assertThatQuery("song:BCD").containsExactlySongs(song1)
        }

        "album title filter" - {
          val album1 = persisted(createAlbum(title = "berries"))
          val album2 = persisted(createAlbum(title = "apples"))
          val song1 = persisted(createSong(title = "abc1", albumId = album1.id))
          val song2 = persisted(createSong(title = "abc2", albumId = album2.id))

          assertThatQuery("album:PPLE song:abc")
            .containsExactlySongs(song2)
        }

        "artist name filter" - {
          val artist1 = persisted(createArtist("oranges"))
          val artist2 = persisted(createArtist("pears"))
          val song1 = persisted(createSong(title = "abc1", artistId = artist1.id))
          val song2 = persisted(createSong(title = "abc2", artistId = artist2.id))

          assertThatQuery("artist:PEAR song:abc")
            .containsExactlySongs(song2)
        }
      }
    }

    "parsePrefixAndSuffix()" - {
      "single colon" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          ComplexQueryFilterFactory.parsePrefixAndSuffix(s"$prefixString:some value") ==>
            Some((prefix, "some value"))
        }
      }
      "multiple colons" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          ComplexQueryFilterFactory
            .parsePrefixAndSuffix(s"$prefixString:some: value") ==> Some((prefix, "some: value"))
        }
      }
      "wrong prefix" - {
        ComplexQueryFilterFactory.parsePrefixAndSuffix(s"unknownPrefix:some: value") ==> None
      }
      "empty string" - {
        ComplexQueryFilterFactory.parsePrefixAndSuffix(s"") ==> None
      }
      "empty prefix" - {
        ComplexQueryFilterFactory.parsePrefixAndSuffix(s":some value") ==> None
      }
      "empty suffix" - {
        ComplexQueryFilterFactory.parsePrefixAndSuffix(s"category:") ==> None
      }
      "no colons" - {
        ComplexQueryFilterFactory.parsePrefixAndSuffix(s"category") ==> None
      }
    }

    "splitInParts()" - {
      "empty string" - {
        ComplexQueryFilterFactory.splitInParts("") ==> Seq()
      }
      "negation" - {
        ComplexQueryFilterFactory.splitInParts("-a c -def") ==>
          Seq(QueryPart.not("a"), QueryPart("c"), QueryPart.not("def"))
      }
      "double negation" - {
        ComplexQueryFilterFactory.splitInParts("--a") ==> Seq(QueryPart.not("-a"))
      }
      "double quotes" - {
        ComplexQueryFilterFactory.splitInParts(""" "-a c" """) ==> Seq(QueryPart("-a c"))
      }
      "single quotes" - {
        ComplexQueryFilterFactory.splitInParts(" '-a c' ") ==> Seq(QueryPart("-a c"))
      }
      "negated quotes" - {
        ComplexQueryFilterFactory.splitInParts("-'XX YY'") ==> Seq(QueryPart.not("XX YY"))
      }
      "quote after colon" - {
        ComplexQueryFilterFactory.splitInParts("-don:'t wont'") ==> Seq(QueryPart.not("don:t wont"))
      }
      "quote inside text" - {
        ComplexQueryFilterFactory
          .splitInParts("-don't won't") ==> Seq(QueryPart.not("don't"), QueryPart("won't"))
      }
    }
  }

  private def persisted[E <: Entity](entity: E)(implicit entityAccess: FakeJsEntityAccess): E = {
    entity match {
      case e: Artist => entityAccess.addRemotelyAddedEntities(e)
      case e: Album  => entityAccess.addRemotelyAddedEntities(e)
      case e: Song   => entityAccess.addRemotelyAddedEntities(e)
    }
    entity
  }

  private def assertThatQuery(query: String)(implicit complexQueryFilterFactory: ComplexQueryFilterFactory,
                                             entityAccess: FakeJsEntityAccess) = {
    new Object {
      def containsExactlySongs(expected: Song*): Future[Unit] = async {
        val filter = await(complexQueryFilterFactory.fromQuery(query).getSongFilter())
        assertContainSameElements[Song](
          entityAccess.newQuerySync[Song]().filter(filter).data(),
          expected,
          properties = Seq(_.title))
      }

      private def assertContainSameElements[E](iterable1: Iterable[E],
                                               iterable2: Iterable[E],
                                               properties: Seq[E => Any] = Seq()): Unit = {
        val set1 = iterable1.toSet
        val set2 = iterable2.toSet
        for (propertyFunc <- properties) {
          set1.map(propertyFunc) ==> set2.map(propertyFunc)
        }
        set1 ==> set2
      }
    }
  }
}
