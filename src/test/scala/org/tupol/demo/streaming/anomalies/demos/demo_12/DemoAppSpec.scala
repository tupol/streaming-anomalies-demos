package org.tupol.demo.streaming.anomalies.demos.demo_12

import java.sql.Timestamp

import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.streaming.{ GroupStateTimeout, OutputMode, StreamingQuery, Trigger }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ BeforeAndAfter, FunSuite, Matchers }
import org.tupol.demo.streaming.anomalies.demos.timestamp
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.testing.files.{ TestTempFilePath1, TestTempFilePath2 }

import scala.concurrent.Future

class DemoAppSpec extends FunSuite with Matchers with Eventually with SharedSparkSession with BeforeAndAfter
  with TestTempFilePath1 with TestTempFilePath2 {

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

    val inputStream = MemoryStream[DataRecord]

    val data1 = inputStream.toDS
      .map(x => (x, timestamp))
      .groupByKey(_._1.key)
      .flatMapGroupsWithState[DataState, (DataRecord, Timestamp, DataState)](
        OutputMode.Update(),
        GroupStateTimeout.NoTimeout)(DemoApp.stateUpdaterByRecord)

    val data2 = inputStream.toDS
      .map(x => (x, timestamp))
      .flatMap(r => r._1.tags.map(tag => (tag, r._1, r._2)))
      .groupByKey(_._1).mapValues { case (_, record, timestamp) => (record, timestamp) }
      .flatMapGroupsWithState[DataState, (String, DataState)](
        OutputMode.Update(),
        GroupStateTimeout.NoTimeout)(DemoApp.stateUpdaterByKey)

    val output1: StreamingQuery = data1.writeStream
      .queryName("stats_records")
      .option("checkpointLocation", testPath1)
      .format("memory")
      .option("truncate", false)
      .outputMode("update")
      .trigger(Trigger.ProcessingTime(500))
      .start()

    val output2: StreamingQuery = data2.writeStream
      .queryName("stats_tags")
      .option("checkpointLocation", testPath2)
      .format("console")
      .option("truncate", false)
      .outputMode("update")
      .trigger(Trigger.ProcessingTime(500))
      .start()

    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      TestData.foreach { record =>
        Thread.sleep(500)
        inputStream.addData(record)
      }
    }
    output1.awaitTermination(10000)
    output2.awaitTermination(10000)

    //    spark.table("stats_records").orderBy("_2", "_1").show(false)
    //    spark.table("stats_tags").orderBy("_2", "_1").show(false)
  }

}
