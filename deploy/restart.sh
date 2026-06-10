#!/bin/bash
# ChengXun Game Maker - 后端重启脚本
# 用法: bash deploy/restart.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_PORT=19922
APP_LOG="$PROJECT_DIR/app.log"
APP_PID_FILE="$PROJECT_DIR/app.pid"
BACKEND_JAR="$PROJECT_DIR/target/game-maker-1.0-SNAPSHOT.jar"

echo -e "${YELLOW}重启后端服务 ...${NC}"

# 停止旧进程
if [ -f "$APP_PID_FILE" ]; then
    OLD_PID=$(cat "$APP_PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo -e "停止旧进程 (PID: $OLD_PID) ..."
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
        kill -9 "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$APP_PID_FILE"
fi

# 检查端口占用
if lsof -ti:$BACKEND_PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}端口 $BACKEND_PORT 被占用，正在释放 ...${NC}"
    lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true
    sleep 1
fi

# 加载环境变量
cd "$PROJECT_DIR"
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# 检查 jar 包
if [ ! -f "$BACKEND_JAR" ]; then
    echo -e "${YELLOW}jar 包不存在，开始构建 ...${NC}"
    mvn clean package -DskipTests -q
fi

# 以真实用户运行（Claude CLI 禁止 root 使用 --dangerously-skip-permissions）
if [ "$(id -u)" -eq 0 ]; then
    DEPLOY_USER="${SUDO_USER:-$(id -un)}"
    nohup su -s /bin/bash "$DEPLOY_USER" -c "
        cd '$PROJECT_DIR' && \
        set -a && source .env && set +a && \
        java -jar -Xms256m -Xmx512m '$BACKEND_JAR' \
            --spring.profiles.active=prod \
            --server.port=$BACKEND_PORT \
            --server.address=127.0.0.1 \
            > '$APP_LOG' 2>&1
    " &
else
    nohup java -jar -Xms256m -Xmx512m "$BACKEND_JAR" \
        --spring.profiles.active=prod \
        --server.port=$BACKEND_PORT \
        --server.address=127.0.0.1 \
        > "$APP_LOG" 2>&1 &
fi

echo $! > "$APP_PID_FILE"
NEW_PID=$(cat "$APP_PID_FILE")

echo -e "后端服务启动中 (PID: $NEW_PID) ..."
echo -n "等待服务就绪"
for i in $(seq 1 30); do
    sleep 2
    echo -n "."
    if curl -s "http://localhost:$BACKEND_PORT/api/install/status" > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}后端服务启动成功！${NC}"
        exit 0
    fi
done

echo ""
echo -e "${RED}启动超时，请检查日志: $APP_LOG${NC}"
exit 1
