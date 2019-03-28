package app.flux.stores.media.helpers

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import app.common.testing.TestObjects._
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.Prefix
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.QueryPart
import app.models.media.Song
import hydro.models.access.DbQuery
import hydro.models.access.EntityAccess
import hydro.models.access.JsEntityAccess
import utest._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.reflectiveCalls

object ComplexQueryFilterFactoryTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new app.common.testing.TestModule

    implicit val fakeEntityAccess = testModule.fakeEntityAccess

    implicit val complexQueryFilterFactory = new ComplexQueryFilterFactory

    "fromQuery()" - {
      "getSongFilter()" - {
        "empty filter" - async {
          val song1 = createSong()
          val song2 = createSong()

          val filter: DbQuery.Filter[Song] = await(complexQueryFilterFactory.fromQuery(" ").getSongFilter())

          filter(song1) ==> true
          filter(song2) ==> true
        }

        "song title filter" - async {
          val song1 = createSong(title = "abcd")
          val song2 = createSong(title = "defg")

          val filter: DbQuery.Filter[Song] =
            await(complexQueryFilterFactory.fromQuery("song:BCD").getSongFilter())

          filter(song1) ==> true
          filter(song2) ==> false
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
}
