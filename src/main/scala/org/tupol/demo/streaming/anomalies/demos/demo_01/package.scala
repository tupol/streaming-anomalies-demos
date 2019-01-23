package org.tupol.demo.streaming.anomalies.demos

import org.tupol.demo.streaming.states._
import org.tupol.stats.StatsOps._
import org.tupol.stats._


package object demo_01 {

  case class DataRecord(value: Double)

  case class DataState(previousRecord: Option[DataRecord], valueStats: Stats[Double]) extends
    StateUpdater[DataState, DataRecord]{
    override def update(record: DataRecord): DataState =
      DataState(Some(record), valueStats |+| record.value)
    override def update(records: Iterable[DataRecord]): DataState =
      records.foldLeft(this)((result, record) => result |+| record)
    def probability3S(x: Double, sigmaIncrements: Int = 30) = {
      if((valueStats.stdev() == 0 && x == valueStats.mean) || valueStats.count < 2) 1.0
      else valueStats.probabilityNSigma(x, valueStats.stdev()/sigmaIncrements, 3)
    }
  }

  def InitialState(record: DataRecord): DataState =
    DataState(None, DoubleStats.fromDoubles(record.value))

}
