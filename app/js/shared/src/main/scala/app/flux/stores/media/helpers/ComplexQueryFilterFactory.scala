package app.flux.stores.media.helpers

import app.flux.stores.media.helpers.ComplexQueryFilterFactory.FilterPairFactory

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.async
import scala.async.Async.await
import hydro.common.GuavaReplacement.Splitter
import hydro.common.ScalaUtils.visibleForTesting
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.Prefix
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.QueryFilterPair
import app.flux.stores.media.helpers.ComplexQueryFilterFactory.QueryPart
import app.models.access.ModelFields
import app.models.media.Song
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.DbQuery
import hydro.models.access.JsEntityAccess
import hydro.models.access.ModelField

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try
import scala.util.Success
import scala.util.Failure

final class ComplexQueryFilterFactory(implicit entityAccess: JsEntityAccess) {

  // **************** Public API **************** //
  def fromQuery(query: String): ComplexQueryFilter = new ComplexQueryFilter(query)

  // **************** Inner types **************** //
  final class ComplexQueryFilter(query: String) {

    // **************** Public API **************** //
    def toSongFilter(): Future[DbQuery.Filter[Song]] = async {

      parseQuery(filterPairFactory = SongPrefixFilterPairFactory ifUnsupported SongFallbackFilterPairFactory)
    }

    // **************** Private helper methods **************** //
    private def parseQuery[E](filterPairFactory: FilterPairFactory[E]): DbQuery.Filter[E] = {
      val filters: Seq[DbQuery.Filter[E]] =
        splitInParts(query)
          .flatMap {
            case QueryPart(string, negated) =>
              val maybeFilterPair = filterPairFactory.createIfSupported(singlePartWithoutNegation = string)

              maybeFilterPair.map { filterPair =>
                if (negated) {
                  filterPair.negated
                } else {
                  filterPair
                }
              }
          }
          .sortBy(_.estimatedExecutionCost)
          .map(_.positiveFilter)

      if (filters.nonEmpty) {
        DbQuery.Filter.And(filters)
      } else {
        DbQuery.Filter.NullFilter()
      }
    }

    private object SongPrefixFilterPairFactory extends FilterPairFactory[Song] {
      override def createIfSupported(singlePartWithoutNegation: String): Option[QueryFilterPair[Song]] = {
        parsePrefixAndSuffix(singlePartWithoutNegation) flatMap {
          case (prefix, suffix) =>
            prefix match {
              case Prefix.SongTitle =>
                Some(QueryFilterPair.containsIgnoreCase(ModelFields.Song.title, suffix))
              case _ => None
            }
        }
      }
    }
    private object SongFallbackFilterPairFactory extends FilterPairFactory[Song] {
      override def createIfSupported(singlePartWithoutNegation: String): Option[QueryFilterPair[Song]] = {
        parsePrefixAndSuffix(singlePartWithoutNegation) flatMap {
          case (prefix, suffix) =>
            prefix match {
              case Prefix.SongTitle =>
                Some(QueryFilterPair.containsIgnoreCase(ModelFields.Song.title, suffix))
              case _ => None
            }
        }
      }
    }

    @visibleForTesting private[helpers] def parsePrefixAndSuffix(string: String): Option[(Prefix, String)] = {
      val prefixStringToPrefix: Map[String, Prefix] = {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } yield prefixString -> prefix
      }.toMap

      val split = Splitter.on(':').split(string).toList
      split match {
        case prefix :: suffix if (prefixStringToPrefix contains prefix) && suffix.mkString(":").nonEmpty =>
          Some((prefixStringToPrefix(prefix), suffix.mkString(":")))
        case _ => None
      }
    }
    @visibleForTesting private[helpers] def splitInParts(query: String): Seq[QueryPart] = {
      val quotes = Seq('"', '\'')
      val parts = mutable.Buffer[QueryPart]()
      val nextPart = new StringBuilder
      var currentQuote: Option[Char] = None
      var negated = false

      for (char <- query) char match {
        case '-' if nextPart.isEmpty && currentQuote.isEmpty && !negated =>
          negated = true
        case _
            if (quotes contains char) && (nextPart.isEmpty || nextPart
              .endsWith(":")) && currentQuote.isEmpty =>
          currentQuote = Some(char)
        case _ if currentQuote contains char =>
          currentQuote = None
        case ' ' if currentQuote.isEmpty && nextPart.nonEmpty =>
          parts += QueryPart(nextPart.result().trim, negated = negated)
          nextPart.clear()
          negated = false
        case ' ' if currentQuote.isEmpty && nextPart.isEmpty =>
        // do nothing
        case _ =>
          nextPart += char
      }
      if (nextPart.nonEmpty) {
        parts += QueryPart(nextPart.result().trim, negated = negated)
      }
      Seq(parts: _*)
    }

  }
}

object ComplexQueryFilterFactory {
  private case class QueryFilterPair[E](positiveFilter: DbQuery.Filter[E],
                                        negativeFilter: DbQuery.Filter[E],
                                        estimatedExecutionCost: Int) {
    def negated: QueryFilterPair[E] =
      QueryFilterPair(
        positiveFilter = negativeFilter,
        negativeFilter = positiveFilter,
        estimatedExecutionCost = estimatedExecutionCost)
  }

  private object QueryFilterPair {
    def isEqualTo[V, E](field: ModelField[V, E], value: V): QueryFilterPair[E] =
      anyOf(field, Seq(value))

    def anyOf[V, E](field: ModelField[V, E], values: Seq[V]): QueryFilterPair[E] =
      values match {
        case Seq(value) =>
          QueryFilterPair(
            estimatedExecutionCost = 1,
            positiveFilter = field === value,
            negativeFilter = field !== value)
        case _ =>
          QueryFilterPair(
            estimatedExecutionCost = 2,
            positiveFilter = field isAnyOf values,
            negativeFilter = field isNoneOf values)
      }

    def containsIgnoreCase[E](field: ModelField[String, E], substring: String): QueryFilterPair[E] =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field containsIgnoreCase substring,
        negativeFilter = field doesntContainIgnoreCase substring)

    def seqContains[E](field: ModelField[Seq[String], E], value: String): QueryFilterPair[E] =
      QueryFilterPair(
        estimatedExecutionCost = 3,
        positiveFilter = field contains value,
        negativeFilter = field doesntContain value
      )
  }

  @visibleForTesting private[helpers] case class QueryPart(unquotedString: String, negated: Boolean = false)
  @visibleForTesting private[helpers] object QueryPart {
    def not(unquotedString: String): QueryPart = QueryPart(unquotedString, negated = true)
  }

  @visibleForTesting private[helpers] sealed abstract class Prefix private (val prefixStrings: Seq[String]) {
    override def toString = getClass.getSimpleName
  }
  @visibleForTesting private[helpers] object Prefix {
    def all: Seq[Prefix] =
      Seq(ArtistName, AlbumTitle, SongTitle, RelativePath, Year)

    // Artist prefixes
    object ArtistName extends Prefix(Seq("artist", "art", "a"))
    // Album prefixes
    object AlbumTitle extends Prefix(Seq("album", "alb", "b"))
    object RelativePath extends Prefix(Seq("path", "p"))
    object Year extends Prefix(Seq("year", "y"))
    // Song prefixes
    object SongTitle extends Prefix(Seq("song", "s"))
  }

  private trait FilterPairFactory[E] {
    def createIfSupported(singlePartWithoutNegation: String): Option[QueryFilterPair[E]]

    def ifUnsupported(that: FilterPairFactory[E]): FilterPairFactory[E] = { singlePartWithoutNegation =>
      this.createIfSupported(singlePartWithoutNegation) orElse
        that.createIfSupported(singlePartWithoutNegation)
    }
  }
}
