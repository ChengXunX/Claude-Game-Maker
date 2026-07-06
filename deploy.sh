#!/bin/bash
# ChengXun Game Maker 部署脚本
# 使用非常用端口避免被扫描封禁

set -e

echo "=========================================="
echo "  ChengXun Game Maker 部署脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置
FRONTEND_PORT=18080
BACKEND_PORT=19922
DEPLOY_DIR="/var/www/game-maker"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="game-maker"
APP_LOG="$PROJECT_DIR/app.log"
APP_PID_FILE="$PROJECT_DIR/app.pid"

# sudo 包装函数：支持 SUDO_PASS 环境变量
_sudo() {
    if [ -n "$SUDO_PASS" ]; then
        echo "$SUDO_PASS" | sudo -S "$@"
    else
        sudo "$@"
    fi
}

echo -e "${YELLOW}部署配置:${NC}"
echo "  前端端口: $FRONTEND_PORT"
echo "  后端端口: $BACKEND_PORT"
echo "  部署目录: $DEPLOY_DIR"
echo "  项目目录: $PROJECT_DIR"
echo ""

# 停止已运行的后端服务
stop_backend() {
    if [ -f "$APP_PID_FILE" ]; then
        OLD_PID=$(cat "$APP_PID_FILE")
        if kill -0 "$OLD_PID" 2>/dev/null; then
            echo -e "${YELLOW}停止已运行的后端服务 (PID: $OLD_PID)...${NC}"
            kill "$OLD_PID"
            sleep 3
            # 强制停止
            if kill -0 "$OLD_PID" 2>/dev/null; then
                kill -9 "$OLD_PID"
            fi
        fi
        rm -f "$APP_PID_FILE"
    fi
    # 也检查端口占用
    if lsof -ti:$BACKEND_PORT > /dev/null 2>&1; then
        echo -e "${YELLOW}端口 $BACKEND_PORT 被占用，正在释放...${NC}"
        lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
}

# 步骤 1: 构建前端
echo -e "${GREEN}[1/6] 构建前端...${NC}"
cd "$PROJECT_DIR/frontend"
npm run build
echo "前端构建完成"
echo ""

# 步骤 2: 部署前端文件
echo -e "${GREEN}[2/6] 部署前端文件...${NC}"
_sudo mkdir -p "$DEPLOY_DIR"
_sudo cp -r "$PROJECT_DIR/frontend/dist/"* "$DEPLOY_DIR/"
_sudo chown -R www-data:www-data "$DEPLOY_DIR"
echo "前端文件已部署到 $DEPLOY_DIR"
echo ""

# 步骤 3: 构建后端
echo -e "${GREEN}[3/6] 构建后端...${NC}"
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
echo "后端构建完成"
echo ""

# 步骤 4: 配置 Nginx
echo -e "${GREEN}[4/6] 配置 Nginx...${NC}"
cat > /tmp/game-maker-nginx.conf << 'NGINX_EOF'
# ChengXun Game Maker Nginx 配置
# 使用非常用端口避免被扫描封禁

# WebSocket 升级映射（仅当客户端请求升级时才升级连接）
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 18080;
    server_name _;

    # 前端静态文件
    root /var/www/game-maker;
    index index.html;

    # 安全头（后端已设置，Nginx 不再重复添加，避免飞书等外部回调解析异常）

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript application/xml+rss application/atom+xml image/svg+xml;

    # 安装页面代理到后端（必须在SPA规则之前）
    location = /install {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # OpenAPI 文档代理（必须在SPA规则之前）
    location /v3/api-docs {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Swagger UI 代理（^~ 前缀优先于正则匹配，防止 .css/.js 被静态资源规则拦截）
    location ^~ /swagger-ui {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 前端路由 - SPA 支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # SSE 流式接口（AI助手等）- 需要长超时和禁用缓冲
    location /api/v1/ai-assistant/stream/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;

        # SSE 必须禁用缓冲，否则事件不会实时推送
        proxy_buffering off;
        proxy_cache off;

        # SSE 长超时（AI 响应可能需要几分钟）
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # SSE 流式接口（项目讨论）- 需要长超时和禁用缓冲
    location /api/project-discussions {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;
        proxy_set_header Authorization $http_authorization;

        # SSE 必须禁用缓冲
        proxy_buffering off;
        proxy_cache off;

        # SSE 长超时
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        # 优先使用上游代理传来的协议头（支持 HTTPS 代理链）
        proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;

        # WebSocket 支持（仅当客户端发送 Upgrade 头时才升级）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Actuator 监控代理
    location /actuator/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 限制访问 IP
        allow 127.0.0.1;
        allow ::1;
        deny all;
    }

    # 飞书 Webhook 代理
    location /feishu/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 钉钉 Webhook 代理
    location /dingtalk/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
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
_sudo cp /tmp/game-maker-nginx.conf /etc/nginx/sites-available/game-maker
rm -f /tmp/game-maker-nginx.conf

# 启用站点
_sudo ln -sf /etc/nginx/sites-available/game-maker /etc/nginx/sites-enabled/

# 测试配置
_sudo nginx -t

# 重新加载
_sudo systemctl reload nginx
echo "Nginx 配置完成"
echo ""

# 步骤 5: 停止旧服务并启动后端
echo -e "${GREEN}[5/6] 启动后端服务...${NC}"
stop_backend

cd "$PROJECT_DIR"

# 以真实用户运行后端（Claude CLI 禁止 root 使用 --dangerously-skip-permissions）
#
# JVM 内存调优（G8 后）：
# - 堆内存 8g：保持不变，应用对象主要在堆
# - Metaspace 512m：原 2g 过大，Spring Boot 典型值 256-512m 足够
# - CodeCache 256m：JIT 编译代码缓存
# - DirectMemory 512m：Netty 直接缓冲区
# - G1GC：大堆友好，暂停时间可控
if [ "$(id -u)" -eq 0 ]; then
    # $SUDO_USER = 执行 sudo 的真实用户；无 sudo 时用当前用户
    DEPLOY_USER="${SUDO_USER:-$(id -un)}"
    nohup su -s /bin/bash "$DEPLOY_USER" -c "
        cd '$PROJECT_DIR' && \
        set -a && source .env && set +a && \
        java -jar \
            -Xms8g -Xmx8g \
            -XX:MaxMetaspaceSize=512m \
            -XX:ReservedCodeCacheSize=256m \
            -XX:MaxDirectMemorySize=512m \
            -XX:+UseG1GC \
            -XX:MaxGCPauseMillis=200 \
            target/game-maker-1.0-SNAPSHOT.jar \
            --spring.profiles.active=prod \
            --server.port=$BACKEND_PORT \
            --server.address=127.0.0.1 \
            > '$APP_LOG' 2>&1
    " &
else
    set -a
    source .env
    set +a
    nohup java -jar \
        -Xms8g -Xmx8g \
        -XX:MaxMetaspaceSize=512m \
        -XX:ReservedCodeCacheSize=256m \
        -XX:MaxDirectMemorySize=512m \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=200 \
        target/game-maker-1.0-SNAPSHOT.jar \
        --spring.profiles.active=prod \
        --server.port=$BACKEND_PORT \
        --server.address=127.0.0.1 \
        > "$APP_LOG" 2>&1 &
fi

echo $! > "$APP_PID_FILE"
NEW_PID=$(cat "$APP_PID_FILE")

echo "后端服务启动中 (PID: $NEW_PID)..."
echo "日志文件: $APP_LOG"

# 等待启动
echo -n "等待服务就绪"
for i in {1..30}; do
    sleep 2
    echo -n "."
    if curl -s http://localhost:$BACKEND_PORT/api/install/status > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}后端服务启动成功！${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo ""
        echo -e "${RED}后端服务启动超时，请检查日志: $APP_LOG${NC}"
    fi
done
echo ""

# 步骤 6: 验证部署
echo -e "${GREEN}[6/6] 验证部署...${NC}"

# 检查 Nginx
if systemctl is-active --quiet nginx; then
    echo -e "  Nginx: ${GREEN}运行中${NC}"
else
    echo -e "  Nginx: ${RED}未运行${NC}"
fi

# 检查后端
if curl -s http://localhost:$BACKEND_PORT/api/install/status > /dev/null 2>&1; then
    echo -e "  后端服务: ${GREEN}运行中${NC}"
else
    echo -e "  后端服务: ${RED}未响应${NC}"
fi

# 检查前端
if curl -s http://localhost:$FRONTEND_PORT/ > /dev/null 2>&1; then
    echo -e "  前端服务: ${GREEN}运行中${NC}"
else
    echo -e "  前端服务: ${RED}未响应${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}部署完成！${NC}"
echo "=========================================="
echo ""
echo -e "${YELLOW}访问地址:${NC} http://YOUR_IP:$FRONTEND_PORT"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志: tail -f $APP_LOG"
echo "  停止服务: kill \$(cat $APP_PID_FILE)"
echo "  重启服务: $PROJECT_DIR/deploy.sh"
echo ""
