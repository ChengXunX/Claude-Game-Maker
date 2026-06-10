#!/bin/bash
# ChengXun Game Maker - 快速更新部署脚本
# 适用于环境已就绪的后续部署（跳过环境安装）
# 用法: bash deploy/update.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# sudo 处理
if [ "$(id -u)" -eq 0 ]; then
    SUDO=""
else
    SUDO="sudo"
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$PROJECT_DIR/frontend"
BACKEND_PORT=19922
FRONTEND_PORT=80
DEPLOY_WEB_DIR="/var/www/game-maker"
APP_LOG="$PROJECT_DIR/app.log"
APP_PID_FILE="$PROJECT_DIR/app.pid"
BACKEND_JAR="$PROJECT_DIR/target/game-maker-1.0-SNAPSHOT.jar"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  ChengXun Game Maker 更新部署${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# 加载 .env
if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
fi

# 确保 npm 配置了国内镜像
NPM_REGISTRY=$(npm config get registry 2>/dev/null)
if echo "$NPM_REGISTRY" | grep -qv "npmmirror"; then
    echo -e "${YELLOW}配置 npm 淘宝镜像 ...${NC}"
    npm config set registry https://registry.npmmirror.com
fi

# [1/5] 构建前端
echo -e "${GREEN}[1/5] 构建前端 ...${NC}"
cd "$FRONTEND_DIR"
npm run build
echo -e "${GREEN}前端构建完成${NC}"

# [2/5] 部署前端
echo -e "${GREEN}[2/5] 部署前端文件 ...${NC}"
$SUDO mkdir -p "$DEPLOY_WEB_DIR"
$SUDO rm -rf "$DEPLOY_WEB_DIR"/*
$SUDO cp -r "$FRONTEND_DIR/dist/"* "$DEPLOY_WEB_DIR/"
$SUDO chown -R www-data:www-data "$DEPLOY_WEB_DIR"

# 确保 Maven 配置了阿里云镜像
M2_SETTINGS="$HOME/.m2/settings.xml"
if [ ! -f "$M2_SETTINGS" ] || ! grep -q "aliyun" "$M2_SETTINGS" 2>/dev/null; then
    echo -e "${YELLOW}配置 Maven 阿里云镜像 ...${NC}"
    mkdir -p "$HOME/.m2"
    cat > "$M2_SETTINGS" << 'MVN_SETTINGS'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Alibaba Cloud Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
MVN_SETTINGS
fi

# [3/5] 构建后端
echo -e "${GREEN}[3/5] 构建后端 ...${NC}"
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
echo -e "${GREEN}后端构建完成${NC}"

# [4/5] 重启后端
echo -e "${GREEN}[4/5] 重启后端服务 ...${NC}"

# 停止旧进程
if [ -f "$APP_PID_FILE" ]; then
    OLD_PID=$(cat "$APP_PID_FILE")
    kill "$OLD_PID" 2>/dev/null || true
    sleep 2
    kill -9 "$OLD_PID" 2>/dev/null || true
    rm -f "$APP_PID_FILE"
fi
lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true
sleep 1

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
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo ""
        echo -e "${RED}启动超时，请检查日志: $APP_LOG${NC}"
        exit 1
    fi
done

# [5/5] Reload Nginx
echo -e "${GREEN}[5/5] 重载 Nginx ...${NC}"
$SUDO nginx -t && $SUDO systemctl reload nginx

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  更新部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
