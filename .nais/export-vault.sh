#!/usr/bin/env bash

if test -f /var/run/secrets/nais.io/serviceuser/password; then
  export MQ_SERVICE_PASSWORD=$(cat /var/run/secrets/nais.io/srvmotmq/password)
  echo "- exporting MQ_SERVICE_PASSWORD"
fi