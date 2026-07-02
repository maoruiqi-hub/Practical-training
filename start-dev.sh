#!/bin/bash
# ============================================================
# 开发环境启动脚本
# 自动检测端口占用，切换到可用端口后再启动前后端
# ============================================================

BACKEND_PORT=${1:-8081}
FRONTEND_PORT=${2:-3000}

# 检查端口是否被占用，是则递增直到找到可用端口
find_free_port() {
  local port=$1
  while netstat -ano 2>/dev/null | grep -q ":$port " | grep -q "LISTENING"; do
    echo "⚠️  端口 $port 已被占用，尝试 $((port + 1))..."
    port=$((port + 1))
  done
  echo $port
}

BACKEND_PORT=$(find_free_port "$BACKEND_PORT")
FRONTEND_PORT=$(find_free_port "$FRONTEND_PORT")

echo "============================================"
echo "  后端端口: $BACKEND_PORT"
echo "  前端端口: $FRONTEND_PORT"
echo "============================================"

# 更新前端代理配置指向后端实际端口
VUE_CONFIG="$(dirname "$0")/frontend/vue.config.js"
if [ -f "$VUE_CONFIG" ]; then
  sed -i "s|http://localhost:[0-9]*|http://localhost:$BACKEND_PORT|g" "$VUE_CONFIG"
  echo "✅ 前端代理已指向 http://localhost:$BACKEND_PORT"
fi

# 启动后端
echo ""
echo ">>> 启动后端 (port $BACKEND_PORT)..."
cd "$(dirname "$0")/backend"
if [ -f ".env" ]; then
  set -a
  . ./.env
  set +a
fi
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$BACKEND_PORT" &
BACKEND_PID=$!

# 启动前端
echo ""
echo ">>> 启动前端 (port $FRONTEND_PORT)..."
cd "$(dirname "$0")/frontend"
npx vue-cli-service serve --port $FRONTEND_PORT &
FRONTEND_PID=$!

echo ""
echo "============================================"
echo "  后端: http://localhost:$BACKEND_PORT/practical-training"
echo "  前端: http://localhost:$FRONTEND_PORT"
echo "============================================"
echo "  Ctrl+C 停止所有服务"
echo "============================================"

# 捕获退出信号，停掉所有子进程
cleanup() {
  echo ""
  echo "正在停止服务..."
  kill $BACKEND_PID 2>/dev/null
  kill $FRONTEND_PID 2>/dev/null
  wait
  echo "已停止。"
}
trap cleanup EXIT INT TERM

wait
