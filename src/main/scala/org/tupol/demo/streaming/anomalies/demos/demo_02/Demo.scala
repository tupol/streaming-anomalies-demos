package org.tupol.demo.streaming.anomalies.demos.demo_02

import org.tupol.demo.streaming.anomalies.demos._

object DemoApp extends App {

  def isAnomaly(record: DataRecord, dataState: DataState, confidence: Double = 0.99): Boolean =
      dataState.probability3S(record.value) <= 1.0 - confidence

  println("Training Data")
  printHeader
  val trainingDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord( 10 * math.sin(x*math.Pi/100) +  2 * (0.5 - math.random) ))
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
  val runtimeDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord( 10 * math.sin(math.Pi + x*math.Pi/100) + 6 * (0.5 - math.random) ))
  val prediction: DataState = runtimeDataStream.foldLeft(trainedState){ (state, record) =>
    val result = stateUpdaterWithCorrectionForAnomalies(state, record)
    println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.valueStats.mean}%10.3f | ${result.valueStats.stdev()}%10.3f |")
    result
  }
  printLine


  /** Allow the state to be corrected even when anomalies are present, by computing the stats as if it would be just on the edge */
  def stateUpdaterWithCorrectionForAnomalies(state: DataState, record: DataRecord): DataState =
    if(isAnomaly(record, state)) {
      val correctedState = state |+| DataRecord(state.valueStats.avg + 3 * state.valueStats.stdev() * math.signum(record.value - state.valueStats.avg))
      correctedState.copy(previousRecord = Some(record))
    } else state |+| record

}
