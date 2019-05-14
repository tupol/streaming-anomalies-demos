#!/usr/bin/env bash


function usage {
cat <<EOF
$0 source_id target_host
    source_id     Identifier of the source machine sending this ping
    target_host   The host being pinged
EOF
}

if [[ $# -ne 2 ]];  then usage; exit -1; fi

SOURCE="$1"
TARGET="$2"

# date +%Y-%m-%dT%H:%M:%S.%3N
# date +%Y%m%d%H%M%S%3N

ping -i 0.5 "$TARGET" | while read pong; do echo "$(date +%Y-%m-%dT%H:%M:%S.%3N),"$SOURCE","$TARGET",$pong"; done
