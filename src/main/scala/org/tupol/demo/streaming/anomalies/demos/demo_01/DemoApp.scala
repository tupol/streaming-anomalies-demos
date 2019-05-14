package org.tupol.demo.streaming.anomalies.demos.demo_01

import org.tupol.demo.streaming.anomalies.demos._

object DemoApp extends App {

  def isAnomaly(record: DataRecord, dataState: DataState, confidence: Double = 0.8): Boolean =
    math.abs(record.value - dataState.stats.mean) >= 1.5 * dataState.stats.stdev()

  def signalFunction(x: Double) = 10.0
  def smallNoiseFunction(x: Double) = 10 * math.random
  def trainingValueFunction(x: Double) = signalFunction(x) + smallNoiseFunction(x)
  def largeNoiseFunction(x: Double) = 30 * math.random
  def runtimeValueFunction(x: Double) = signalFunction(x) + largeNoiseFunction(x)

  println("Training Data")
  printHeader
  val trainingDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(trainingValueFunction(x)))
  val trainedState =
    trainingDataStream.tail.foldLeft(InitialState(trainingDataStream.head)) { (state, record) =>
      val result = state |+| record
      println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
      result
    }
  printLine

  println
  println("Runtime Data")
  printHeader
  val runtimeDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(runtimeValueFunction(x)))
  val prediction: DataState = runtimeDataStream.foldLeft(trainedState) { (state, record) =>
    val result = if (isAnomaly(record, state)) state else state |+| record
    println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
    result
  }
  printLine

}
