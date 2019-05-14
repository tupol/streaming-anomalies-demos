package org.tupol.demo.streaming.anomalies.demos

import org.tupol.demo.streaming.states._
import org.tupol.stats.{ EWeightedStats, Stats }

package object demo_11 {

  val Alpha = 0.3 // this determines how "aggressive" should the stats react to changes; the larger the more aggressive

  case class DataRecord(key: String, value: Double, tags: Seq[String])

  case class DataState(record: DataRecord, stats: Stats, ewStats: EWeightedStats) extends StateUpdater[DataState, DataRecord] {
    override def update(record: DataRecord): DataState =
      DataState(record, stats.append(record.value), ewStats |+| record.value)
    override def update(records: Iterable[DataRecord]): DataState =
      records.foldLeft(this)((result, record) => result |+| record)
    def probability3S(x: Double, sigmaIncrements: Int = 10) = {
      val epsilon = if (ewStats.stdev() == 0) 1E-12 else ewStats.stdev() / sigmaIncrements
      ewStats.probabilityNSigma(x, epsilon, 3)
    }
  }

  def InitialState(record: DataRecord): DataState =
    DataState(record, Stats.zeroDouble(record.value), EWeightedStats.zeroDouble(Alpha, record.value))

  def printLine =
    println("+-------+------------+------------+------------+------------+------------+------------+")
  def printHeader = {
    printLine
    println("| Anom? | Prob norm  |   Value    |  EW Mean   |  EW St Dev |    Mean    |   St Dev   |")
    printLine
  }
}
