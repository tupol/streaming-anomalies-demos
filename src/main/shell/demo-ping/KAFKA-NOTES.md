# Kafka Notes

## Setting Up Kafka 0.10.0.1

- https://archive.apache.org/dist/kafka/0.10.0.1/kafka_2.11-0.10.0.1.tgz


## Setting up Zookeeper

- https://archive.apache.org/dist/zookeeper/zookeeper-3.4.8/zookeeper-3.4.8.tar.gz 

## Start Zookeeper

```
$ZOOKEEPER_HOME/zookeeper-server-start.sh $KAFKA_HOME/config/zookeeper.properties
```

## Start Kafka

```
$KAFKA_HOME/kafka-server-start.sh $KAFKA_HOME/config/server.properties
```

Check the [Kafka notes](../KAFKA-NOTES.md)
```console
kafka-topics.sh --zookeeper localhost:2181 --create --topic input-topic  --partitions 1 --replication-factor 1

kafka-console-consumer.sh --zookeeper localhost:2181 --bootstrap-server localhost:9092 --topic input-topic

kafka-console-consumer.sh --zookeeper localhost:2181 --bootstrap-server localhost:9092 --topic output-topic


kafka-console-producer.sh --broker-list localhost:9092 --topic input-topic

cat ../../test/resources/ping-test-kmkz.log | while read line; do echo $line; sleep 1; done | kafka-console-producer.sh --broker-list localhost:9092 --topic input-topic

```
