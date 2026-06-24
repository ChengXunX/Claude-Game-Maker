# ChengXun Game Maker

AI 驱动的游戏开发自动化管理系统，支持多 Agent 协作开发游戏项目。

## 系统架构

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2, Spring Data JPA, Spring Security, JWT |
| 前端 | Vue 3, Element Plus, Pinia, Vue Router, Axios |
| 数据库 | MySQL (生产), H2 (测试) |
| AI 引擎 | Claude CLI |
| Web 服务器 | Nginx |

### 端口配置

| 服务 | 端口 |
|------|------|
| 前端 | 18080 |
| 后端 | 19922 |

### 目录结构

```
chengxun_game_maker/
├── frontend/              # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/          # API 接口定义
│   │   ├── components/   # 公共组件
│   │   ├── layouts/      # 布局组件
│   │   ├── router/       # 路由配置
│   │   ├── stores/       # Pinia 状态管理
│   │   ├── utils/        # 工具函数
│   │   └── views/        # 页面组件
│   └── dist/             # 构建输出
├── src/main/java/        # 后端 Java 源码
│   └── com/chengxun/gamemaker/
│       ├── agent/         # Agent 实现
│       ├── config/       # 配置类
│       ├── engine/        # 核心引擎
│       ├── feishu/        # 飞书集成
│       ├── manager/       # 管理器
│       ├── model/         # 数据模型
│       └── web/           # Web 层（Controller、Service、Repository）
├── sql/                   # 数据库脚本
├── nginx.conf            # Nginx 配置文件
├── deploy.sh             # 部署脚本
└── .env                  # 环境变量（需创建）
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+（生产环境）
- Nginx
- Claude CLI（已登录并认证）

### 配置

1. 复制环境变量模板并配置：

```bash
cp .env.example .env  # 如果存在模板
# 编辑 .env 填入数据库、Claude API 等配置
```

2. 创建数据库：

```sql
CREATE DATABASE game_maker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 首次启动时，系统会自动跳转到安装页面完成初始化配置。

### 部署

使用部署脚本一键部署：

```bash
./deploy.sh
```

部署脚本会自动：
- 构建前端并部署到 `/var/www/game-maker`
- 构建后端 JAR
- 配置 Nginx
- 启动后端服务

### 手动启动

#### 后端

```bash
# 配置环境变量
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/game_maker
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
export CLAUDE_CLI_API_KEY=your_api_key

# 启动
java -jar target/game-maker-1.0-SNAPSHOT.jar --server.port=19922
```

#### 前端（开发模式）

```bash
cd frontend
npm install
npm run dev
```

## 功能模块

### Agent 管理

- **Producer Agent**：制作人 Agent，负责游戏整体规划和版本迭代
- **Server-Dev Agent**：服务端开发 Agent，负责后端代码开发
- **Git-Commit Agent**：Git 提交专员，负责代码提交和版本管理
- **System-Planner Agent**：系统策划 Agent，负责游戏系统设计
- **Numerical-Planner Agent**：数值策划 Agent，负责游戏数值平衡

### 项目管理

- 游戏项目创建、配置、版本迭代
- 里程碑和任务管理
- 项目讨论（支持 SSE 实时流）

### 代码质量

- 自动化代码审查
- 质量评分和验证

### 通知系统

- 站内信
- 邮件通知
- 飞书机器人通知

### 系统配置

- 用户权限管理
- Token 管理
- AI 模型配置
- 飞书/钉钉集成

## API 文档

部署后访问：`http://your-host:18080/swagger-ui.html`

## 常见问题

### Claude CLI 无响应

检查 CLAUDE CLI 是否正确安装并登录：
```bash
claude auth status
```

### 前端静态资源 404

确保 Nginx 配置正确加载且 `root` 路径指向 `/var/www/game-maker`。

### 数据库连接失败

检查 MySQL 服务状态及 `.env` 中的数据库配置是否正确。

## 许可证

私有项目，仅供内部使用。
