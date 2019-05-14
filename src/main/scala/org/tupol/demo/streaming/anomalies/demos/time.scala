package org.tupol.demo.streaming.anomalies.demos

import java.sql.Timestamp
import java.time.{ Duration, LocalDate, LocalDateTime, ZoneId }
import java.time.format.DateTimeFormatter

package object time {

  val UTC = ZoneId.of("UTC")
  /**
   * Constant used to express an undetermined time period.
   * We consider this value to be big enough for the lifetime of this product.
   */
  val EndOfTime = Timestamp.valueOf("9999-12-31 23:59:59.999")

  /** Constant used to express the reference epoch time. */
  val StartOfTime = new Timestamp(0L)

  /** Simple wrapper for the current timestamp */
  def now = new Timestamp(new java.util.Date().getTime)

  /** Parse a string into a `LocalDateTime` object which gets converted into a `Timestamp` */
  def parseDateTime(inputString: String): Timestamp = {
    val localDateTime = LocalDateTime.parse(inputString, DateTimeFormatter.ISO_DATE_TIME)
    Timestamp.valueOf(localDateTime)
  }

  /**
   * Convert a given timestamp from UTC to the given timeZone.
   * Timestamp does not carry timeZone information.
   */
  def utcToLocalByTimezone(timestamp: Timestamp, timeZone: ZoneId): Timestamp =
    Timestamp.valueOf(timestamp.toLocalDateTime.atZone(UTC).withZoneSameInstant(timeZone).toLocalDateTime)

  /**
   * Convert a given timestamp from the given timeZone to UTC.
   * Timestamp does not carry timeZone information.
   */
  def localToUtcByTimezone(timestamp: Timestamp, timeZone: ZoneId): Timestamp =
    Timestamp.valueOf(timestamp.toLocalDateTime.atZone(timeZone).withZoneSameInstant(UTC).toLocalDateTime)

  def max(timestamp1: Timestamp, timestamp2: Timestamp) = if (timestamp1 >= timestamp2) timestamp1 else timestamp2

  def min(timestamp1: Timestamp, timestamp2: Timestamp) = if (timestamp1 <= timestamp2) timestamp1 else timestamp2

  implicit class TimestampOps(val timestamp: Timestamp) extends AnyVal {
    def utcToLocalByTimezone(timeZone: ZoneId): Timestamp = time.utcToLocalByTimezone(timestamp, timeZone)

    def localToUtcByTimezone(timeZone: ZoneId): Timestamp = time.localToUtcByTimezone(timestamp, timeZone)

    def <(other: Timestamp): Boolean = timestamp.getTime < other.getTime

    def <=(other: Timestamp): Boolean = timestamp.getTime <= other.getTime

    def >(other: Timestamp): Boolean = timestamp.getTime > other.getTime

    def >=(other: Timestamp): Boolean = timestamp.getTime >= other.getTime

    /**
     * Get the difference in milliseconds between two timestamps
     *
     * @param other timestamp that will be subtracted out of this timestamp
     * @return
     */
    def -(other: Timestamp): Duration = Duration.ofMillis(timestamp.getTime - other.getTime)
  }

  implicit class LocalDateTimeOps(val localDateTime: LocalDateTime) extends AnyVal {
    def toTimestamp: Timestamp = Timestamp.valueOf(localDateTime)
  }

  implicit def timestampToLocalDateTime(timestamp: Timestamp): LocalDateTime = timestamp.toLocalDateTime

  implicit def localDateTimeToTimestamp(localDateTime: LocalDateTime): Timestamp = Timestamp.valueOf(localDateTime)

  /** Convert the given local date into the corresponding UTC timestamps that match the start and the end of the local date. */
  def localDateToUtcTimestamps(localDate: LocalDate, timeZone: ZoneId): (Timestamp, Timestamp) = {
    val startDateUtc = Timestamp.valueOf(localDate.atStartOfDay)
      .localToUtcByTimezone(timeZone)
    val endDateUtc = Timestamp.valueOf(localDate.atStartOfDay.plusDays(1))
      .localToUtcByTimezone(timeZone)
    (startDateUtc, endDateUtc)
  }
}
