# ChengXun Game Maker

AI 驱动的游戏开发自动化管理系统，支持多 Agent 协作开发游戏项目。

> 核心理念：让多个 AI Agent 像真实团队一样协作，把"想法"变成"可玩的游戏"。所有验证都基于**真实运行+截图**，不是文件结构检查。

## 系统架构

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2, Spring Data JPA, Spring Security, JWT |
| 前端 | Vue 3, Element Plus, Pinia, Vue Router, Axios |
| 数据库 | MySQL 8.0+ (生产), H2 (测试) |
| AI 引擎 | Claude CLI |
| 游戏验证 | Chrome Headless + Claude vision 多模态 |
| Web 服务器 | Nginx |

### 端口配置

| 服务 | 端口 |
|------|------|
| 前端 | 18080 |
| 后端 | 19922 |
| 游戏截图端口池 | 18200-18299 |

### JVM 内存配置（G8 调优后）

| 区域 | 大小 | 说明 |
|------|------|------|
| 堆 (-Xms/-Xmx) | 8 GB | 对象主存储 |
| Metaspace | 512 MB | 类元数据（原 2G 过大，缩减为典型值） |
| Code Cache | 256 MB | JIT 编译代码 |
| Direct Memory | 512 MB | Netty 直接缓冲 |
| GC 算法 | G1 | 大堆友好，暂停 < 200ms |

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
│       ├── agent/         # Agent 实现（Producer/ServerDev/Verifier/...）
│       ├── engine/        # 核心引擎（消息队列、AI 调用）
│       ├── feishu/        # 飞书集成
│       ├── manager/       # 管理器
│       └── web/           # Web 层（Controller、Service、Repository）
├── data/                 # 运行时数据
│   ├── projects/         # 项目数据
│   ├── boards/           # 看板数据
│   ├── knowledge-base/   # 知识库
│   └── game-screenshots/ # G8：游戏真实运行截图存档
├── sql/                   # 数据库脚本（mysql + h2 + init data）
├── nginx.conf            # Nginx 配置文件
├── deploy.sh             # 部署脚本
└── .env                  # 环境变量
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+（生产环境）
- Nginx
- Claude CLI（已登录认证）
- Google Chrome / Chromium（用于 G8 游戏截图）

### 配置

1. 复制环境变量模板：

```bash
cp .env.example .env  # 编辑 .env 填入数据库、Claude API 等配置
```

2. 创建数据库：

```sql
CREATE DATABASE game_maker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 首次启动会自动跳转到安装页面完成初始化。

### 部署

```bash
./deploy.sh
```

部署脚本自动完成：
- 构建前端（npm run build）→ 部署到 `/var/www/game-maker`
- 构建后端 JAR
- 配置 Nginx 并 reload
- 启动后端服务（带 G1GC + 调优后的非堆内存）

### 手动启动

```bash
# 后端
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_HOST=localhost
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
java -jar target/game-maker-1.0-SNAPSHOT.jar --server.port=19922

# 前端开发模式
cd frontend && npm install && npm run dev
```

## 功能模块

### 多 Agent 协作

| Agent 角色 | 职责 |
|-----------|------|
| Producer | 制作人：目标分解、版本迭代、团队管理 |
| Server-Dev | 服务端开发 |
| Client-Dev | 客户端开发 |
| UI-Dev | UI/UX 实现 |
| System-Planner | 系统策划 |
| Numerical-Planner | 数值策划 |
| Verifier | 验证督查（结构 + 真实运行 + 视觉） |
| Tester | 测试 |
| Security-Expert | 安全审查 |
| DevOps | CI/CD |
| ... | 共 21 种角色 |

### G8：真实运行+截图验证（核心特性）

之前所有验证都是文件结构检查，从 G8 起改为**真实运行游戏 + Chrome Headless 截图 + Claude vision 视觉分析**。

**核心组件**：
- `GameScreenshotService` — Chrome Headless 截图，端口池 18200-18299
- `ClaudeAiService.sendMessageWithImages` — 多模态对话（base64 inline）
- `GameRuntimeVerifier.verifyFull()` — 集成 5 维度评分
- `GameVerifyController` — 5 个新 API 端点

**5 维度评分权重**：
- 结构 10% / 构建 15% / 运行 25% / **视觉 20%** / AI 质量 30%

**视觉分析 4 维度**：renderHealth（白屏/崩溃检测）/ playable（玩法可见性）/ uiux（界面布局）/ visual（整体视觉）

**新 API 端点**：
```
POST   /api/game-verify/screenshot                 # 启动+截图
POST   /api/game-verify/analyze-screenshot         # AI 视觉分析
POST   /api/game-verify/verify-with-screenshot     # 一体化：运行+截图+视觉
GET    /api/game-verify/projects/{id}/screenshots  # 获取截图列表
GET    /api/game-verify/screenshots/view?path=...  # 查看截图
```

**AI 工具**（Agent 可调用）：
- `screenshot_game` — 启动并截图
- `analyze_screenshot` — 视觉分析
- `verify_game_with_screenshot` — 完整流程
- `get_project_screenshots` — 查截图列表

**Agent 能力**：
- `screenshotGame` / `analyzeScreenshot` / `verifyGameWithScreenshot`（verifier 角色）

### 项目管理

- 游戏项目创建、配置、版本迭代
- 里程碑和任务管理
- 项目讨论（支持 SSE 实时流）
- 实时运行状态监控

### 代码质量

- 自动化代码审查
- 质量评分（多维度）
- 质量门禁（L1-L5）
- AI 设计审查

### 通知系统

- 站内信
- 邮件通知
- 飞书机器人（卡片消息）
- 钉钉集成

### MCP 集成

预置 35+ MCP Server 模板：filesystem / github / puppeteer / feishu-doc / unity / godot 等。

## API 文档

部署后访问：`http://your-host:18080/swagger-ui.html`

或查看 OpenAPI 规范：`http://your-host:19922/v3/api-docs`

## 数据库迁移

使用 Flyway 管理：
- 迁移脚本：`src/main/resources/db/migration/V{n}__description.sql`
- 当前版本：V48（游戏截图与视觉分析）
- 自动应用：服务启动时自动执行新迁移

## 权限系统

| 权限 | 说明 |
|------|------|
| `game:verify` | 触发游戏验证 |
| `game:verify:view` | 查看验证结果 |
| `game:visual:view` | G8：查看游戏视觉验证（截图） |
| `game:preview` | 启动游戏预览 |

详细权限定义见 `PermissionConstants`。

## 常见问题

### Claude CLI 无响应
```bash
claude auth status
```

### Chrome 截图失败
- 检查 Chrome 路径：`/usr/bin/google-chrome`
- 检查截图目录：`data/game-screenshots/`
- 检查端口：18200-18299 是否被占用

### 数据库连接失败
检查 `.env` 中 `MYSQL_*` 变量配置。

### 部署后前端 404
确认 Nginx `root` 指向 `/var/www/game-maker`：
```bash
sudo nginx -t && sudo nginx -s reload
```

## 运维命令

```bash
# 查看日志
tail -f /home/chengxun/chengxun_game_maker/app.log

# 重启服务
/home/chengxun/chengxun_game_maker/deploy.sh

# 停止服务
kill $(cat /home/chengxun/chengxun_game_maker/app.pid)

# 查看 JVM 状态
jcmd <PID> GC.heap_info
```

## 许可证

私有项目，仅供内部使用。