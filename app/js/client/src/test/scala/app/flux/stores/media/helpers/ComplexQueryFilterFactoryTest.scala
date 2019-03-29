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
          val song1 = createSong()
          val song2 = createSong()

          withPersisted(song1, song2).assertThatQuery(" ").containsExactlySongs(song1, song2)
        }

        "unrecognized tag" - {
          val song1 = createSong(title = "ay:ad")
          val song2 = createSong(title = "defg")

          withPersisted(song1, song2).assertThatQuery("y:ad").containsExactlySongs(song1)
        }

        "song title filter" - {
          val song1 = createSong(title = "abcd")
          val song2 = createSong(title = "defg")

          withPersisted(song1, song2).assertThatQuery("song:BCD").containsExactlySongs(song1)
        }

        "album title filter" - {
          val album1 = createAlbum(title = "berries")
          val album2 = createAlbum(title = "apples")
          val song1 = createSong(title = "abc", albumId = album1.id)
          val song2 = createSong(title = "abc", albumId = album2.id)

          withPersisted(song1, song2).assertThatQuery("album:PPLE song:abc").containsExactlySongs(song1)
        }

//      "filter without prefix" - {
//        val transaction1 = createTransaction(description = "cat dog fish")
//        val transaction2 = createTransaction(description = "fish")
//        val transaction3 = createTransaction(description = "cat")
//
//        withTransactions(transaction1, transaction2, transaction3)
//          .assertThatQuery("  fish  ")
//          .containsExactly(transaction1, transaction2)
//      }
//      "filter with negation" - {
//        val transaction1 = createTransaction(description = "cat dog fish")
//        val transaction2 = createTransaction(description = "fish")
//        val transaction3 = createTransaction(description = "cat")
//
//        withTransactions(transaction1, transaction2, transaction3)
//          .assertThatQuery("-description:fish")
//          .containsExactly(transaction3)
//        withTransactions(transaction1, transaction2, transaction3)
//          .assertThatQuery(""" -"dog f" """)
//          .containsExactly(transaction2, transaction3)
//      }
//      "filter with multiple parts" - {
//        val transaction1 = createTransaction(description = "cat dog fish", tags = Seq("monkey"))
//        val transaction2 = createTransaction(description = "fish")
//        val transaction3 = createTransaction(description = "cat", tags = Seq("monkey"))
//
//        withTransactions(transaction1, transaction2, transaction3)
//          .assertThatQuery("fish tag:monkey")
//          .containsExactly(transaction1)
//        withTransactions(transaction1, transaction2, transaction3)
//          .assertThatQuery("fish -tag:monkey")
//          .containsExactly(transaction2)
//      }
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

  private def withPersisted(storedEntities: Entity*)(
      implicit complexQueryFilterFactory: ComplexQueryFilterFactory,
      entityAccess: FakeJsEntityAccess) = {
    storedEntities map {
      case e: Artist => entityAccess.addRemotelyAddedEntities(e)
      case e: Album  => entityAccess.addRemotelyAddedEntities(e)
      case e: Song   => entityAccess.addRemotelyAddedEntities(e)
    }

    new Object {
      def assertThatQuery(query: String) = new Object {
        def containsExactlySongs(expected: Song*): Future[Unit] = async {
          val filter = await(complexQueryFilterFactory.fromQuery(query).getSongFilter())
          assertContainSameElements[Song](
            entityAccess.newQuerySync[Song]().filter(filter).data(),
            expected,
            properties = Seq(_.title))
        }
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
