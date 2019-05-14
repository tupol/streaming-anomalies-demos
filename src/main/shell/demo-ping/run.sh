
APPLICATION_CONF_DIR="application.conf"
JAR="../../../../target/scala-2.11/streaming-anomalies-demos-assembly.jar"

###############################################################################
## Spark Submit Section                                                      ##
###############################################################################

spark-submit  -v  \
--master local[*] \
--deploy-mode client \
--class org.tupol.demo.streaming.anomalies.demos.demo_ping.DemoPing \
--name DemoPing \
--conf spark.yarn.submit.waitAppCompletion=true \
--queue default \
--files "$APPLICATION_CONF" \
--packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.3.2 \
$JAR
