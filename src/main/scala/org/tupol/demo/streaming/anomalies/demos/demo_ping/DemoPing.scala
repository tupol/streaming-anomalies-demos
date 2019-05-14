package org.tupol.demo.streaming.anomalies.demos.demo_ping

import org.apache.spark.sql.streaming.{ GroupState, GroupStateTimeout, OutputMode }
import org.apache.spark.sql.{ Dataset, SparkSession }
import org.tupol.spark.SparkFun
import org.tupol.spark.implicits._
import org.tupol.spark.io.streaming.structured.{ FormatAwareStreamingSinkConfiguration, KafkaStreamDataSinkConfiguration, KafkaStreamDataSourceConfiguration }
import org.tupol.utils.config.Configurator

import scala.util.Try

object DemoPing extends SparkFun[DemoPingContext, Unit](DemoPingContext(_).get) {

  override def run(implicit spark: SparkSession, context: DemoPingContext): Unit =
    Try(streamProcessor).map(_.awaitTermination())

  def streamProcessor(implicit spark: SparkSession, context: DemoPingContext) = {
    import spark.implicits._

    val inputStream = spark.source(context.input).read
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)",
        "topic", "partition", "offset", "timestamp", "timestampType")
      .as[StringKafkaMessage]
      .mapPartitions(mx => mx.map(m => (m, PingData.fromString(m.value))))
      .filter(_._2.isDefined)
      .map(_._2.get)
      .filter(!_.timeout)

    val statsBySource: Dataset[(PingData, PingDataState)] = inputStream
      .groupByKey(_.sourceId)
      .flatMapGroupsWithState[PingDataState, (PingData, PingDataState)](
        OutputMode.Append(), GroupStateTimeout.NoTimeout)(stateUpdaterByRecord)

    val statsByTarget: Dataset[(PingData, PingDataState)] = inputStream
      .groupByKey(_.targetName)
      .flatMapGroupsWithState[PingDataState, (PingData, PingDataState)](
        OutputMode.Append(), GroupStateTimeout.NoTimeout)(stateUpdaterByRecord)

    val joinedSteams = statsBySource
      .joinWith(statsByTarget, statsBySource("_1") === statsByTarget("_1"))
      .map {
        case ((_, sStats), (pd, tStats)) => PingDataOut(
          isAnomaly(pd, tStats, 0.01), pd, PingStats(sStats), PingStats(tStats))
      }
    //        .map { case ((_, sStats), (pd, tStats)) => formattedResult(pd, sStats, tStats) }

    val outputStream = context.output match {
      case _: KafkaStreamDataSinkConfiguration => joinedSteams.toJSON.toDF("value")
      case _ => joinedSteams.toDF
    }

    outputStream.streamingSink(context.output).write
  }

  def formattedResult(record: PingData, sourceState: PingDataState, targetState: PingDataState) =
    f"| ${isAnomaly(record, targetState)}%5s | ${record.sourceId}%15s | ${formattedStats(record, sourceState)} | ${record.targetName}%15s| ${record.icmpSeq}%3d | ${record.time}%8.3f | ${formattedStats(record, targetState)} |"

  def formattedStats(record: PingData, state: PingDataState) =
    f"${state.ewProbability3S(record.time)}%8.6f | ${state.ewStats.mean}%10.3f | ${state.ewStats.stdev()}%10.3f " +
      f"| ${state.stats.probabilityNSigma(record.time, 10, 2)}%8.6f | ${state.stats.mean}%10.3f | ${state.stats.stdev()}%10.3f"

  def stateUpdaterByRecord(key: String, values: Iterator[PingData], state: GroupState[PingDataState]): Iterator[(PingData, PingDataState)] = {
    values.toSeq.sortBy(_.timestamp.getNanos) match {
      case Nil => Iterator.empty
      case data =>
        val initialState = state.getOption.getOrElse(InitialState(data.head)) |+| data.head
        state.update(initialState)
        val result = data.tail.foldLeft(Seq((data.head, initialState))) { (acc, rx) =>
          val newState = acc.head._2 |+| rx
          state.update(newState)
          (rx, newState) +: acc
        }
        result.toIterator
    }
  }

}

case class DemoPingContext(input: KafkaStreamDataSourceConfiguration, output: FormatAwareStreamingSinkConfiguration)
object DemoPingContext extends Configurator[DemoPingContext] {
  import com.typesafe.config.Config
  import org.tupol.spark.io._
  import org.tupol.spark.io.streaming.structured._
  import org.tupol.utils.config._
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._
  override def validationNel(config: Config): ValidationNel[Throwable, DemoPingContext] = {
    config.extract[KafkaStreamDataSourceConfiguration]("input") |@|
      config.extract[FormatAwareStreamingSinkConfiguration]("output") apply
      DemoPingContext.apply
  }
}

