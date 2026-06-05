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
sudo mkdir -p "$DEPLOY_DIR"
sudo cp -r "$PROJECT_DIR/frontend/dist/"* "$DEPLOY_DIR/"
sudo chown -R www-data:www-data "$DEPLOY_DIR"
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
sudo tee /etc/nginx/sites-available/game-maker > /dev/null << 'NGINX_EOF'
# ChengXun Game Maker Nginx 配置
# 使用非常用端口避免被扫描封禁

server {
    listen 18080;
    server_name _;

    # 前端静态文件
    root /var/www/game-maker;
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

    # 安装页面代理到后端（必须在SPA规则之前）
    location = /install {
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

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

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

# 启用站点
sudo ln -sf /etc/nginx/sites-available/game-maker /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重新加载
sudo systemctl reload nginx
echo "Nginx 配置完成"
echo ""

# 步骤 5: 停止旧服务并启动后端
echo -e "${GREEN}[5/6] 启动后端服务...${NC}"
stop_backend

cd "$PROJECT_DIR"
# 加载环境变量
set -a
source .env
set +a

nohup java -jar -Xms256m -Xmx512m target/game-maker-1.0-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --server.port=$BACKEND_PORT \
    --server.address=127.0.0.1 \
    > "$APP_LOG" 2>&1 &

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
