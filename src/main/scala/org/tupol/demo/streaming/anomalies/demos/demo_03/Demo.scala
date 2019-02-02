package org.tupol.demo.streaming.anomalies.demos.demo_03

object DemoApp extends App {

  def isAnomaly(record: DataRecord, dataState: DataState, confidence: Double = 0.99): Boolean =
    dataState.probability3S(record.value) <= 1.0 - confidence

  def signalFunction(x: Double) = if(x % 100 < 35) 0.0 else if(x % 100 >= 35 && x % 100 < 70) 10.0 else 0.0
  def smallNoiseFunction(x: Double) = 2 * ( 0.5 - math.random )
  def trainingValueFunction(x: Double) = signalFunction(x) + smallNoiseFunction(x)
  def largeNoiseFunction(x: Double) = 2 * smallNoiseFunction(x)
  def runtimeValueFunction(x: Double) = signalFunction(x) + largeNoiseFunction(x)

  println("Training Data")
  printHeader
  val trainingDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(trainingValueFunction(x)))
  val trainedState =
    trainingDataStream.foldLeft(InitialState(trainingDataStream.head)) { (state, record) =>
      val result = state |+| record
      println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.ewStats.mean}%10.3f | ${result.ewStats.stdev()}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
      result
    }
  printLine

  println
  println("Runtime Data")
  printHeader
  val runtimeDataStream: Seq[DataRecord] = Stream.range(0, 100, 1).map(x => DataRecord(runtimeValueFunction(x)))
  val prediction: DataState = runtimeDataStream.foldLeft(trainedState) { (state, record) =>
    val result = stateUpdaterWithCorrectionForAnomalies(state, record)
    println(f"| ${isAnomaly(record, state)}%5s | ${state.probability3S(record.value)}%10.4E | ${record.value}%10.3f | ${result.ewStats.mean}%10.3f | ${result.ewStats.stdev()}%10.3f | ${result.stats.mean}%10.3f | ${result.stats.stdev()}%10.3f |")
    result
  }
  printLine

  /** Allow the state to be corrected even when anomalies are present, by computing the stats as if it would be just on the edge */
  def stateUpdaterWithCorrectionForAnomalies(state: DataState, record: DataRecord): DataState =
    if (isAnomaly(record, state)) {
      val correctionValue = state.ewStats.avg + 3 * state.ewStats.stdev() * math.signum(record.value - state.ewStats.avg)
      val correctedState = state |+| DataRecord(correctionValue)
      correctedState.copy(previousRecord = Some(record))
    } else state |+| record

}
