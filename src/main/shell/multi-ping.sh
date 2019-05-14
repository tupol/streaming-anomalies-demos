#!/usr/bin/env bash

TARGETS="google.com yahoo.com bing.com qwant.com rds.ro upc.ro upc.nl facebook.com twitter.com instagram.com"

#TARGETS="google.com"

for TARGET in $TARGETS
do
#  ./ping-test.sh `hostname` "$TARGET" >> ../../test/resources/test2/ping-test.log &
  ./ping-test.sh `hostname` "$TARGET" | kafka-console-producer.sh --broker-list localhost:9092 --topic input-topic &
done
