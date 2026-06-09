#!/bin/bash

echo "=========================================="
echo "  居家手工美食烘焙园地 - 项目启动脚本"
echo "=========================================="
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "错误：未找到 .env 配置文件"
    exit 1
fi

set -a
source "$ENV_FILE"
set +a

check_port_occupied() {
    local port=$1
    local pid=$(lsof -nP -iTCP:$port -sTCP:LISTEN -t 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "$pid"
        return 0
    else
        return 1
    fi
}

get_process_info() {
    local pid=$1
    if [ -n "$pid" ]; then
        ps -p $pid -o pid,comm 2>/dev/null | tail -1
    fi
}

wait_for_url() {
    local name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -fsS "$url" >/dev/null 2>&1; then
            echo "✅ $name 已就绪"
            return 0
        fi

        echo "⏳ 等待 $name 就绪 ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ $name 启动超时：$url"
    return 1
}

echo "正在检测端口..."
echo ""

PORTS=(
    "前端:$FRONTEND_PORT"
    "后端:$BACKEND_PORT"
    "MySQL:$MYSQL_PORT"
    "Redis:$REDIS_PORT"
)

HAS_CONFLICT=0

for PORT_INFO in "${PORTS[@]}"; do
    NAME="${PORT_INFO%%:*}"
    PORT="${PORT_INFO##*:}"
    PID=$(check_port_occupied $PORT)
    if [ $? -eq 0 ]; then
        PROCESS=$(get_process_info $PID)
        echo "❌ 端口冲突：$NAME 端口 $PORT 已被占用"
        echo "   占用进程：$PROCESS"
        HAS_CONFLICT=1
    else
        echo "✅ $NAME 端口 $PORT 可用"
    fi
done

echo ""

if [ $HAS_CONFLICT -eq 1 ]; then
    echo "错误：存在端口冲突，请先停止占用端口的进程，或修改 .env 文件中的端口配置"
    exit 1
fi

if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif command -v docker &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    echo "错误：未找到 Docker Compose，请先安装 Docker 和 Docker Compose"
    exit 1
fi

echo "正在构建并启动服务..."
echo ""

cd "$SCRIPT_DIR"
$COMPOSE_CMD up -d --build

BUILD_EXIT=$?

if [ $BUILD_EXIT -ne 0 ]; then
    echo ""
    echo "错误：服务构建启动失败，请检查日志"
    exit $BUILD_EXIT
fi

echo ""
echo "等待服务就绪..."
echo ""

wait_for_url "后端 API" "http://127.0.0.1:$BACKEND_PORT/api/categories" 60 || exit 1
wait_for_url "前端页面" "http://127.0.0.1:$FRONTEND_PORT" 30 || exit 1

echo ""
echo "=========================================="
echo "  🎉  服务启动成功！"
echo "=========================================="
echo ""
echo "🌐 前端访问地址："
echo "   http://localhost:$FRONTEND_PORT"
echo "   http://127.0.0.1:$FRONTEND_PORT"
echo ""
echo "🔧 后端 API 地址："
echo "   http://localhost:$BACKEND_PORT/api"
echo "   http://127.0.0.1:$BACKEND_PORT/api"
echo ""
echo "🗄️  数据库端口：$MYSQL_PORT"
echo "⚡ Redis 端口：$REDIS_PORT"
echo ""
echo "📋 查看日志：$COMPOSE_CMD logs -f"
echo "⏹️  停止服务：$COMPOSE_CMD down"
echo "🔄 重启服务：$COMPOSE_CMD restart"
echo ""
