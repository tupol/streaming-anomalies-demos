package org.tupol.demo.streaming.anomalies.demos.demo_01

import org.tupol.demo.streaming.anomalies.demos._

object DemoApp extends App {

  def isAnomaly(record: DataRecord, dataState: DataState, confidence: Double = 0.8): Boolean =
      math.abs(record.value - dataState.valueStats.mean) >= 1.5 * dataState.valueStats.stdev()

  println("Training Data")
  printHeader
  val trainingDataStream: Seq[DataRecord] = Stream.fill(100)(DataRecord( 10 + 10 * math.random ))
  val trainedState =
    trainingDataStream.tail.foldLeft(InitialState(trainingDataStream.head)){(state, record) =>
      val result = state |+| record
      println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.valueStats.mean}%10.3f | ${result.valueStats.stdev()}%10.3f |")
      result
    }
  printLine

  println
  println("Runtime Data")
  printHeader
  val runtimeDataStream: Seq[DataRecord] = Stream.fill(100)(DataRecord( 30 * math.random ))
  val prediction: DataState = runtimeDataStream.foldLeft(trainedState){ (state, record) =>
    val result = if(isAnomaly(record, state)) state else state |+| record
    println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.valueStats.mean}%10.3f | ${result.valueStats.stdev()}%10.3f |")
    result
  }
  printLine

}
