# ChengXun Game Maker 项目规范

## 项目概述

ChengXun Game Maker 是一个 AI 驱动的游戏开发自动化管理系统，支持多 Agent 协作开发游戏项目。

### 技术栈

- **后端**: Spring Boot 3.2, Spring Data JPA, Spring Security, JWT
- **前端**: Vue 3, Element Plus, Pinia, Vue Router, Axios
- **数据库**: MySQL (生产), H2 (测试)
- **AI 引擎**: Claude CLI

### 前端架构

```
frontend/src/
├── api/                # API 接口定义
│   └── index.js        # 所有 API 模块导出
├── components/         # 公共组件
│   ├── TourGuide.vue   # 新手引导组件
│   └── streaming/      # 流式输出组件
├── layouts/            # 布局组件
│   └── MainLayout.vue  # 主布局
├── router/             # 路由配置
│   └── index.js        # 路由定义
├── stores/             # Pinia 状态管理
│   └── user.js         # 用户状态
├── utils/              # 工具函数
│   ├── permission.js   # 权限工具
│   ├── shortcuts.js    # 快捷键工具
│   └── batch.js        # 批量操作工具
└── views/              # 页面组件
    ├── admin/          # 管理页面
    ├── agents/         # Agent 页面
    ├── alerts/         # 告警页面
    ├── ai-assistant/   # AI 助手页面
    ├── code/           # 代码浏览页面
    ├── code-quality/   # 代码质量页面
    ├── devices/        # 设备信任页面
    ├── dingtalk/       # 钉钉配置页面
    ├── git/            # Git 仓库页面
    ├── health/         # Agent 健康页面
    ├── interventions/  # Agent 干预页面
    ├── notifications/  # 通知管理页面
    ├── notification-templates/ # 通知模板页面
    ├── performance/    # Agent 绩效页面
    ├── performance-mgmt/ # 绩效管理页面
    ├── pipeline/       # CICD 流水线页面
    ├── profile/        # 个人资料页面
    ├── projects/       # 项目管理页面
    ├── recruitment/    # Agent 招聘页面
    ├── resources/      # 资源用量页面
    ├── reviews/        # 代码审查页面
    ├── scheduler/      # Agent 调度页面
    ├── search/         # 全局搜索页面
    ├── skills/         # 技能管理页面
    ├── tokens/         # Token 管理页面
    └── workflow/       # 工作流页面
```

## 代码规范

### 中文注释要求

**所有代码必须有足够丰富的中文注释**，包括但不限于：

1. **类注释**：每个类必须有类级别的Javadoc注释，说明类的用途、主要功能和使用场景
2. **方法注释**：每个公共方法必须有方法级别的Javadoc注释，说明：
   - 方法的功能描述
   - 参数说明（@param）
   - 返回值说明（@return）
   - 异常说明（@throws）
   - 特殊说明（如线程安全性、前置条件等）
3. **字段注释**：重要的类字段必须有注释说明其用途和取值范围
4. **常量注释**：所有常量必须有注释说明其含义和使用场景
5. **复杂逻辑注释**：复杂的业务逻辑必须有行内注释说明实现思路

### 注释示例

```java
/**
 * Agent管理器
 * 负责Agent的创建、销毁、调度和生命周期管理
 * 
 * 主要功能：
 * - 根据配置创建不同类型的Agent
 * - 管理Agent的运行状态
 * - 提供Agent查询和监控接口
 * 
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AgentManager {

    /** Agent实例缓存，key为Agent ID */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 创建Agent实例
     * 
     * 根据AgentDefinition中的role字段创建对应类型的Agent：
     * - producer: 制作人Agent
     * - server-dev: 服务端开发Agent
     * - git-commit: Git提交专员Agent
     * - system-planner: 系统策划Agent
     * - numerical-planner: 数值策划Agent
     * 
     * @param definition Agent定义，包含ID、名称、角色等信息
     * @return 创建的Agent实例
     * @throws IllegalArgumentException 当role不支持时抛出
     */
    public Agent createAgent(AgentDefinition definition) {
        // 实现细节...
    }
}
```

### 命名规范

1. **类名**：使用大驼峰命名法（PascalCase），如 `AgentManager`、`UserService`
2. **方法名**：使用小驼峰命名法（camelCase），如 `createAgent`、`getUserById`
3. **常量**：使用全大写下划线分隔（UPPER_SNAKE_CASE），如 `MAX_RETRY_COUNT`
4. **变量**：使用小驼峰命名法，名称要有意义，避免缩写

### 项目结构

```
src/main/java/com/chengxun/gamemaker/
├── agent/              # Agent实现类
├── config/             # 配置类
├── controller/         # 控制器（已迁移到web/controller）
├── engine/             # 核心引擎（消息队列、任务调度等）
├── feishu/             # 飞书集成
├── manager/            # 管理器类
├── model/              # 数据模型
└── web/
    ├── config/         # Web配置（Security、MVC等）
    ├── controller/     # Web控制器
    ├── dto/            # 数据传输对象
    ├── entity/         # JPA实体类
    ├── repository/     # 数据访问层
    └── service/        # 业务服务层
```

## 安全规范

1. **密码安全**：
   - 默认密码必须随机生成
   - 首次登录必须强制修改密码
   - 密码强度要求：至少8位，包含大小写字母、数字和特殊字符

2. **敏感数据**：
   - API Key等敏感信息必须脱敏显示
   - 数据库中的敏感字段需要加密存储

3. **CSRF保护**：
   - 所有表单操作必须启用CSRF保护
   - 外部API（如飞书Webhook）可禁用CSRF

4. **会话管理**：
   - 会话超时时间：30分钟
   - 每个用户最多1个并发会话
   - 登出时清除会话Cookie

## Agent开发规范

1. **Agent工作目录**：
   - 每个Agent的工作目录需要可配置
   - 所有Agent工作目录必须在同一个项目大目录下

2. **Agent通信**：
   - 使用消息队列进行Agent间通信
   - 支持消息优先级和重试机制

3. **Agent任务**：
   - 任务队列支持优先级排序
   - 任务失败支持自动重试（最多3次）

## 版本管理

1. **版本号规范**：使用语义化版本号（Semantic Versioning）
   - 主版本号：不兼容的API修改
   - 次版本号：向下兼容的功能性新增
   - 修订号：向下兼容的问题修正

2. **版本历史**：记录每次版本变更的详细信息

## 通知系统

支持三种通知渠道：
1. **站内信**：系统内部通知
2. **邮件**：通过邮件服务发送
3. **飞书**：通过飞书机器人发送

## 日志规范

1. **日志级别**：
   - ERROR：错误日志，需要立即处理
   - WARN：警告日志，需要关注
   - INFO：信息日志，记录关键操作
   - DEBUG：调试日志，仅开发环境启用

2. **日志格式**：包含时间戳、日志级别、类名、消息内容

3. **日志导出**：支持导出为CSV格式

## 配置管理

1. **系统配置**：存储在数据库的system_configs表中
2. **配置分组**：按功能模块分组（security、agent、email、notification、system）
3. **配置缓存**：使用内存缓存提高查询效率
4. **热更新**：支持配置动态更新，无需重启服务
