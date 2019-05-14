package org.tupol.demo.streaming.anomalies.demos.demo_13

import java.sql.Timestamp

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.streaming.{ GroupStateTimeout, OutputMode, StreamingQuery, Trigger }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ BeforeAndAfter, FunSuite, Matchers }
import org.tupol.demo.streaming.anomalies.demos._
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.testing.files.TestTempFilePath1

import scala.concurrent.Future

class DemoAppSpec extends FunSuite with Matchers with Eventually with SharedSparkSession with BeforeAndAfter
  with TestTempFilePath1 {

  import spark.implicits._

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(20, Seconds)))

  val TestData = Seq(
    DataRecord("Id-01", 1.1, Seq("a", "b")),
    DataRecord("Id-01", 1.2, Seq("a", "b")),
    DataRecord("Id-02", 2.1, Seq("b", "c")),
    DataRecord("Id-02", 2.2, Seq("b", "c")),
    DataRecord("Id-02", 2.3, Seq("b", "c")),
    DataRecord("Id-03", 3.1, Seq("c", "d")),
    DataRecord("Id-03", 3.2, Seq("c", "d")),
    DataRecord("Id-03", 3.3, Seq("c", "d")))

  test("basic") {

    val outputMode = OutputMode.Append()

    val inputStream = MemoryStream[DataRecord]

    val data1 = inputStream.toDS
      .map(x => (x, timestamp))
      //      .withWatermark("_2", "2 second").groupBy(window(col("_2"), "1 second", "1 second"), "_1")
      .groupByKey(_._1.key)
      .flatMapGroupsWithState[DataState, (DataRecord, Timestamp, DataState)](
        outputMode,
        GroupStateTimeout.ProcessingTimeTimeout)(DemoApp.stateUpdaterByRecord)

    val data2 = inputStream.toDS
      .map(x => (x, timestamp))
      //      .withWatermark("_2", "500 millisecond")
      .flatMap(r => r._1.tags.map(tag => (tag, r._1, r._2)))
      .groupByKey(_._1).mapValues { case (_, record, timestamp) => (record, timestamp) }
      .flatMapGroupsWithState[DataState, (String, DataState)](
        outputMode,
        GroupStateTimeout.ProcessingTimeTimeout)(DemoApp.stateUpdaterByKey)

    val data11 = data1.flatMap { case (record, ts, state) => record.tags.map(tag => (tag, record, ts, state)) }
    val data22: Dataset[(Timestamp, DataRecord, DataState, (String, DataState))] = data11.joinWith(data2, data11("_1") === data2("_1"))
      .map(r => (r._1._3, r._1._2, r._1._4, (r._2._1, r._2._2)))
    val data = data22

    val output: StreamingQuery = data.writeStream
      .queryName("stats_all")
      .option("checkpointLocation", testPath1)
      .format("memory")
      .option("truncate", false)
      .outputMode(outputMode)
      .trigger(Trigger.ProcessingTime(400))
      .start()

    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      Thread.sleep(1000)
      TestData.foreach { record =>
        Thread.sleep(2000)
        inputStream.addData(record)
        //        println(s"${new Date()}: $record")
      }
    }

    output.awaitTermination(40000)

    spark.table("stats_all").orderBy("_2", "_1").show(false)
  }

}
