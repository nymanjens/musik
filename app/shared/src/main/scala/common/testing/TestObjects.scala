package common.testing
import models.media.{Album, Artist, Song}

import scala.concurrent.duration._
import java.time.Instant
import java.time.Month._

import api.ScalaJsApi.{GetInitialDataResponse, UpdateToken, UserPrototype}
import common.OrderToken
import common.time.{LocalDateTime, LocalDateTimes}
import models.media.{Song, Album, Artist}
import models.modification.EntityModification
import models.user.User

import scala.collection.immutable.Seq

object TestObjects {

  val orderTokenA: OrderToken = OrderToken.middleBetween(None, Some(OrderToken.middle))
  val orderTokenB: OrderToken = OrderToken.middleBetween(Some(OrderToken.middle), None)
  val orderTokenC: OrderToken = OrderToken.middleBetween(Some(orderTokenB), None)
  val orderTokenD: OrderToken = OrderToken.middleBetween(Some(orderTokenC), None)
  val orderTokenE: OrderToken = OrderToken.middleBetween(Some(orderTokenD), None)

  val testDate: LocalDateTime = LocalDateTimes.createDateTime(2008, MARCH, 13)
  val testInstant = Instant.ofEpochMilli(999000111)
  val testUpdateToken: UpdateToken = s"123782:12378"

  def testUserA: User = User(
    loginName = "testUserA",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User A",
    isAdmin = false,
    idOption = Option(918273)
  )
  val testUserB: User = User(
    loginName = "testUserB",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User B",
    isAdmin = false,
    idOption = Option(918274)
  )
  def testUser: User = testUserA
  def testUserRedacted: User = testUser.copy(passwordHash = "<redacted>")

  val testUserPrototype = UserPrototype.create(
    id = testUser.id,
    loginName = testUser.loginName,
    plainTextPassword = "dlkfjasfd",
    name = testUser.name,
    isAdmin = testUser.isAdmin)

  val testModificationA: EntityModification = EntityModification.Add(testUserRedacted)
  val testModificationB: EntityModification =
    EntityModification.Add(testUserB.copy(passwordHash = "<redacted>"))
  def testModification: EntityModification = testModificationA

  val testGetInitialDataResponse: GetInitialDataResponse = GetInitialDataResponse(
    user = testUserA,
    i18nMessages = Map("abc" -> "def"),
    nextUpdateToken = testUpdateToken
  )

  val testArtist = Artist(name = "Test Artist", idOption = Some(128902378))
  val testAlbum = Album(artistId = testArtist.id, title = "Test Album", idOption = Some(91723969))
  val testSong = Song(
    albumId = testAlbum.id,
    title = "Test Song",
    trackNumber = 8,
    duration = 2.minutes,
    year = 1999,
    disc = 1,
    idOption = Some(7646464),
  )
}
