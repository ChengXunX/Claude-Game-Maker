#!/bin/bash
# ============================================
# ChengXun Game Maker 卸载脚本
# ============================================
# 清除安装状态，使系统重启后进入安装向导
#
# 用法: ./scripts/uninstall.sh [--force] [--keep-db]
#   --force    跳过确认提示
#   --keep-db  保留数据库（只清除安装标记和数据文件）

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
APP_PID_FILE="$PROJECT_DIR/app.pid"
DATA_DIR="$PROJECT_DIR/data"
LOGS_DIR="$PROJECT_DIR/logs"
SQL_DIR="$PROJECT_DIR/sql"

# 数据库配置（从环境变量或默认值）
MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_DB=${MYSQL_DB:-game_maker}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-}

# 参数解析
FORCE=false
KEEP_DB=false

for arg in "$@"; do
    case $arg in
        --force) FORCE=true ;;
        --keep-db) KEEP_DB=true ;;
        *) echo -e "${RED}未知参数: $arg${NC}"; exit 1 ;;
    esac
done

# 打印横幅
echo -e "${RED}"
echo "╔══════════════════════════════════════════════════╗"
echo "║       ChengXun Game Maker 卸载脚本              ║"
echo "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"

# 确认卸载
if [ "$FORCE" = false ]; then
    echo -e "${YELLOW}此操作将清除以下内容:${NC}"
    echo "  1. 后端服务进程"
    echo "  2. 安装标记文件 (data/.installed)"
    echo "  3. 运行时数据目录 (data/)"
    echo "  4. 日志文件 (logs/)"
    if [ "$KEEP_DB" = false ]; then
        echo -e "  5. ${RED}数据库 ($MYSQL_DB) — 所有表和数据${NC}"
    else
        echo -e "  5. ${YELLOW}数据库 — 保留${NC}"
    fi
    echo ""
    echo -e "${CYAN}清除后重启服务将重新进入安装向导${NC}"
    echo ""
    read -p "确定要继续卸载吗？(y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "取消卸载"
        exit 0
    fi
fi

echo ""

# ===== 步骤 1: 停止后端服务 =====
echo -e "${BLUE}[1/5] 停止后端服务...${NC}"

if [ -f "$APP_PID_FILE" ]; then
    OLD_PID=$(cat "$APP_PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo -e "${YELLOW}  停止进程 (PID: $OLD_PID)...${NC}"
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
        kill -9 "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$APP_PID_FILE"
fi

# 检查端口占用
if lsof -ti:$BACKEND_PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}  释放端口 $BACKEND_PORT...${NC}"
    lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true
    sleep 1
fi

echo -e "${GREEN}  ✓ 后端服务已停止${NC}"
echo ""

# ===== 步骤 2: 删除安装标记 =====
echo -e "${BLUE}[2/5] 删除安装标记...${NC}"

INSTALL_MARKER="$DATA_DIR/.installed"
if [ -f "$INSTALL_MARKER" ]; then
    rm -f "$INSTALL_MARKER"
    echo -e "${GREEN}  ✓ 已删除: $INSTALL_MARKER${NC}"
else
    echo -e "${YELLOW}  安装标记不存在，跳过${NC}"
fi
echo ""

# ===== 步骤 3: 清理数据目录 =====
echo -e "${BLUE}[3/5] 清理数据目录...${NC}"

if [ -d "$DATA_DIR" ]; then
    # 保留目录结构，删除内容
    rm -rf "$DATA_DIR"/*
    # 确保目录存在
    mkdir -p "$DATA_DIR"
    echo -e "${GREEN}  ✓ 已清理: $DATA_DIR/${NC}"
else
    mkdir -p "$DATA_DIR"
    echo -e "${YELLOW}  数据目录不存在，已创建${NC}"
fi
echo ""

# ===== 步骤 4: 清理日志 =====
echo -e "${BLUE}[4/5] 清理日志文件...${NC}"

if [ -d "$LOGS_DIR" ]; then
    rm -rf "$LOGS_DIR"/*
    echo -e "${GREEN}  ✓ 已清理: $LOGS_DIR/${NC}"
fi

# 清理项目根目录下的日志文件
rm -f "$PROJECT_DIR"/*.log 2>/dev/null || true
echo -e "${GREEN}  ✓ 日志已清理${NC}"
echo ""

# ===== 步骤 5: 清理数据库 =====
echo -e "${BLUE}[5/5] 清理数据库...${NC}"

if [ "$KEEP_DB" = false ]; then
    # 检查 MySQL 连接
    if command -v mysql &> /dev/null; then
        if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1" > /dev/null 2>&1; then
            # 检查数据库是否存在
            DB_EXISTS=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
                -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$MYSQL_DB'" \
                -s -N 2>/dev/null)

            if [ -n "$DB_EXISTS" ]; then
                echo -e "${YELLOW}  删除数据库: $MYSQL_DB${NC}"
                mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
                    -e "DROP DATABASE IF EXISTS $MYSQL_DB;"
                echo -e "${GREEN}  ✓ 数据库已删除${NC}"

                # 重新创建空数据库（安装向导需要）
                mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
                    -e "CREATE DATABASE IF NOT EXISTS $MYSQL_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
                echo -e "${GREEN}  ✓ 已创建空数据库: $MYSQL_DB${NC}"
            else
                echo -e "${YELLOW}  数据库不存在，跳过${NC}"
            fi
        else
            echo -e "${YELLOW}  无法连接 MySQL，跳过数据库清理${NC}"
            echo -e "${YELLOW}  请手动清理数据库:${NC}"
            echo "    mysql -u root -p -e \"DROP DATABASE IF EXISTS $MYSQL_DB; CREATE DATABASE $MYSQL_DB;\""
        fi
    else
        echo -e "${YELLOW}  mysql 客户端未安装，跳过数据库清理${NC}"
        echo -e "${YELLOW}  请手动清理数据库${NC}"
    fi
else
    echo -e "${YELLOW}  保留数据库（--keep-db）${NC}"
    echo -e "${YELLOW}  注意: 安装向导可能需要手动清理旧表${NC}"
fi
echo ""

# ===== 清理构建产物（可选） =====
echo -e "${BLUE}[附加] 清理构建产物...${NC}"

if [ -d "$PROJECT_DIR/target" ]; then
    rm -rf "$PROJECT_DIR/target"
    echo -e "${GREEN}  ✓ 已删除: target/${NC}"
fi

if [ -d "$PROJECT_DIR/frontend/dist" ]; then
    rm -rf "$PROJECT_DIR/frontend/dist"
    echo -e "${GREEN}  ✓ 已删除: frontend/dist/${NC}"
fi
echo ""

# ===== 卸载完成 =====
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════╗"
echo "║              卸载完成！                          ║"
echo "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${GREEN}已清理:${NC}"
echo "  ✓ 后端服务进程"
echo "  ✓ 安装标记 (data/.installed)"
echo "  ✓ 运行时数据 (data/*)"
echo "  ✓ 日志文件"
if [ "$KEEP_DB" = false ]; then
    echo "  ✓ 数据库（已重建空库）"
fi
echo ""
echo -e "${CYAN}下次启动服务时将自动进入安装向导${NC}"
echo ""
echo -e "${YELLOW}重新安装:${NC}"
echo "  1. 构建: mvn clean package -DskipTests"
echo "  2. 启动: java -jar target/game-maker-1.0-SNAPSHOT.jar"
echo "  3. 访问: http://localhost:19922 进入安装向导"
echo ""
echo -e "${YELLOW}或使用一键重装:${NC} ./scripts/reinstall.sh"
echo ""
