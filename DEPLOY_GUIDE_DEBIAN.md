# ChengXun Game Maker - Debian 13.2.0 部署指南

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Java | 17+ | 推荐 OpenJDK 17 或 21 |
| Node.js | 18+ | 推荐 LTS 版本 |
| npm | 8+ | 随 Node.js 安装 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6+ | 缓存和会话存储 |
| Nginx | 1.18+ | Web服务器 |

---

## 一、基础环境安装

### 1.1 更新系统

```bash
sudo apt update && sudo apt upgrade -y
```

### 1.2 安装 Java 17

```bash
# 安装 OpenJDK 17
sudo apt install -y openjdk-17-jdk

# 验证安装
java -version

# 设置 JAVA_HOME（可选）
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### 1.3 安装 Node.js 20 LTS

```bash
# 安装 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -

# 安装 Node.js
sudo apt install -y nodejs

# 验证安装
node -v
npm -v

# 配置 npm 镜像（可选，国内加速）
npm config set registry https://registry.npmmirror.com
```

### 1.4 安装 Maven

```bash
sudo apt install -y maven

# 验证安装
mvn -v
```

### 1.5 安装 MySQL 8.0

```bash
# 安装 MySQL
sudo apt install -y mysql-server

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
# 按提示操作：
# - 设置 root 密码
# - 移除匿名用户
# - 禁止 root 远程登录
# - 移除测试数据库
# - 重新加载权限表
```

#### 创建数据库和用户

```bash
# 登录 MySQL
sudo mysql -u root -p

# 执行以下 SQL
CREATE DATABASE game_maker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'gamemaker'@'localhost' IDENTIFIED BY '你的安全密码';
GRANT ALL PRIVILEGES ON game_maker.* TO 'gamemaker'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 1.6 安装 Redis

```bash
# 安装 Redis
sudo apt install -y redis-server

# 启动 Redis
sudo systemctl start redis
sudo systemctl enable redis

# 验证安装
redis-cli ping
# 应返回 PONG
```

#### Redis 配置（可选，设置密码）

```bash
# 编辑 Redis 配置
sudo nano /etc/redis/redis.conf

# 找到 # requirepass foobared，取消注释并设置密码
requirepass 你的Redis密码

# 重启 Redis
sudo systemctl restart redis
```

### 1.7 安装 Nginx

```bash
sudo apt install -y nginx

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# 验证安装
nginx -v
```

---

## 二、项目部署

### 2.1 上传项目文件

```bash
# 方式1：使用 scp 上传
scp -r /home/chengxun/chengxun_game_maker user@新服务器IP:/home/user/

# 方式2：使用 rsync 同步
rsync -avz --exclude 'target' --exclude 'node_modules' --exclude '.git' \
  /home/chengxun/chengxun_game_maker user@新服务器IP:/home/user/

# 方式3：使用 git 克隆
git clone <你的仓库地址> /home/user/chengxun_game_maker
```

### 2.2 配置环境变量

```bash
cd /home/user/chengxun_game_maker

# 复制环境变量模板
cp .env.example .env  # 如果有模板的话
# 或者直接创建
nano .env
```

#### .env 文件内容

```bash
# ===== Spring Profile =====
SPRING_PROFILES_ACTIVE=prod

# ===== 数据库配置 =====
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=gamemaker
MYSQL_USER=gamemaker
MYSQL_PASSWORD=你的安全密码

# ===== Redis配置 =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=你的Redis密码

# ===== Claude API配置 =====
CLAUDE_API_KEY=你的Claude API Key
CLAUDE_API_URL=https://api.anthropic.com
CLAUDE_MODEL=claude-sonnet-4-20250514
CLAUDE_CLI_PATH=/usr/bin/claude

# ===== JWT配置 =====
JWT_SECRET=生成一个随机密钥（至少32位）
JWT_EXPIRATION=86400000

# ===== 邮件配置（可选） =====
EMAIL_ENABLED=false
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# ===== 飞书配置（可选） =====
FEISHU_ENABLED=false
FEISHU_APP_ID=
FEISHU_APP_SECRET=
FEISHU_WEBHOOK_URL=
FEISHU_CHAT_ID=
FEISHU_VERIFY_TOKEN=
FEISHU_ENCRYPT_KEY=

# ===== 钉钉配置（可选） =====
DINGTALK_ENABLED=false
DINGTALK_WEBHOOK_URL=
DINGTALK_SECRET=

# ===== 游戏项目目录 =====
GAME_PROJECT_ROOT=./game-projects

# ===== 监控配置（可选） =====
GRAFANA_PASSWORD=你的Grafana密码
```

#### 生成 JWT 密钥

```bash
# 生成随机密钥
openssl rand -base64 32
```

### 2.3 创建必要目录

```bash
cd /home/user/chengxun_game_maker

# 创建数据目录
mkdir -p data/projects
mkdir -p data/contexts
mkdir -p data/memory
mkdir -p data/templates
mkdir -p game-projects
mkdir -p logs
```

### 2.4 构建前端

```bash
cd /home/user/chengxun_game_maker/frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 部署前端文件
sudo mkdir -p /var/www/game-maker
sudo cp -r dist/* /var/www/game-maker/
sudo chown -R www-data:www-data /var/www/game-maker
```

### 2.5 构建后端

```bash
cd /home/user/chengxun_game_maker

# 构建（跳过测试）
mvn clean package -DskipTests

# 验证构建
ls -la target/game-maker-1.0-SNAPSHOT.jar
```

### 2.6 配置 Nginx

```bash
sudo nano /etc/nginx/sites-available/game-maker
```

#### Nginx 配置内容

```nginx
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

    # 前端路由 - SPA 支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # SSE 流式接口
    location /api/v1/ai-assistant/stream/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Swagger UI
    location ^~ /swagger-ui {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # OpenAPI 文档
    location /v3/api-docs {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400;
    }

    # 飞书 Webhook
    location /feishu/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 钉钉 Webhook
    location /dingtalk/ {
        proxy_pass http://127.0.0.1:19922;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
}
```

#### 启用站点

```bash
sudo ln -sf /etc/nginx/sites-available/game-maker /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 2.7 启动后端服务

```bash
cd /home/user/chengxun_game_maker

# 加载环境变量
set -a
source .env
set +a

# 启动服务
nohup java -jar -Xms256m -Xmx512m target/game-maker-1.0-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --server.port=19922 \
    --server.address=127.0.0.1 \
    > app.log 2>&1 &

# 保存 PID
echo $! > app.pid

# 查看日志
tail -f app.log
```

---

## 三、系统服务配置（推荐）

### 3.1 创建 systemd 服务

```bash
sudo nano /etc/systemd/system/game-maker.service
```

#### 服务文件内容

```ini
[Unit]
Description=ChengXun Game Maker Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=user
WorkingDirectory=/home/user/chengxun_game_maker
EnvironmentFile=/home/user/chengxun_game_maker/.env
ExecStart=/usr/bin/java -jar -Xms256m -Xmx512m /home/user/chengxun_game_maker/target/game-maker-1.0-SNAPSHOT.jar --spring.profiles.active=prod --server.port=19922 --server.address=127.0.0.1
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 3.2 启用服务

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start game-maker

# 设置开机启动
sudo systemctl enable game-maker

# 查看状态
sudo systemctl status game-maker

# 查看日志
sudo journalctl -u game-maker -f
```

---

## 四、防火墙配置

```bash
# 安装 ufw（如果没有）
sudo apt install -y ufw

# 允许 SSH
sudo ufw allow 22/tcp

# 允许 HTTP（前端端口）
sudo ufw allow 18080/tcp

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status
```

---

## 五、验证部署

### 5.1 检查服务状态

```bash
# 检查 MySQL
sudo systemctl status mysql

# 检查 Redis
sudo systemctl status redis

# 检查 Nginx
sudo systemctl status nginx

# 检查 Game Maker
sudo systemctl status game-maker
```

### 5.2 访问测试

```bash
# 本地测试
curl http://localhost:19922/api/install/status

# 外部访问
# 浏览器打开: http://你的服务器IP:18080
```

---

## 六、常见问题

### 6.1 Java 版本不匹配

```bash
# 如果有多个 Java 版本，切换到 17
sudo update-alternatives --config java
```

### 6.2 MySQL 连接失败

```bash
# 检查 MySQL 是否运行
sudo systemctl status mysql

# 检查用户权限
sudo mysql -u root -p -e "SELECT user, host FROM mysql.user;"
```

### 6.3 端口被占用

```bash
# 查看端口占用
sudo lsof -i:19922
sudo lsof -i:18080

# 杀死占用进程
sudo kill -9 <PID>
```

### 6.4 权限问题

```bash
# 修复目录权限
sudo chown -R user:user /home/user/chengxun_game_maker
chmod +x /home/user/chengxun_game_maker/deploy.sh
```

---

## 七、备份策略

### 7.1 数据库备份

```bash
# 创建备份脚本
nano ~/backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/home/user/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u gamemaker -p'密码' game_maker > $BACKUP_DIR/game_maker_$DATE.sql

# 保留最近7天的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
```

```bash
# 添加执行权限
chmod +x ~/backup.sh

# 添加定时任务
crontab -e
# 添加：每天凌晨3点备份
0 3 * * * /home/user/backup.sh
```

### 7.2 应用数据备份

```bash
# 备份数据目录
tar -czf game_maker_data_$(date +%Y%m%d).tar.gz \
    /home/user/chengxun_game_maker/data \
    /home/user/chengxun_game_maker/.env
```

---

## 八、监控（可选）

### 8.1 查看应用日志

```bash
# 实时查看日志
tail -f /home/user/chengxun_game_maker/app.log

# 或使用 systemd 日志
sudo journalctl -u game-maker -f
```

### 8.2 系统资源监控

```bash
# 安装 htop
sudo apt install -y htop

# 查看系统资源
htop
```

---

## 快速部署命令汇总

```bash
# 1. 安装基础环境
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk mysql-server redis-server nginx maven

# 2. 安装 Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 3. 配置 MySQL
sudo mysql -u root -p
# CREATE DATABASE game_maker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
# CREATE USER 'gamemaker'@'localhost' IDENTIFIED BY '密码';
# GRANT ALL PRIVILEGES ON game_maker.* TO 'gamemaker'@'localhost';
# FLUSH PRIVILEGES;

# 4. 上传项目并配置
cd /home/user/chengxun_game_maker
# 编辑 .env 文件

# 5. 构建部署
cd frontend && npm install && npm run build
sudo cp -r dist/* /var/www/game-maker/
cd .. && mvn clean package -DskipTests

# 6. 启动服务
sudo systemctl start mysql redis nginx
sudo systemctl enable mysql redis nginx
# 按上面的步骤配置 systemd 服务
```
