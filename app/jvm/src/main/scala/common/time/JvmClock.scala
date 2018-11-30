package common.time

import java.time.{Instant, LocalDate, LocalTime, ZoneId}

import com.google.inject._

final class JvmClock extends Clock {
  // TODO: Make this configurable
  private val zone = ZoneId.of("Europe/Paris")

  private val initialInstant: Instant = Instant.now
  private val initialNanos: Long = System.nanoTime

  override def now = {
    val date = LocalDate.now(zone)
    val time = LocalTime.now(zone)
    LocalDateTime.of(date, time)
  }

  override def nowInstant = initialInstant plusNanos (System.nanoTime - initialNanos)
}
