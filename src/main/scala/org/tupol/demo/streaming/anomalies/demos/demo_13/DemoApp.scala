package org.tupol.demo.streaming.anomalies.demos.demo_13

import java.sql.Timestamp

import org.apache.spark.sql.streaming.GroupState
import org.tupol.spark.io.streaming.structured.{ GenericStreamDataSinkConfiguration, GenericStreamDataSourceConfiguration }
import org.tupol.utils.config.Configurator

object DemoApp {

  def isAnomaly(record: DataRecord, dataState: DataState, confidence: Double = 0.99): Boolean =
    dataState.probability3S(record.value) <= 1.0 - confidence

  def signalFunction(x: Double) = if (x % 100 < 35) 0.0 else if (x % 100 >= 35 && x % 100 < 70) 10.0 else 0.0
  def smallNoiseFunction(x: Double) = 2 * (0.5 - math.random)
  def trainingValueFunction(x: Double) = signalFunction(x) + smallNoiseFunction(x)
  def largeNoiseFunction(x: Double) = 2 * smallNoiseFunction(x)
  def runtimeValueFunction(x: Double) = signalFunction(x) + largeNoiseFunction(x)
  //
  //
  //
  //  println("Training Data")
  //  printHeader
  //  val trainingDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(trainingValueFunction(x)))
  //  val trainedState =
  //    trainingDataStream.foldLeft(InitialState(trainingDataStream.head)) { (state, record) =>
  //      val result = state |+| record
  //      println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.ewStats.mean}%10.3f | ${result.ewStats.stdev()}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
  //      result
  //    }
  //  printLine
  //
  //  println
  //  println("Runtime Data")
  //  printHeader
  //  val runtimeDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(runtimeValueFunction(x)))
  //  val prediction: DataState = runtimeDataStream.foldLeft(trainedState) { (state, record) =>
  //    val result = stateUpdaterWithCorrectionForAnomalies(state, record)
  //    println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.ewStats.mean}%10.3f | ${result.ewStats.stdev()}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
  //    result
  //  }
  //  printLine
  //
  //  /** Allow the state to be corrected even when anomalies are present, by computing the stats as if it would be just on the edge */
  //  def stateUpdaterWithCorrectionForAnomalies(state: DataState, record: DataRecord): DataState =
  //    if (isAnomaly(record, state)) {
  //      val correctionValue = state.ewStats.avg + 3 * state.ewStats.stdev() * math.signum(record.value - state.ewStats.avg)
  //      val correctedState = state |+| DataRecord(correctionValue)
  //      correctedState.copy(previousRecord = Some(record))
  //    } else state |+| record

  def stateUpdaterByRecord(key: String, values: Iterator[(DataRecord, Timestamp)], state: GroupState[DataState]): Iterator[(DataRecord, Timestamp, DataState)] = {
    values.toSeq.sortBy(x => x._2.getNanos) match {
      case Nil => Iterator.empty
      case data =>
        val initialState = state.getOption.getOrElse(InitialState(data.head._1)) |+| data.head._1
        //        println("--------------------@@@@@@@@@@@@@@-----------------------")
        //        println(s"* initialState($key): $initialState")
        state.update(initialState)
        val result = data.tail.foldLeft(Seq((data.head._1, data.head._2, initialState))) { (acc, rx) =>
          val newState = acc.head._3 |+| rx._1
          state.update(newState)
          //          println(s"* updatedState($key): $newState")
          (rx._1, rx._2, newState) +: acc
        }
        //        println("# Results:")
        //        result.foreach(x => println(s"# $x"))
        result.toIterator
    }
  }

  def stateUpdaterByKey(key: String, values: Iterator[(DataRecord, Timestamp)], state: GroupState[DataState]): Iterator[(String, DataState)] =
    values.toSeq.sortBy(x => x._2.getNanos).map(_._1) match {
      case Nil => Iterator.empty
      case data =>
        //        println("--------------------===============-----------------------")
        val initialState = state.getOption.getOrElse(InitialState(data.head))
        //        println(s"* initialState($key): $initialState")
        state.update(initialState)
        //        data.foreach(x => println(s"- ($key) : $x"))
        val newState = data.foldLeft(initialState) { (acc, rx) => acc |+| rx }
        //        println(s"* updatedState($key): $newState")
        state.update(newState)
        Iterator((key, newState))
    }

}

case class DemoAppContext(input: GenericStreamDataSourceConfiguration, output: GenericStreamDataSinkConfiguration)
object DemoAppContext extends Configurator[DemoAppContext] {
  import com.typesafe.config.Config
  import org.tupol.spark.io._
  import org.tupol.spark.io.streaming.structured._
  import org.tupol.utils.config._
  import scalaz.ValidationNel
  import scalaz.syntax.applicative._
  override def validationNel(config: Config): ValidationNel[Throwable, DemoAppContext] = {
    config.extract[GenericStreamDataSourceConfiguration]("input") |@|
      config.extract[GenericStreamDataSinkConfiguration]("output") apply
      DemoAppContext.apply
  }
}
