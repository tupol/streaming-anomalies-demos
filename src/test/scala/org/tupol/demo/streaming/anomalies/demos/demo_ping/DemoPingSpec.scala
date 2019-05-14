package org.tupol.demo.streaming.anomalies.demos.demo_ping

import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.apache.spark.sql.streaming.Trigger
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Span }
import org.scalatest.{ FunSuite, Matchers }
import org.tupol.spark.SharedSparkSession
import org.tupol.spark.io.FormatType.Kafka
import org.tupol.spark.io.streaming.structured.{ GenericStreamDataSinkConfiguration, KafkaStreamDataSinkConfiguration, KafkaStreamDataSourceConfiguration, KafkaSubscription }
import org.tupol.spark.testing.files.TestTempFilePath1

import scala.io.Source
import scala.util.Try

class DemoPingSpec extends FunSuite
  with Matchers with Eventually
  with SharedSparkSession with TestTempFilePath1 with EmbeddedKafka {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(500, Millis)))
  override val sparkConfig = super.sparkConfig + ("spark.io.compression.codec" -> "snappy")

  implicit val config = EmbeddedKafkaConfig()
  val inputTopic = "inputTopic"
  val outputTopic = "outputTopic"

  val InputConfig = KafkaStreamDataSourceConfiguration(
    s":${config.kafkaPort}",
    KafkaSubscription("subscribe", inputTopic), Some("earliest"))
  val GenericSinkConfig = GenericStreamDataSinkConfiguration(Kafka, Map(), None,
    Some(Trigger.ProcessingTime(100)))

  val RawData = Source.fromFile("src/test/resources/multi-ping-test.log").getLines()

  test("basic") {
    val OutputConfig = KafkaStreamDataSinkConfiguration(
      s":${config.kafkaPort}",
      GenericSinkConfig, Some(outputTopic), Some(testPath1))

    implicit val TestConfig = DemoPingContext(InputConfig, OutputConfig)

    withRunningKafka {

      val sq = DemoPing.streamProcessor
      RawData.take(10).foreach { line =>
        Thread.sleep(200)
        println(s"$line")
        publishStringMessageToKafka(inputTopic, line)
        eventually {
          Try(println(consumeFirstStringMessageFrom(outputTopic)))
        }
      }

      sq.awaitTermination(10000)

    }

  }

}
