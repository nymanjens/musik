package app.common.testing
import java.time.Instant
import java.time.Month._

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApi.UpdateToken
import app.api.ScalaJsApi.UserPrototype
import hydro.common.OrderToken
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import hydro.models.modification.EntityModification
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.common.time.LocalDateTimes
import hydro.models.UpdatableEntity.LastUpdateTime

import scala.concurrent.duration._

object TestObjects {

  def orderTokenA: OrderToken = OrderToken.middleBetween(None, Some(OrderToken.middle))
  def orderTokenB: OrderToken = OrderToken.middleBetween(Some(OrderToken.middle), None)
  def orderTokenC: OrderToken = OrderToken.middleBetween(Some(orderTokenB), None)
  def orderTokenD: OrderToken = OrderToken.middleBetween(Some(orderTokenC), None)
  def orderTokenE: OrderToken = OrderToken.middleBetween(Some(orderTokenD), None)
  def testOrderToken: OrderToken = orderTokenC

  def testDate: LocalDateTime = LocalDateTimes.createDateTime(2008, MARCH, 13)
  def testInstantA: Instant = Instant.ofEpochMilli(999000001)
  def testInstantB: Instant = Instant.ofEpochMilli(999000002)
  def testInstantC: Instant = Instant.ofEpochMilli(999000003)
  def testInstantD: Instant = Instant.ofEpochMilli(999000004)
  def testInstant: Instant = testInstantA
  def testUpdateToken: UpdateToken = s"123782:12378"

  def testLastUpdateTime = LastUpdateTime.allFieldsUpdated(testInstant)

  def testUserA: User = User(
    loginName = "testUserA",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User A",
    isAdmin = false,
    idOption = Option(918273),
    lastUpdateTime = testLastUpdateTime,
  )
  def testUserB: User = User(
    loginName = "testUserB",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User B",
    isAdmin = false,
    idOption = Option(918274),
    lastUpdateTime = testLastUpdateTime,
  )
  def testUser: User = testUserA
  def testUserRedacted: User = testUser.copy(passwordHash = "<redacted>")

  def testUserPrototype = UserPrototype.create(
    id = testUser.id,
    loginName = testUser.loginName,
    plainTextPassword = "dlkfjasfd",
    name = testUser.name,
    isAdmin = testUser.isAdmin)

  def testArtist = Artist(name = "Test Artist", idOption = Some(128902378))
  def testAlbum = Album(
    relativePath = "folderA/folderB",
    title = "Test Album",
    artistId = Some(testArtist.id),
    year = Some(1999),
    idOption = Some(91723969))
  def testSong = Song(
    filename = "test-song.mp3",
    title = "Test Song",
    albumId = testAlbum.id,
    artistId = Some(testArtist.id),
    trackNumber = 8,
    duration = 2.minutes,
    disc = 1,
    idOption = Some(7646464),
  )
  def testPlaylistEntry = PlaylistEntry(
    songId = testSong.id,
    orderToken = orderTokenA,
    userId = testUser.id,
    idOption = Some(28316982172874774L),
  )
  def testPlayStatus = PlayStatus(
    currentPlaylistEntryId = testPlaylistEntry.id,
    hasStarted = true,
    stopAfterCurrentSong = true,
    userId = testUser.id,
    idOption = Some(1271626262),
    lastUpdateTime = testLastUpdateTime,
  )

  def testModificationA: EntityModification = EntityModification.Add(testArtist)
  def testModificationB: EntityModification =
    EntityModification.Add(testUserB.copy(passwordHash = "<redacted>"))
  def testModification: EntityModification = testModificationA

  def testGetInitialDataResponse: GetInitialDataResponse = GetInitialDataResponse(
    user = testUserRedacted,
    i18nMessages = Map("abc" -> "def"),
    nextUpdateToken = testUpdateToken
  )
}
