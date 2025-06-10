#!/bin/bash
# wait-for-keycloak.sh

set -e

host="$1"
port="$2"
realm="$3"
shift 3
cmd="$@"

until curl -f "http://$host:$port/realms/$realm/.well-known/openid-configuration" >/dev/null 2>&1; do
  echo "Waiting for Keycloak realm '$realm' to be ready at $host:$port..."
  sleep 5
done

echo "Keycloak realm '$realm' is ready!"
exec $cmd
