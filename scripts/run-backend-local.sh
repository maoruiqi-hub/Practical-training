#!/usr/bin/env bash

# Starts the local database tunnel only for the lifetime of this backend process.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BACKEND_DIR="$ROOT_DIR/backend"
ENV_FILE="$BACKEND_DIR/.env"

read_env_value() {
  local key=$1
  awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$ENV_FILE"
}

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing local configuration file: $ENV_FILE" >&2
  exit 1
fi

TUNNEL_HOST=$(read_env_value DB_TUNNEL_HOST)
TUNNEL_LOCAL_PORT=$(read_env_value DB_TUNNEL_LOCAL_PORT)
TUNNEL_REMOTE_HOST=$(read_env_value DB_TUNNEL_REMOTE_HOST)
TUNNEL_REMOTE_PORT=$(read_env_value DB_TUNNEL_REMOTE_PORT)
JAVA_17_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

if [[ -z "$TUNNEL_HOST" || -z "$TUNNEL_LOCAL_PORT" || -z "$TUNNEL_REMOTE_HOST" || -z "$TUNNEL_REMOTE_PORT" ]]; then
  echo "Set every DB_TUNNEL_* value in $ENV_FILE before starting the local backend." >&2
  exit 1
fi

if ! command -v ssh >/dev/null; then
  echo "ssh is required to start the database tunnel." >&2
  exit 1
fi

if ! command -v nc >/dev/null; then
  echo "nc is required to verify the database tunnel." >&2
  exit 1
fi

if nc -z 127.0.0.1 "$TUNNEL_LOCAL_PORT" >/dev/null 2>&1; then
  echo "Local port $TUNNEL_LOCAL_PORT is already in use; refusing to reuse an unmanaged tunnel." >&2
  exit 1
fi

ssh -N \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -L "127.0.0.1:${TUNNEL_LOCAL_PORT}:${TUNNEL_REMOTE_HOST}:${TUNNEL_REMOTE_PORT}" \
  "$TUNNEL_HOST" &
TUNNEL_PID=$!

cleanup() {
  if kill -0 "$TUNNEL_PID" >/dev/null 2>&1; then
    kill "$TUNNEL_PID" >/dev/null 2>&1 || true
    wait "$TUNNEL_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

for _ in {1..10}; do
  if ! kill -0 "$TUNNEL_PID" >/dev/null 2>&1; then
    wait "$TUNNEL_PID"
    exit 1
  fi
  if nc -z 127.0.0.1 "$TUNNEL_LOCAL_PORT" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! nc -z 127.0.0.1 "$TUNNEL_LOCAL_PORT" >/dev/null 2>&1; then
  echo "Database tunnel did not start on port $TUNNEL_LOCAL_PORT." >&2
  exit 1
fi

cd "$BACKEND_DIR"
JAVA_HOME="$JAVA_17_HOME" PATH="$JAVA_17_HOME/bin:$PATH" \
  mvn -Dkingbase-local spring-boot:run
