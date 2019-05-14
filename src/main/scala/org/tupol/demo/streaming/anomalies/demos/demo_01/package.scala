package org.tupol.demo.streaming.anomalies.demos

import org.tupol.demo.streaming.states._
import org.tupol.stats._

package object demo_01 {

  case class DataRecord(value: Double)

  case class DataState(previousRecord: Option[DataRecord], stats: Stats) extends StateUpdater[DataState, DataRecord] {
    override def update(record: DataRecord): DataState =
      DataState(Some(record), stats |+| record.value)
    override def update(records: Iterable[DataRecord]): DataState =
      records.foldLeft(this)((result, record) => result |+| record)
    def probability3S(x: Double, sigmaIncrements: Int = 30) = {
      if ((stats.stdev() == 0 && x == stats.mean) || stats.count < 2) 1.0
      else stats.probabilityNSigma(x, stats.stdev() / sigmaIncrements, 3, 1E-9)
    }
  }

  def InitialState(record: DataRecord): DataState =
    DataState(None, Stats.fromDouble(record.value))

}
