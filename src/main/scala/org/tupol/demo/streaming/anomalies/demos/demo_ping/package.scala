package org.tupol.demo.streaming.anomalies.demos

import java.sql.Timestamp

import org.tupol.demo.streaming.anomalies.demos.time.parseDateTime
import org.tupol.demo.streaming.states.StateUpdater
import org.tupol.stats.Stats

import scala.util.Try

package object demo_ping {

  /**
   * Alpha determines how "aggressive" should the stats react to changes; the larger the more aggressive.
   * History is weighted 1 - alpha
   */
  val Alpha = 0.5

  case class PingData(timestamp: Timestamp, sourceId: String, targetName: String, icmpSeq: Long,
    targetIP: String, bytesNo: Int, ttl: Int, time: Double, timeout: Boolean = false)

  object PingData {
    val HeaderPattern = "(.*),(.*),(.*)".r
    val SuccessPattern = s"""${HeaderPattern.toString},(\\d*) bytes from ([\\d.]*).* icmp_seq=(\\d*) ttl=(\\d*) time=([\\d]*[.][\\d]*) ms""".r
    val TimeoutPattern = s"""${HeaderPattern.toString},Request timeout for icmp_seq (\\d*).*""".r
    def fromString(input: String): Option[PingData] = input match {
      case SuccessPattern(timestamp, sourceId, targetHost, bytesNo, targetIp, icmpSeq, ttl, time) =>
        Try(PingData(parseDateTime(timestamp), sourceId, targetHost, icmpSeq.toLong,
          targetIp, bytesNo.toInt, ttl.toInt, time.toDouble, false)).toOption
      case TimeoutPattern(timestamp, sourceId, targetHost, icmpSeq) =>
        Try(PingData(parseDateTime(timestamp), sourceId, targetHost, icmpSeq.toLong,
          "", -1, -1, -1, true)).toOption
      case _ => None
    }
  }

  case class PingDataState(record: PingData, stats: Stats) extends StateUpdater[PingDataState, PingData] {
    override def update(record: PingData): PingDataState =
      PingDataState(record, stats |+| record.time)
    override def update(records: Iterable[PingData]): PingDataState =
      records.foldLeft(this)((result, record) => result |+| record)
    def probability3S(x: Double, sigmaIncrements: Int = 10) =
      stats.probabilityNSigma(x, 10, 3)
  }

  def InitialState(record: PingData): PingDataState =
    PingDataState(record, Stats.zeroDouble(record.time))

  case class StringKafkaMessage(key: String, value: String, topic: String, partition: Int, offset: Long,
    timestamp: Long, timestampType: Int)

  def isAnomaly(record: PingData, state: PingDataState, threshold: Double = 0.6): Boolean = {
    require(threshold > 0 && threshold < 1, "The threshold should be between 0 and 1 exclusive.")
    state.probability3S(record.time) <= threshold && record.time > state.stats.avg
  }

  case class PingDataOut(anomaly: Boolean, ping: PingData, sourceStats: PingStats, targetStats: PingStats)
  case class PingStats(probability: Double, stats: Stats)
  object PingStats {
    def apply(state: PingDataState): PingStats = PingStats(
      state.probability3S(state.record.time),
      state.stats)
  }

}
