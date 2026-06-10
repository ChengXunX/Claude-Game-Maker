#!/bin/bash
# ============================================================
# ChengXun Game Maker - Linux (Debian/Ubuntu) 一键部署脚本
# 功能：自动检测环境、安装依赖、配置数据库/Nginx、构建部署
# 用法：sudo bash deploy/install.sh
# ============================================================

set -e
trap 'echo -e "${RED}[错误] 部署失败，请检查上方日志${NC}"' ERR

# ===== 颜色定义 =====
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ===== sudo 处理 =====
if [ "$(id -u)" -eq 0 ]; then
    SUDO=""
else
    SUDO="sudo"
fi

# ===== 项目路径 =====
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$PROJECT_DIR/frontend"
BACKEND_JAR="$PROJECT_DIR/target/game-maker-1.0-SNAPSHOT.jar"
APP_LOG="$PROJECT_DIR/app.log"
APP_PID_FILE="$PROJECT_DIR/app.pid"
INIT_SQL="$PROJECT_DIR/src/main/resources/db/init.sql"
DEPLOY_WEB_DIR="/var/www/game-maker"

# ===== 默认配置 =====
BACKEND_PORT=19922
FRONTEND_PORT=80
DB_NAME="game_maker"
DB_USER="game_maker"

# ============================================================
# 工具函数
# ============================================================

info()    { echo -e "${GREEN}[信息]${NC} $1"; }
warn()    { echo -e "${YELLOW}[警告]${NC} $1"; }
error()   { echo -e "${RED}[错误]${NC} $1"; }
step()    { echo -e "\n${CYAN}===== [$1/$TOTAL_STEPS] $2 =====${NC}"; }
confirm() {
    echo -e "${YELLOW}$1${NC}"
    read -rp "确认继续？(Y/n): " choice
    case "$choice" in
        [nN]|[nN][oO]) echo "已取消部署"; exit 0 ;;
    esac
}

TOTAL_STEPS=8

# ============================================================
# Step 0: 系统检测
# ============================================================

step 0 "系统环境检测"

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS_ID="$ID"
    OS_VERSION="$VERSION_ID"
    OS_NAME="$PRETTY_NAME"
else
    error "无法检测操作系统，仅支持 Debian/Ubuntu"
    exit 1
fi

case "$OS_ID" in
    debian|ubuntu)
        info "操作系统: $OS_NAME"
        ;;
    *)
        error "不支持的操作系统: $OS_ID ($OS_NAME)"
        error "本脚本仅支持 Debian 和 Ubuntu"
        exit 1
        ;;
esac

# 检测架构
ARCH=$(uname -m)
info "系统架构: $ARCH"

# 检测内存
MEM_MB=$(free -m | awk '/^Mem:/{print $2}')
if [ "$MEM_MB" -lt 1024 ]; then
    warn "系统内存不足 1GB ($MEM_MB MB)，可能导致构建失败"
fi

# ============================================================
# Step 1: 安装基础依赖
# ============================================================

step 1 "安装基础依赖"

# 配置 apt 国内镜像源（Debian/Ubuntu）
configure_apt_mirror() {
    # 检测是否已有国内源
    if grep -qE 'mirrors\.(aliyun|tencent|huawei)' /etc/apt/sources.list /etc/apt/sources.list.d/*.list 2>/dev/null; then
        info "apt 已配置国内镜像源"
        return
    fi

    info "配置 apt 国内镜像源 ..."
    local MIRROR_URL=""
    case "$OS_ID" in
        ubuntu)
            # Ubuntu: 阿里云镜像
            local UBUNTU_CODENAME="${VERSION_CODENAME:-$(lsb_release -cs 2>/dev/null || echo "jammy")}"
            MIRROR_URL="https://mirrors.aliyun.com/ubuntu/"
            $SUDO cp /etc/apt/sources.list /etc/apt/sources.list.bak.$(date +%s)
            cat > /tmp/sources.list << APT_EOF
deb ${MIRROR_URL} ${UBUNTU_CODENAME} main restricted universe multiverse
deb ${MIRROR_URL} ${UBUNTU_CODENAME}-updates main restricted universe multiverse
deb ${MIRROR_URL} ${UBUNTU_CODENAME}-backports main restricted universe multiverse
deb ${MIRROR_URL} ${UBUNTU_CODENAME}-security main restricted universe multiverse
APT_EOF
            $SUDO cp /tmp/sources.list /etc/apt/sources.list
            rm -f /tmp/sources.list
            ;;
        debian)
            # Debian: 阿里云镜像
            local DEBIAN_CODENAME="${VERSION_CODENAME:-$(lsb_release -cs 2>/dev/null || echo "bookworm")}"
            MIRROR_URL="https://mirrors.aliyun.com/debian/"
            $SUDO cp /etc/apt/sources.list /etc/apt/sources.list.bak.$(date +%s)
            cat > /tmp/sources.list << APT_EOF
deb ${MIRROR_URL} ${DEBIAN_CODENAME} main contrib non-free non-free-firmware
deb ${MIRROR_URL} ${DEBIAN_CODENAME}-updates main contrib non-free non-free-firmware
deb ${MIRROR_URL}-security ${DEBIAN_CODENAME}-security main contrib non-free non-free-firmware
APT_EOF
            $SUDO cp /tmp/sources.list /etc/apt/sources.list
            rm -f /tmp/sources.list
            ;;
    esac
    info "apt 国内镜像源配置完成"
}

configure_apt_mirror
$SUDO apt-get update -qq

# 安装基础工具
for pkg in curl wget git lsof gnupg2 ca-certificates apt-transport-https; do
    if ! command -v "$pkg" &>/dev/null && ! dpkg -l "$pkg" &>/dev/null; then
        info "安装 $pkg ..."
        $SUDO apt-get install -y -qq "$pkg"
    fi
done
info "基础工具已就绪"

# ============================================================
# Step 2: 安装 Java 21
# ============================================================

step 2 "检查 Java 环境"

JAVA_OK=false
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\).*/\1/')
    if [ "$JAVA_VER" -ge 21 ] 2>/dev/null; then
        info "Java $JAVA_VER 已安装"
        JAVA_OK=true
    else
        warn "Java 版本过低 ($JAVA_VER)，需要 21+"
    fi
fi

if [ "$JAVA_OK" = false ]; then
    info "安装 OpenJDK 21 ..."
    $SUDO apt-get install -y -qq openjdk-21-jdk
    # 配置 JAVA_HOME
    JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
    if ! grep -q "JAVA_HOME" /etc/environment 2>/dev/null; then
        echo "JAVA_HOME=$JAVA_HOME_PATH" | $SUDO tee -a /etc/environment > /dev/null
    fi
    export JAVA_HOME="$JAVA_HOME_PATH"
    info "Java 21 安装完成"
fi

# ============================================================
# Step 3: 安装 Maven
# ============================================================

step 3 "检查 Maven 环境"

if command -v mvn &>/dev/null; then
    MVN_VER=$(mvn -version 2>&1 | head -1 | grep -oP '[0-9]+\.[0-9]+\.[0-9]+')
    info "Maven $MVN_VER 已安装"
else
    info "安装 Maven ..."
    $SUDO apt-get install -y -qq maven
    info "Maven 安装完成"
fi

# 配置 Maven 阿里云镜像（加速依赖下载）
M2_DIR="$HOME/.m2"
M2_SETTINGS="$M2_DIR/settings.xml"
if [ ! -f "$M2_SETTINGS" ] || ! grep -q "aliyun" "$M2_SETTINGS" 2>/dev/null; then
    info "配置 Maven 阿里云镜像 ..."
    mkdir -p "$M2_DIR"
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
    info "Maven 阿里云镜像已配置"
fi

# ============================================================
# Step 4: 安装 Node.js & npm
# ============================================================

step 4 "检查 Node.js 环境"

NODE_OK=false
if command -v node &>/dev/null; then
    NODE_VER=$(node -v | sed 's/v//' | cut -d. -f1)
    if [ "$NODE_VER" -ge 18 ] 2>/dev/null; then
        info "Node.js $(node -v) 已安装"
        NODE_OK=true
    else
        warn "Node.js 版本过低 ($(node -v))，需要 18+"
    fi
fi

if [ "$NODE_OK" = false ]; then
    info "安装 Node.js 20.x (LTS) ..."
    # 使用 NodeSource 官方脚本
    curl -fsSL https://deb.nodesource.com/setup_20.x | $SUDO bash -
    $SUDO apt-get install -y -qq nodejs
    info "Node.js $(node -v) 安装完成"
fi

# 确保 npm 可用
if ! command -v npm &>/dev/null; then
    $SUDO apt-get install -y -qq npm
fi

# 配置 npm 淘宝镜像（加速包下载）
NPM_REGISTRY=$(npm config get registry 2>/dev/null)
if echo "$NPM_REGISTRY" | grep -qv "npmmirror"; then
    info "配置 npm 淘宝镜像 ..."
    npm config set registry https://registry.npmmirror.com
    info "npm 淘宝镜像已配置"
fi
info "npm $(npm -v) 已就绪（registry: $(npm config get registry)）"

# ============================================================
# Step 5: 安装 MySQL
# ============================================================

step 5 "检查 MySQL 环境"

MYSQL_OK=false
if command -v mysql &>/dev/null; then
    MYSQL_VER=$(mysql --version | grep -oP '[0-9]+\.[0-9]+\.[0-9]+')
    info "MySQL $MYSQL_VER 已安装"
    MYSQL_OK=true
fi

if [ "$MYSQL_OK" = false ]; then
    info "安装 MySQL Server ..."
    $SUDO apt-get install -y -qq mysql-server

    # 确保 MySQL 服务启动
    $SUDO systemctl enable mysql
    $SUDO systemctl start mysql

    info "MySQL 安装完成"
fi

# 确保 MySQL 正在运行
if ! $SUDO systemctl is-active --quiet mysql; then
    info "启动 MySQL 服务 ..."
    $SUDO systemctl start mysql
fi

# ============================================================
# Step 6: 安装 Nginx
# ============================================================

step 6 "检查 Nginx 环境"

if command -v nginx &>/dev/null; then
    info "Nginx $(nginx -v 2>&1 | grep -oP '[0-9]+\.[0-9]+\.[0-9]+') 已安装"
else
    info "安装 Nginx ..."
    $SUDO apt-get install -y -qq nginx
    info "Nginx 安装完成"
fi

# 确保 Nginx 正在运行
$SUDO systemctl enable nginx
if ! $SUDO systemctl is-active --quiet nginx; then
    $SUDO systemctl start nginx
fi

# ============================================================
# Step 7: 用户配置
# ============================================================

step 7 "配置参数"

echo ""
echo -e "${BLUE}请填写以下部署参数：${NC}"
echo ""

# 域名
while true; do
    read -rp "网站域名（如 example.com 或 IP 地址）: " DOMAIN
    if [ -n "$DOMAIN" ]; then
        break
    fi
    warn "域名不能为空"
done

# 前端端口
read -rp "前端访问端口 [默认 80]: " input_port
if [ -n "$input_port" ]; then
    FRONTEND_PORT="$input_port"
fi

# MySQL 密码
echo ""
echo -e "${BLUE}数据库配置：${NC}"
while true; do
    read -rsp "MySQL root 密码（新安装请设置，已有请输入原密码）: " MYSQL_ROOT_PASSWORD
    echo ""
    if [ -n "$MYSQL_ROOT_PASSWORD" ]; then
        break
    fi
    warn "密码不能为空"
done

read -rp "数据库名 [默认 game_maker]: " input_db
if [ -n "$input_db" ]; then
    DB_NAME="$input_db"
fi

# 自动生成 JWT Secret
JWT_SECRET=$(openssl rand -hex 24)

# 生成数据库用户密码
DB_PASSWORD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)

# 显示配置摘要
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  部署配置摘要${NC}"
echo -e "${CYAN}========================================${NC}"
echo -e "  域名:          ${GREEN}$DOMAIN${NC}"
echo -e "  前端端口:      ${GREEN}$FRONTEND_PORT${NC}"
echo -e "  后端端口:      ${GREEN}$BACKEND_PORT${NC} (内部)"
echo -e "  数据库名:      ${GREEN}$DB_NAME${NC}"
echo -e "  项目目录:      ${GREEN}$PROJECT_DIR${NC}"
echo -e "  部署目录:      ${GREEN}$DEPLOY_WEB_DIR${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

confirm "以上配置是否正确？开始部署？"

# ============================================================
# Step 8: 执行部署
# ============================================================

step 8 "执行部署"

# ----- 8.1 配置 MySQL -----
info "配置 MySQL 数据库 ..."

# 设置/更新 root 密码（兼容已有安装）
$SUDO mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';" 2>/dev/null || \
$SUDO mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "SELECT 1;" 2>/dev/null || {
    warn "无法设置 MySQL root 密码，尝试无密码模式 ..."
}

# 创建数据库
$SUDO mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || \
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

# 创建应用用户并授权
$SUDO mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "
    CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
    FLUSH PRIVILEGES;
" 2>/dev/null || \
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "
    CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
    FLUSH PRIVILEGES;
" 2>/dev/null

# 导入初始化 SQL
if [ -f "$INIT_SQL" ]; then
    info "导入数据库初始化脚本 ..."
    mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "${DB_NAME}" < "$INIT_SQL" 2>/dev/null || {
        warn "初始化 SQL 导入可能部分失败（表可能已存在），继续 ..."
    }
fi

info "数据库配置完成"

# ----- 8.2 生成 .env 文件 -----
info "生成环境配置文件 ..."

cat > "$PROJECT_DIR/.env" << EOF
# ChengXun Game Maker 生产环境配置
# 由 install.sh 自动生成于 $(date '+%Y-%m-%d %H:%M:%S')

# ===== Spring Profile =====
SPRING_PROFILES_ACTIVE=prod

# ===== 数据库配置 =====
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=${DB_NAME}
MYSQL_USER=${DB_USER}
MYSQL_PASSWORD=${DB_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}

# ===== Redis 配置 =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ===== Claude API 配置（请手动填写） =====
CLAUDE_API_KEY=
CLAUDE_API_URL=https://api.anthropic.com
CLAUDE_MODEL=claude-sonnet-4-20250514
CLAUDE_CLI_PATH=/usr/bin/claude

# ===== JWT 配置 =====
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000

# ===== 邮件配置（可选） =====
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=

# ===== 飞书配置（可选） =====
FEISHU_WEBHOOK_URL=
FEISHU_WEBHOOK_SECRET=

# ===== 钉钉配置（可选） =====
DINGTALK_WEBHOOK_URL=
DINGTALK_WEBHOOK_SECRET=
EOF

info "环境配置文件已生成: $PROJECT_DIR/.env"

# ----- 8.3 安装前端依赖并构建 -----
info "构建前端 ..."
cd "$FRONTEND_DIR"

if [ ! -d "node_modules" ]; then
    info "安装前端依赖 (首次运行) ..."
    npm install --legacy-peer-deps
fi

npm run build
info "前端构建完成"

# ----- 8.4 构建后端 -----
info "构建后端 ..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
info "后端构建完成"

# ----- 8.5 部署前端 -----
info "部署前端文件 ..."
$SUDO mkdir -p "$DEPLOY_WEB_DIR"
$SUDO rm -rf "$DEPLOY_WEB_DIR"/*
$SUDO cp -r "$FRONTEND_DIR/dist/"* "$DEPLOY_WEB_DIR/"
$SUDO chown -R www-data:www-data "$DEPLOY_WEB_DIR"

# ----- 8.6 配置 Nginx -----
info "配置 Nginx ..."

$SUDO tee /etc/nginx/sites-available/game-maker > /dev/null << NGINX_EOF
# ChengXun Game Maker Nginx 配置
# 由 install.sh 自动生成

server {
    listen ${FRONTEND_PORT};
    server_name ${DOMAIN};

    # 前端静态文件
    root ${DEPLOY_WEB_DIR};
    index index.html;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript application/xml+rss application/atom+xml image/svg+xml;

    # 安装页面代理到后端
    location = /install {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # OpenAPI 文档代理
    location /v3/api-docs {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # Swagger UI 代理
    location ^~ /swagger-ui {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # 前端路由 - SPA 支持
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # SSE 流式接口
    location /api/v1/ai-assistant/stream/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Actuator 监控（仅本地访问）
    location /actuator/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        allow 127.0.0.1;
        allow ::1;
        deny all;
    }

    # 飞书 Webhook
    location /feishu/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    # 钉钉 Webhook
    location /dingtalk/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_read_timeout 86400;
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
}
NGINX_EOF

$SUDO ln -sf /etc/nginx/sites-available/game-maker /etc/nginx/sites-enabled/
$SUDO nginx -t
$SUDO systemctl reload nginx
info "Nginx 配置完成"

# ----- 8.7 停止旧服务 & 启动后端 -----
info "启动后端服务 ..."

# 停止旧进程
if [ -f "$APP_PID_FILE" ]; then
    OLD_PID=$(cat "$APP_PID_FILE")
    kill "$OLD_PID" 2>/dev/null || true
    sleep 2
    kill -9 "$OLD_PID" 2>/dev/null || true
    rm -f "$APP_PID_FILE"
fi
lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true

# 启动后端（以项目所有者用户运行，避免 Claude CLI root 权限问题）
cd "$PROJECT_DIR"
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

echo $! > "$APP_PID_FILE"
NEW_PID=$(cat "$APP_PID_FILE")

info "后端服务启动中 (PID: $NEW_PID) ..."

# 等待启动
echo -n "等待服务就绪"
for i in $(seq 1 40); do
    sleep 2
    echo -n "."
    if curl -s "http://localhost:$BACKEND_PORT/api/install/status" > /dev/null 2>&1; then
        echo ""
        info "后端服务启动成功！"
        break
    fi
    if [ "$i" -eq 40 ]; then
        echo ""
        warn "后端服务启动超时，请检查日志: $APP_LOG"
    fi
done

# ----- 8.8 验证部署 -----
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  部署验证${NC}"
echo -e "${CYAN}========================================${NC}"

# Nginx
if $SUDO systemctl is-active --quiet nginx; then
    echo -e "  Nginx:  ${GREEN}运行中${NC}"
else
    echo -e "  Nginx:  ${RED}未运行${NC}"
fi

# 后端
if curl -s "http://localhost:$BACKEND_PORT/api/install/status" > /dev/null 2>&1; then
    echo -e "  后端:   ${GREEN}运行中${NC}"
else
    echo -e "  后端:   ${RED}未响应${NC}"
fi

# 前端
if curl -s "http://localhost:$FRONTEND_PORT/" > /dev/null 2>&1; then
    echo -e "  前端:   ${GREEN}可访问${NC}"
else
    echo -e "  前端:   ${RED}无法访问${NC}"
fi

# MySQL
if mysql -u "${DB_USER}" -p"${DB_PASSWORD}" -e "USE \`${DB_NAME}\`" 2>/dev/null; then
    echo -e "  数据库: ${GREEN}连接正常${NC}"
else
    echo -e "  数据库: ${RED}连接失败${NC}"
fi

# ============================================================
# 完成
# ============================================================

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

if [ "$FRONTEND_PORT" = "80" ]; then
    echo -e "  访问地址: ${CYAN}http://${DOMAIN}${NC}"
else
    echo -e "  访问地址: ${CYAN}http://${DOMAIN}:${FRONTEND_PORT}${NC}"
fi

echo ""
echo -e "${YELLOW}后续操作：${NC}"
echo -e "  1. 编辑 ${CYAN}$PROJECT_DIR/.env${NC} 填写 Claude API Key 等配置"
echo -e "  2. 重启后端使配置生效: ${CYAN}$PROJECT_DIR/deploy/restart.sh${NC}"
echo ""
echo -e "${YELLOW}常用命令：${NC}"
echo -e "  查看日志:   ${CYAN}tail -f $APP_LOG${NC}"
echo -e "  停止服务:   ${CYAN}kill \$(cat $APP_PID_FILE)${NC}"
echo -e "  重启后端:   ${CYAN}$PROJECT_DIR/deploy/restart.sh${NC}"
echo -e "  重新部署:   ${CYAN}sudo bash $PROJECT_DIR/deploy/install.sh${NC}"
echo ""
