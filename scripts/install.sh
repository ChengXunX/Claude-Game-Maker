#!/bin/bash
# ============================================
# ChengXun Game Maker 一键安装脚本
# ============================================
# 用法: ./scripts/install.sh [--dev|--prod] [--skip-build]
#   --dev        开发环境（使用 H2 内存数据库）
#   --prod       生产环境（使用 MySQL）
#   --skip-build 跳过构建步骤
#
# 安装流程:
#   1. 检查依赖
#   2. 停止旧服务
#   3. 构建前端
#   4. 构建后端
#   5. 启动服务（自动进入安装向导）

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_PORT=${BACKEND_PORT:-19922}
APP_NAME="game-maker"
APP_LOG="$PROJECT_DIR/logs/app.log"
APP_PID_FILE="$PROJECT_DIR/app.pid"

# 参数解析
ENV_MODE="dev"
SKIP_BUILD=false

for arg in "$@"; do
    case $arg in
        --dev) ENV_MODE="dev" ;;
        --prod) ENV_MODE="prod" ;;
        --skip-build) SKIP_BUILD=true ;;
        *) echo -e "${RED}未知参数: $arg${NC}"; exit 1 ;;
    esac
done

# 打印横幅
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════╗"
echo "║       ChengXun Game Maker 一键安装脚本          ║"
echo "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"
echo -e "${YELLOW}环境模式:${NC} $ENV_MODE"
echo -e "${YELLOW}后端端口:${NC} $BACKEND_PORT"
echo -e "${YELLOW}项目目录:${NC} $PROJECT_DIR"
echo ""

# 创建日志目录
mkdir -p "$PROJECT_DIR/logs"

# ===== 步骤 1: 检查依赖 =====
echo -e "${BLUE}[1/5] 检查系统依赖...${NC}"

check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}  ✗ $1 未安装${NC}"
        return 1
    else
        echo -e "${GREEN}  ✓ $1 已安装${NC}"
        return 0
    fi
}

MISSING=false
check_command java || MISSING=true
check_command mvn || MISSING=true
check_command node || MISSING=true
check_command npm || MISSING=true

if [ "$MISSING" = true ]; then
    echo -e "${RED}缺少必要依赖，请先安装后重试${NC}"
    exit 1
fi

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Java 版本过低，需要 17+，当前: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}  ✓ Java 版本: $JAVA_VERSION${NC}"
echo ""

# ===== 步骤 2: 停止旧服务 =====
echo -e "${BLUE}[2/5] 停止旧服务...${NC}"

if [ -f "$APP_PID_FILE" ]; then
    OLD_PID=$(cat "$APP_PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo -e "${YELLOW}  停止后端服务 (PID: $OLD_PID)...${NC}"
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
        kill -9 "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$APP_PID_FILE"
fi

if lsof -ti:$BACKEND_PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}  释放端口 $BACKEND_PORT...${NC}"
    lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true
fi
echo -e "${GREEN}  ✓ 旧服务已停止${NC}"
echo ""

# ===== 步骤 3: 构建前端 =====
if [ "$SKIP_BUILD" = false ]; then
    echo -e "${BLUE}[3/5] 构建前端...${NC}"
    cd "$PROJECT_DIR/frontend"
    npm install --legacy-peer-deps
    npm run build
    echo -e "${GREEN}  ✓ 前端构建完成${NC}"
else
    echo -e "${BLUE}[3/5] 跳过前端构建${NC}"
fi
echo ""

# ===== 步骤 4: 构建后端 =====
if [ "$SKIP_BUILD" = false ]; then
    echo -e "${BLUE}[4/5] 构建后端...${NC}"
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    echo -e "${GREEN}  ✓ 后端构建完成${NC}"
else
    echo -e "${BLUE}[4/5] 跳过后端构建${NC}"
fi
echo ""

# ===== 步骤 5: 启动服务 =====
echo -e "${BLUE}[5/5] 启动服务...${NC}"
cd "$PROJECT_DIR"

if [ "$ENV_MODE" = "prod" ]; then
    nohup java -jar -Xms512m -Xmx2g target/game-maker-1.0-SNAPSHOT.jar \
        --spring.profiles.active=prod \
        --server.port=$BACKEND_PORT \
        > "$APP_LOG" 2>&1 &
else
    nohup java -jar -Xms512m -Xmx2g target/game-maker-1.0-SNAPSHOT.jar \
        --spring.profiles.active=dev \
        --server.port=$BACKEND_PORT \
        > "$APP_LOG" 2>&1 &
fi

echo $! > "$APP_PID_FILE"
NEW_PID=$(cat "$APP_PID_FILE")
echo -e "${GREEN}  ✓ 服务启动 (PID: $NEW_PID)${NC}"

# 等待启动
echo -n "  等待服务就绪"
for i in {1..30}; do
    sleep 2
    echo -n "."
    if curl -s "http://localhost:$BACKEND_PORT/api/install/status" > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}  ✓ 服务就绪！${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo ""
        echo -e "${RED}  ✗ 服务启动超时，请检查日志: $APP_LOG${NC}"
        exit 1
    fi
done
echo ""

# ===== 安装完成 =====
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════╗"
echo "║              安装完成！                          ║"
echo "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"

# 检查安装状态
INSTALL_STATUS=$(curl -s "http://localhost:$BACKEND_PORT/api/install/status" 2>/dev/null)
IS_INSTALLED=$(echo "$INSTALL_STATUS" | grep -o '"installed":true' || true)

if [ -n "$IS_INSTALLED" ]; then
    echo -e "${GREEN}系统已安装，可以直接访问${NC}"
    echo -e "${GREEN}访问地址:${NC} http://localhost:$BACKEND_PORT"
else
    echo -e "${YELLOW}系统未安装，请访问以下地址完成安装向导:${NC}"
    echo -e "${CYAN}http://localhost:$BACKEND_PORT${NC}"
fi
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志: tail -f $APP_LOG"
echo "  停止服务: kill \$(cat $APP_PID_FILE)"
echo "  卸载程序: ./scripts/uninstall.sh"
echo ""
