# ChengXun Game Maker

AI 驱动的游戏开发自动化管理系统，支持多 Agent 协作开发游戏项目。

> **核心理念**：让多个 AI Agent 像真实团队一样协作，把"想法"变成"可玩的游戏"。所有验证都基于**真实运行 + 截图视觉分析**，不是文件结构检查。

---

## 目录

- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [功能模块](#功能模块)
- [质量评分系统（V49 修复）](#质量评分系统v49-修复)
- [API 文档](#api-文档)
- [数据库迁移](#数据库迁移)
- [权限系统](#权限系统)
- [常见问题](#常见问题)
- [运维命令](#运维命令)

---

## 系统架构

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2, Spring Data JPA, Spring Security, JWT |
| 前端框架 | Vue 3, Element Plus, Pinia, Vue Router, Axios |
| 数据库 | MySQL 8.0+ (生产), H2 (测试) |
| 数据库迁移 | Flyway (V1 ~ V49) |
| AI 引擎 | Claude CLI |
| 游戏验证 | Chrome Headless + Claude Vision 多模态 |
| Web 服务器 | Nginx |
| 即时通讯 | WebSocket（项目讨论 / Agent 实时状态） |
| 外部集成 | 飞书 / 钉钉 / MCP Servers |

### 端口配置

| 服务 | 端口 | 备注 |
|------|------|------|
| 前端（Nginx） | 18080 | 静态资源 + 反向代理 |
| 后端 | 19922 | Spring Boot API |
| 游戏截图端口池 | 18200 - 18299 | Chrome Headless 临时端口 |

### JVM 内存配置（生产调优）

| 区域 | 大小 | 说明 |
|------|------|------|
| 堆（-Xms / -Xmx） | 8 GB | 对象主存储 |
| Metaspace | 512 MB | 类元数据 |
| Code Cache | 256 MB | JIT 编译代码 |
| Direct Memory | 512 MB | Netty 直接缓冲 |
| GC 算法 | G1 | 大堆友好，暂停 < 200ms |

启动参数示例：

```bash
java -Xms8g -Xmx8g \
     -XX:MaxMetaspaceSize=512m \
     -XX:ReservedCodeCacheSize=256m \
     -XX:MaxDirectMemorySize=512m \
     -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -jar target/game-maker-1.0-SNAPSHOT.jar
```

### 目录结构

```
chengxun_game_maker/
├── frontend/                              # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                           # API 接口定义（统一在 index.js）
│   │   ├── components/                    # 公共组件（含 streaming / TourGuide）
│   │   ├── layouts/                       # 布局组件
│   │   ├── router/                        # 路由配置
│   │   ├── stores/                        # Pinia 状态管理
│   │   ├── utils/                         # 工具函数（permission / shortcuts / batch）
│   │   └── views/                         # 50+ 页面（agent / project / verify / ...）
│   └── dist/                              # 构建产物（部署到 /var/www/game-maker）
├── src/main/java/com/chengxun/gamemaker/
│   ├── GameMakerApplication.java          # Spring Boot 启动类
│   ├── agent/                             # Agent 实现（11 类核心角色）
│   ├── config/                            # 配置类
│   ├── controller/                        # 旧版控制器（已逐步迁移到 web.controller）
│   ├── dingtalk/                          # 钉钉集成
│   ├── engine/                            # 核心引擎（消息队列 / AI 调用 / 工作流）
│   ├── feishu/                            # 飞书集成（卡片回调 / 审批）
│   ├── manager/                           # 管理器
│   ├── model/                             # 数据模型
│   ├── monitor/                           # 监控探针
│   ├── service/                           # 业务服务（旧版）
│   └── web/
│       ├── aspect/                        # 切面（日志 / 权限）
│       ├── config/                        # Web 配置（Security / MVC / WebSocket）
│       ├── constants/                     # 常量定义（PermissionConstants 等）
│       ├── controller/                    # 90+ REST 控制器
│       ├── dto/                           # 数据传输对象
│       ├── entity/                        # JPA 实体
│       ├── exception/                     # 全局异常处理
│       ├── repository/                    # 数据访问层
│       ├── service/                       # 业务服务层（30+ 服务）
│       ├── util/                          # 工具类
│       ├── validation/                    # 自定义校验注解
│       └── websocket/                     # WebSocket 端点
├── src/main/resources/
│   ├── application*.yml                   # Spring Boot 配置（prod / dev / test）
│   ├── db/
│   │   ├── schema.sql                     # Flyway 基线 schema
│   │   ├── init.sql                       # 完整安装脚本（mysqldump 格式）
│   │   └── migration/                     # Flyway 增量迁移 V1 ~ V49
│   └── templates/                         # 代码 / Prompt 模板
├── data/                                  # 运行时数据
│   ├── projects/                          # 游戏项目（按项目 ID 分目录）
│   ├── boards/                            # 看板数据
│   ├── knowledge-base/                    # 知识库
│   ├── game-screenshots/                  # G8：真实游戏运行截图存档
│   └── game-projects/                     # Agent 生成的 demo 游戏代码
├── sql/                                   # 数据库脚本（mysql + h2 + init data）
├── docs/                                  # 部署 / 设计 / 排错文档
├── deploy/                                # 部署相关脚本与配置
├── nginx.conf                             # Nginx 配置
├── deploy.sh                              # 一键部署脚本
├── docker-compose.yml                     # Docker Compose（开发环境）
├── Dockerfile                             # 后端镜像构建
└── .env.example                           # 环境变量模板
```

---

## 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+（生产） |
| Nginx | 最新稳定版 |
| Claude CLI | 已登录认证 |
| Chrome / Chromium | 用于 G8 游戏截图验证 |

### 配置流程

**1. 准备环境变量**

```bash
cp .env.example .env
# 编辑 .env：填写 MYSQL_HOST/USER/PASSWORD、Claude API 等
```

**2. 创建数据库**

```sql
CREATE DATABASE game_maker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**3. 首次启动自动初始化**

首次访问会自动跳转到 `/install` 页面完成：
- 数据库连接检测
- Flyway 迁移（V1 ~ V49）
- 管理员账号创建
- 系统配置初始化

### 一键部署（生产）

```bash
./deploy.sh
```

脚本自动完成：
1. 构建前端（`npm run build`）→ 部署到 `/var/www/game-maker`
2. 构建后端 JAR（`mvn clean package`）
3. 配置 Nginx 并 reload
4. 停止旧服务并启动新服务（带 G1GC 调优参数）

### 手动启动（开发）

```bash
# 后端
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run

# 前端开发模式
cd frontend && npm install && npm run dev
```

---

## 功能模块

### 多 Agent 协作

系统内置 11 类核心 Agent 角色，配合权限 / Prompt 库 / 技能系统，可扩展至 21 种角色。

| 角色 | 类 | 主要职责 |
|------|-----|----------|
| Producer | `ProducerAgent` | 目标分解、版本迭代、团队管理、质量门禁 |
| Server-Dev | `ServerDevAgent` | 服务端代码生成 |
| Client-Dev / UI-Dev | `UiDevAgent` | 客户端与 UI 实现 |
| System-Planner | `SystemPlannerAgent` | 系统策划（架构 / 模块拆分） |
| Numerical-Planner | `NumericalPlannerAgent` | 数值策划（公式 / 平衡） |
| Verifier | `VerificationAgent` | 多维度验证（结构 + 运行 + 视觉） |
| Generic | `GenericAgent` | 通用 Agent（兜底角色） |
| Git-Commit | `GitCommitAgent` | 自动提交 / 推送 |

配套能力系统（`agent_capability`）：
- **能力注册**：在 `AiToolRegistry.registerDefaultTools()` 中定义
- **能力调用**：Agent 在工作流中通过 sendMessage / executeCapability 触发
- **重入保护**：避免能力执行器内部触发嵌套能力导致无限循环

### G8：真实运行 + 截图视觉验证（核心特性）

> **变更背景**：之前所有验证都是文件结构检查，从 G8 起改为**真实启动游戏 + Chrome Headless 截图 + Claude Vision 视觉分析**。

**核心组件**：

| 组件 | 路径 | 职责 |
|------|------|------|
| `GameScreenshotService` | web/service | Chrome Headless 截图（端口池 18200-18299） |
| `ClaudeAiService.sendMessageWithImages` | service | 多模态对话（图片 base64 inline） |
| `GameRuntimeVerifier.verifyFull()` | engine | 5 维度评分集成 |
| `GameVerifyController` | web/controller | REST 端点 |

**5 维度评分权重**：

```
结构 10% + 构建 15% + 运行 25% + 视觉 20% + AI 质量 30%
```

**视觉分析 4 维度**：
- `renderHealth`：白屏 / 崩溃检测
- `playable`：玩法可见性
- `uiux`：界面布局合理性
- `visual`：整体视觉观感

**API 端点**：

```
POST   /api/game-verify/screenshot                 # 启动 + 截图
POST   /api/game-verify/analyze-screenshot         # AI 视觉分析
POST   /api/game-verify/verify-with-screenshot     # 一体化：运行 + 截图 + 视觉
GET    /api/game-verify/projects/{id}/screenshots  # 获取截图列表
GET    /api/game-verify/screenshots/view?path=...  # 查看单张截图
```

**AI 工具（Agent 可调用）**：

- `screenshot_game`
- `analyze_screenshot`
- `verify_game_with_screenshot`
- `get_project_screenshots`

### 项目管理

- 游戏项目创建 / 配置 / 版本迭代
- 里程碑（Milestone）+ 任务（Task）管理
- 项目讨论（支持 SSE 实时流）
- 实时运行状态监控

### 代码质量

- 自动化代码审查（基于 Git diff）
- 多维度质量评分（结构 / 构建 / 运行 / 视觉 / AI）
- 质量门禁（L1 ~ L5）
- AI 设计审查（`design_review_records` 表）
- 质量预测（`QualityPredictionController`）

### 通知系统

支持 3 + 1 个渠道：

| 渠道 | 实现类 | 说明 |
|------|--------|------|
| 站内信 | `NotificationService` | 系统内部通知 |
| 邮件 | `EmailService` | SMTP 发送 |
| 飞书 | 飞书机器人 | **必须用卡片消息**（不要纯文字） |
| 钉钉 | `dingtalk` 模块 | 企业通知 |

模板管理：`NotificationTemplateService.initDefaultTemplates()` 在启动时自动创建默认模板。

### MCP 集成

预置 35+ MCP Server 模板（filesystem / github / puppeteer / feishu-doc / unity / godot / 资源生成等），分类如下：

| 分类 | 说明 |
|------|------|
| resource-image / audio / video / 3d | 资源生成 |
| dev / gamedev | 开发工具 |
| data / collaboration | 数据 / 协作 |
| monitoring | 监控 |

MCP Server 可独立配置 AI 凭据（V43）、认证模式（V44）、工具参数提示（V45）。

### 知识库与工作流

- 知识图谱（`knowledge_graph`）
- 知识进化（`knowledge_evolution`）
- 工作流引擎（`WorkflowEngine`，2492 行核心实现）
- 工作流审计（`WorkflowAuditService`）
- 多轮推理（`MultiTurnReasoningController`）

### 智能调度

- Agent 调度器（`AgentSchedulerController`）
- 智能调度器（`IntelligentSchedulerController`）
- 迭代自适应（`IterationAdaptController`）
- Agent 健康（`AgentHealthController`）
- Agent 干预（`AgentInterventionController`）

### Token 与资源管理

- Token 池（`TokenController`，含 provider_type / resource_type 区分）
- 资源用量（`ResourceUsageController`）
- Agent 招聘（`AgentRecruitmentController`）
- Agent 绩效（`AgentPerformanceController`）

### 安全与合规

- Spring Security + JWT 鉴权
- 设备信任（`DeviceTrustService`）
- 登录尝试限制（`LoginAttemptService`）
- Prompt 安全（`PromptSecurityService`）
- 邮箱修改走审批流
- 操作日志（`OperationLogService`）
- 数据归档（`DataArchivalService`）

### 诊断与监控

- 系统诊断（`SystemDiagnosticService`）
- 深度健康检查（`DeepHealthCheckService`）
- 性能监控（`PerformanceMonitorService`）
- 监控指标采集（`MetricsCollectorService`）
- 告警（`AlertController` + `alert_records` 表）
- 日志（支持 CSV 导出）

---

## 质量评分系统（V49 修复）

### 问题背景

2026-07 排查发现：当 AI 分析失败时，`QualityGateService` 会硬编码 `score = 50` 或 `score = 0`，导致：

- ProducerAgent 据此反复触发"质量改进"迭代
- 同一里程碑硬上限 6 次过高，造成"武器系统失败 7 次"失控循环
- 工作流 `COMPLETED` 事件 5 分钟防重复窗口太短，反复触发质量门禁

### V49 修复

**代码层（5 个 Java 文件）**：

| 文件 | 变更 |
|------|------|
| `GameRuntimeVerifier.java` | 新增 `FailureType` 枚举（8 种失败原因）；维度分数默认 `-1` 区分"未评估"与"零分"；删除"维度 0 时强行填成 overallScore"的污染逻辑 |
| `QualityGateService.java` | `QualityCheckResult` 加 `analysisFailed` + `failureReason` 字段；失败时 `score = -1` 而非 0/50；`calculateOverallScore` 跳过 `analysisFailed` 的门禁 |
| `ProducerAgent.java` | 硬上限从 6 次降为 3 次；冷却从 5 分钟改为 30 分钟；升级后 `markQualityIterationTriggered` 防止再次触发；AI 分析失败时返回 `true` 发送 `QUALITY_ANALYSIS_FAILED` 通知而非触发改进 |

**数据层（V49 Flyway 迁移）**：

新增 4 条 `system_constants` 配置（运行期可调）：

| constant_key | 默认值 | 含义 |
|--------------|--------|------|
| `quality.iteration.max-fail-count` | 3 | 同一里程碑验证失败超过此值停止自动改进 |
| `quality.iteration.cooldown-minutes` | 30 | 两次自动改进迭代之间的最小间隔 |
| `quality.score.analysis-failed-marker` | -1 | 分析失败时的标记分数 |
| `quality.score.threshold` | 60 | 通过质量门禁的最低分 |

**部署教训**：
- Flyway 迁移失败时，需 `DELETE FROM flyway_schema_history WHERE version='49';` 才能重试
- 修改 SQL 必须同时同步 `sql/sql_init_data_mysql.sql` 与 `src/main/resources/db/init.sql`

---

## API 文档

| 入口 | URL |
|------|-----|
| Swagger UI | `http://your-host:18080/swagger-ui.html` |
| OpenAPI JSON | `http://your-host:19922/v3/api-docs` |

API 按业务域分组（90+ Controller），核心模块：

```
/api/auth/*                  # 认证（登录 / 注册 / 验证码 / 密码）
/api/agents/*                # Agent 管理 / 状态 / 调度
/api/projects/*              # 游戏项目 CRUD / 迭代 / 讨论
/api/tasks/*                 # 任务管理
/api/milestones/*            # 里程碑
/api/game-verify/*           # G8 游戏验证（截图 / 视觉分析）
/api/ai-assistant/*          # AI 助手对话
/api/notifications/*         # 站内信
/api/approvals/*             # 审批（飞书 / 站内）
/api/code-quality/*          # 代码质量审查
/api/code-review/*           # 设计审查
/api/pipeline/*              # CI/CD 流水线
/api/mcp/*                   # MCP Server / Tool 管理
/api/knowledge-base/*        # 知识库
/api/workflow/*              # 工作流模板
/api/tokens/*                # Token 池
/api/permissions/*           # 权限管理
/api/roles/*                 # 角色管理
/api/users/*                 # 用户管理
/api/admin/*                 # 管理员后台
```

---

## 数据库迁移

使用 **Flyway** 管理 schema 演进，迁移脚本位于 `src/main/resources/db/migration/V{n}__description.sql`。

**当前版本**：V49（质量评分失败分离）

**主要迁移里程碑**：

| 版本 | 内容 |
|------|------|
| V1 ~ V39 | 基础表结构 + 早期迭代 |
| V40 | 设计审查记录表（`design_review_records`） |
| V41 | api_tokens 新增 `provider_type` / `resource_type` |
| V42 | mcp_tools 新增 `default_params` |
| V43 | mcp_servers 独立 AI 配置字段 |
| V44 | mcp_servers 模板分类 + 认证模式 |
| V45 | mcp_tools 参数提示 |
| V46 | G7 游戏质量验证系统（`game_verify_records` / `runtime_errors`） |
| V47 | alert_records 允许 NULL rule_id |
| V48 | G8 游戏截图与视觉分析（`game_screenshots` + 视觉字段） |
| V49 | 质量评分失败分离 + 硬上限系统常量 |

**自动应用**：服务启动时自动检测并应用未执行的迁移。

**同步规范**：任何 DDL / DML 变更必须同步更新：

| 文件 | 用途 |
|------|------|
| `sql/sql_create_mysql.sql` | MySQL 建表脚本（生产安装） |
| `sql/sql_create_h2.sql` | H2 建表脚本（测试） |
| `sql/sql_init_data_mysql.sql` | MySQL 初始化数据 |
| `sql/sql_init_data_h2.sql` | H2 初始化数据 |
| `src/main/resources/db/schema.sql` | Flyway 基线 schema |
| `src/main/resources/db/init.sql` | 完整安装脚本（mysqldump 格式） |

⚠️ **注意**：
- `system` 是 MySQL 8.0+ 保留字，SQL 中必须用反引号包裹（`` `system` ``）
- `ENGINE=InnoDB` 仅 MySQL 需要
- `AUTO_INCREMENT` 与 H2 写法不同

---

## 权限系统

权限定义集中在 `PermissionConstants` 类，遵循 `模块:操作` 命名规范。

### 核心权限

| 权限 | 说明 |
|------|------|
| `user:manage` | 用户管理 |
| `role:manage` | 角色管理 |
| `permission:manage` | 权限管理 |
| `project:create` / `project:update` / `project:delete` | 项目 CRUD |
| `project:view` | 项目查看 |
| `agent:create` / `agent:delete` / `agent:start` / `agent:stop` | Agent 操作 |
| `agent:view` | Agent 查看 |
| `task:assign` / `task:execute` | 任务操作 |
| `approval:create` / `approval:approve` | 审批操作 |
| `game:verify` | 触发游戏验证 |
| `game:verify:view` | 查看验证结果 |
| `game:visual:view` | G8：查看游戏视觉验证（截图） |
| `game:preview` | 启动游戏预览 |
| `code:review` | 代码审查 |
| `notification:send` | 发送通知 |
| `system:config` | 系统配置 |
| `mcp:manage` | MCP Server 管理 |

### 权限同步规范

新增 API 时**必须**同步：
1. `PermissionService` 中定义权限标识
2. `role_permissions` 表中为相关角色分配权限
3. 前端 `utils/permission.js` 中添加路由守卫映射
4. SQL 脚本（mysql + h2 + init data）同步插入

---

## 常见问题

### Claude CLI 无响应

```bash
claude auth status
```

如需重新登录：`claude auth login`。

### Chrome 截图失败

- 检查 Chrome 路径：`/usr/bin/google-chrome`
- 检查截图目录：`data/game-screenshots/` 是否可写
- 检查端口 18200 - 18299 是否被占用：`netstat -tlnp | grep 182`

### 数据库连接失败

检查 `.env` 中 `MYSQL_*` 变量配置；测试连接：

```bash
mysql -u $MYSQL_USER -p$MYSQL_PASSWORD -h $MYSQL_HOST -e "SELECT 1"
```

### 部署后前端 404

确认 Nginx `root` 指向 `/var/www/game-maker`：

```bash
sudo nginx -t && sudo nginx -s reload
ls -la /var/www/game-maker/  # 应有 index.html + assets/
```

### Flyway 迁移失败

1. 查看 `app.log` 中 `Migration of schema ... failed` 详情
2. 若为字段名写错等可修复问题，先修复 SQL
3. 从 `flyway_schema_history` 删除失败的版本：

```sql
DELETE FROM flyway_schema_history WHERE version='49' AND success=0;
```

4. 重新部署 `./deploy.sh`

### Agent 不调度

- 检查 `app.log` 中是否有 `ProducerAgent` / `BaseAgent` 异常
- 检查 Token 池是否耗尽（`/api/tokens`）
- 检查系统常量 `agent.scheduler.enabled` 是否为 `true`

### 通知发送失败

- 飞书：检查 webhook URL + 签名密钥
- 邮件：检查 SMTP 配置（`MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD`）
- 站内信：通常不会失败，可在 `/api/notifications` 查询

---

## 运维命令

```bash
# 查看实时日志
tail -f /home/chengxun/chengxun_game_maker/app.log

# 一键部署（构建 + 重启）
/home/chengxun/chengxun_game_maker/deploy.sh

# 优雅停止
kill $(cat /home/chengxun/chengxun_game_maker/app.pid)

# 强制停止 + 启动
kill -9 $(cat /home/chengxun/chengxun_game_maker/app.pid)
nohup java -jar target/game-maker-1.0-SNAPSHOT.jar > app.log 2>&1 &

# 查看 JVM 堆信息
jcmd $(cat app.pid) GC.heap_info

# 查看线程栈
jstack $(cat app.pid) | head -50

# Nginx 重新加载配置
sudo nginx -t && sudo nginx -s reload

# 数据库备份
mysqldump -u root -p game_maker > backup_$(date +%Y%m%d).sql

# 清理旧截图（保留最近 7 天）
find /home/chengxun/chengxun_game_maker/data/game-screenshots/ \
     -type f -mtime +7 -delete
```

---

## 项目规范（开发要点）

完整的开发规范见 [`CLAUDE.md`](./CLAUDE.md)，核心要点：

- **API 权限同步**：新增 / 修改 API 必须同步 4 处（PermissionService + role_permissions + 前端 utils + SQL）
- **AI 助手工具同步**：AI 助手工具在 `AiToolRegistry.registerDefaultTools()` 中注册
- **SQL 脚本同步**：DDL / DML 变更必须同步 6 个 SQL 文件
- **中文注释**：类 / 方法 / 字段必须有 Javadoc 注释
- **通知模板**：PRODUCER_* 模板 SQL 中只存 FEISHU 渠道，其余由 `NotificationTemplateService.initDefaultTemplates()` 运行时创建

---

## 许可证

私有项目，仅供内部使用。