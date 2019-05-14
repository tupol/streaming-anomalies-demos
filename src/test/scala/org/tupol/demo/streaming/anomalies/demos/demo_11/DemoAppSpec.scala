package org.tupol.demo.streaming.anomalies.demos.demo_11

import java.sql.Timestamp

import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.streaming.{ GroupStateTimeout, StreamingQuery, Trigger }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ BeforeAndAfter, FunSuite, Matchers }
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.testing.files.TestTempFilePath1

import scala.concurrent.Future

class DemoAppSpec extends FunSuite with Matchers with Eventually with SharedSparkSession with BeforeAndAfter with TestTempFilePath1 {

  import spark.implicits._

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(20, Seconds)))

  val TestData = Seq(
    DataRecord("Id-01", 1.1, Seq("a", "b")),
    DataRecord("Id-01", 1.3, Seq("a", "b")),
    DataRecord("Id-02", 2.1, Seq("b", "c")),
    DataRecord("Id-02", 2.2, Seq("b", "c")),
    DataRecord("Id-02", 2.3, Seq("b", "c")),
    DataRecord("Id-03", 3.1, Seq("c", "d")),
    DataRecord("Id-03", 3.2, Seq("c", "d")),
    DataRecord("Id-03", 3.3, Seq("c", "d")))

  test("basic") {

    val inputStream = MemoryStream[DataRecord]

    val data = inputStream.toDS //.withWatermark("event_time", "5 seconds")
      .map(x => (x, new Timestamp(new java.util.Date().getTime)))
      .groupByKey(_._1.key)
      .mapGroupsWithState[DataState, (String, Option[DataState])](GroupStateTimeout.NoTimeout)(DemoApp.stateUpdater)

    val output: StreamingQuery = data.writeStream
      .queryName("stats")
      .option("checkpointLocation", testPath1)
      .format("console")
      .option("truncate", false)
      .outputMode("update")
      .trigger(Trigger.ProcessingTime(500))
      .start()

    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      Thread.sleep(1000)
      TestData.foreach { record =>
        Thread.sleep(1000)
        println(s"Add record: $record")
        inputStream.addData(record)
      }
    }
    Thread.sleep(20000)

    output.awaitTermination(20000)

  }

}
