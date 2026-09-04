#!/usr/bin/env bash

# 本机开发后端唯一推荐启动方式：
#   bash scripts/run-backend-local.sh
#
# 作用：通过 SSH 隧道连接服务器 Kingbase，再启动 Spring Boot。
# DB_URL 中的 127.0.0.1:54321 是本机隧道入口，不是本机数据库。
# 隧道只在后端进程运行期间存在，后端退出时自动关闭。
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BACKEND_DIR="$ROOT_DIR/backend"
ENV_FILE="$BACKEND_DIR/.env"

read_env_value() {
  local key=$1
  awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$ENV_FILE"
}

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[数据库] 找不到配置文件：$ENV_FILE" >&2
  echo "[数据库] 请先执行：cp backend/.env.example backend/.env" >&2
  exit 1
fi

TUNNEL_HOST=$(read_env_value DB_TUNNEL_HOST)
TUNNEL_LOCAL_PORT=$(read_env_value DB_TUNNEL_LOCAL_PORT)
TUNNEL_REMOTE_HOST=$(read_env_value DB_TUNNEL_REMOTE_HOST)
TUNNEL_REMOTE_PORT=$(read_env_value DB_TUNNEL_REMOTE_PORT)
JAVA_17_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

if [[ -z "$TUNNEL_HOST" || -z "$TUNNEL_LOCAL_PORT" || -z "$TUNNEL_REMOTE_HOST" || -z "$TUNNEL_REMOTE_PORT" ]]; then
  echo "[数据库] backend/.env 缺少 DB_TUNNEL_* 配置，无法建立服务器隧道。" >&2
  exit 1
fi

if ! command -v ssh >/dev/null; then
  echo "[数据库] 未找到 ssh，无法建立服务器隧道。" >&2
  exit 1
fi

if ! command -v nc >/dev/null; then
  echo "[数据库] 未找到 nc，无法检查隧道端口。" >&2
  exit 1
fi

if nc -z 127.0.0.1 "$TUNNEL_LOCAL_PORT" >/dev/null 2>&1; then
  echo "[数据库] 本机端口 $TUNNEL_LOCAL_PORT 已被占用，拒绝复用未知隧道。" >&2
  exit 1
fi

echo "[数据库] 正在建立 SSH 隧道：本机 $TUNNEL_LOCAL_PORT -> 服务器数据库端口 $TUNNEL_REMOTE_PORT"
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
  echo "[数据库] SSH 隧道未能在本机端口 $TUNNEL_LOCAL_PORT 建立。" >&2
  exit 1
fi

echo "[数据库] SSH 隧道已建立，正在启动 Kingbase 后端。"
cd "$BACKEND_DIR"
JAVA_HOME="$JAVA_17_HOME" PATH="$JAVA_17_HOME/bin:$PATH" \
  mvn -Dkingbase-local spring-boot:run
