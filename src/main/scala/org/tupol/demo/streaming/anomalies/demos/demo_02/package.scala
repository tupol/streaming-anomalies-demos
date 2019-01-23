package org.tupol.demo.streaming.anomalies.demos

import org.tupol.demo.streaming.states._
import org.tupol.stats.EWeightedStatsOps._
import org.tupol.stats._


package object demo_02 {

  val Alpha = 0.3 // this determines how "aggressive" should the stats react to changes; the larger the more aggressive

  case class DataRecord(value: Double)

  case class DataState(previousRecord: Option[DataRecord], valueStats: EWeightedStats[Double]) extends
    StateUpdater[DataState, DataRecord]{
    override def update(record: DataRecord): DataState =
      DataState(Some(record), valueStats |+| record.value)
    override def update(records: Iterable[DataRecord]): DataState =
      records.foldLeft(this)((result, record) => result |+| record)
    def probability3S(x: Double, sigmaIncrements: Int = 10) = {
      val epsilon = if(valueStats.stdev() == 0) 1E-12 else valueStats.stdev()/sigmaIncrements
      valueStats.probabilityNSigma(x, epsilon, 3)
    }
  }

  def InitialState(record: DataRecord): DataState =
    DataState(None, DoubleEWeightedStats.zeroDouble(Alpha, record.value))

}
