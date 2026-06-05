# ChengXun Game Maker 快速启动指南

## 前置条件

1. **Java 17+**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 8.0+**
   ```bash
   mysql --version
   ```

4. **Redis 6.0+**
   ```bash
   redis-server --version
   ```

## 快速启动

### 方式一：使用启动脚本（推荐）

```bash
# 1. 检查依赖
bash check-dependencies.sh

# 2. 启动应用
bash start.sh
```

### 方式二：手动启动

```bash
# 1. 创建数据库
mysql -u root -p < init-database.sql

# 2. 配置环境变量（可选）
cp .env.example .env
# 编辑 .env 文件，填入实际配置

# 3. 编译项目
mvn clean package -DskipTests

# 4. 启动应用
java -jar target/game-maker-1.0-SNAPSHOT.jar
```

### 方式三：使用Maven直接启动

```bash
# 1. 创建数据库
mysql -u root -p < init-database.sql

# 2. 启动应用
mvn spring-boot:run
```

## 访问应用

- **地址**: http://localhost:9922
- **默认账号**: admin
- **默认密码**: 首次启动时随机生成，查看启动日志获取

### 获取默认密码

首次启动时，系统会自动生成管理员密码并打印到日志中：

```bash
# 启动后查看日志，找到类似以下内容：
# ==========================================================
# 默认管理员账号已创建:
#   用户名: admin
#   密码: xY2kL9mN...
#   首次登录后必须修改密码！
# ==========================================================
```

**注意**: 首次登录后必须修改密码！

## 功能模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 仪表盘 | / | 系统概览 |
| Agents | /agents | Agent管理 |
| 项目 | /projects | 项目管理 |
| 技能 | /skills | 技能管理 |
| 监控 | /monitor | 系统监控 |
| 告警 | /alerts | 告警管理 |
| 通知模板 | /notification-templates | 通知模板配置 |
| 钉钉配置 | /dingtalk/config | 钉钉机器人配置 |

## 可选配置

### 飞书机器人

1. 在飞书开放平台创建应用
2. 获取App ID和App Secret
3. 配置环境变量：
   ```
   FEISHU_ENABLED=true
   FEISHU_APP_ID=your_app_id
   FEISHU_APP_SECRET=your_app_secret
   ```

### 钉钉机器人

1. 在钉钉群中创建自定义机器人
2. 获取Webhook URL
3. 在系统中配置：访问 /dingtalk/config

### 邮件通知

1. 配置SMTP服务器信息
2. 设置环境变量：
   ```
   EMAIL_ENABLED=true
   MAIL_HOST=smtp.qq.com
   MAIL_USERNAME=your_email@qq.com
   MAIL_PASSWORD=your_password
   ```

## 常见问题

### 1. 数据库连接失败

检查MySQL服务是否启动，以及配置是否正确：
```bash
mysql -u root -p -h localhost -P 3306
```

### 2. Redis连接失败

检查Redis服务是否启动：
```bash
redis-cli ping
```

### 3. 端口被占用

检查端口9922是否被占用：
```bash
lsof -i :9922
```

### 4. 编译失败

清理并重新编译：
```bash
mvn clean
mvn package -DskipTests
```

## 更多信息

- API文档：http://localhost:9922/swagger-ui.html
- 健康检查：http://localhost:9922/api/public/health
- 系统信息：http://localhost:9922/api/public/info
