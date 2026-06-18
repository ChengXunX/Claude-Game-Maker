package com.chengxun.gamemaker.config;

/**
 * 系统常量定义类
 * 所有常量在此集中声明，启动时自动同步到数据库
 *
 * 新增常量只需在此类添加字段并标注 @SystemConstantDef，
 * 无需修改数据库或 initDefaults() 方法。
 *
 * @author chengxun
 * @since 2.0.0
 */
public final class SystemConstants {

    private SystemConstants() {} // 工具类

    // ===== Agent 相关 =====

    @SystemConstantDef(
        key = "agent.max-message-backlog",
        name = "最大消息积压",
        description = "Agent 消息队列最大积压数量，超过此数视为上下文异常",
        defaultValue = "50", valueType = "int", group = "agent",
        unit = "条", min = 1, max = 1000, requireRestart = true
    )
    public static final String AGENT_MAX_MESSAGE_BACKLOG = "agent.max-message-backlog";

    @SystemConstantDef(
        key = "agent.max-idle-minutes",
        name = "最大空闲时间",
        description = "Agent 最大无响应时间，超过视为上下文失效",
        defaultValue = "30", valueType = "int", group = "agent",
        unit = "分钟", min = 1, max = 1440
    )
    public static final String AGENT_MAX_IDLE_MINUTES = "agent.max-idle-minutes";

    @SystemConstantDef(
        key = "agent.max-recovery-attempts",
        name = "最大恢复尝试",
        description = "上下文恢复最大尝试次数，超过则停止 Agent",
        defaultValue = "3", valueType = "int", group = "agent",
        unit = "次", min = 1, max = 10
    )
    public static final String AGENT_MAX_RECOVERY_ATTEMPTS = "agent.max-recovery-attempts";

    @SystemConstantDef(
        key = "agent.task-queue-size",
        name = "任务队列大小",
        description = "Agent 任务队列最大容量",
        defaultValue = "500", valueType = "int", group = "agent",
        unit = "条", min = 10, max = 5000, requireRestart = true
    )
    public static final String AGENT_TASK_QUEUE_SIZE = "agent.task-queue-size";

    @SystemConstantDef(
        key = "agent.message-queue-size",
        name = "消息队列大小",
        description = "Agent 消息队列最大容量",
        defaultValue = "1000", valueType = "int", group = "agent",
        unit = "条", min = 10, max = 10000, requireRestart = true
    )
    public static final String AGENT_MESSAGE_QUEUE_SIZE = "agent.message-queue-size";

    @SystemConstantDef(
        key = "agent.max-retry-count",
        name = "最大重试次数",
        description = "任务/消息失败后最大重试次数",
        defaultValue = "3", valueType = "int", group = "agent",
        unit = "次", min = 0, max = 10
    )
    public static final String AGENT_MAX_RETRY_COUNT = "agent.max-retry-count";

    @SystemConstantDef(
        key = "agent.history-size",
        name = "历史记录大小",
        description = "任务/消息历史保留数量",
        defaultValue = "1000", valueType = "int", group = "agent",
        unit = "条", min = 100, max = 10000
    )
    public static final String AGENT_HISTORY_SIZE = "agent.history-size";

    @SystemConstantDef(
        key = "agent.scheduler-interval-ms",
        name = "调度间隔",
        description = "Agent 调度器执行间隔",
        defaultValue = "300000", valueType = "long", group = "agent",
        unit = "毫秒", min = 10000, max = 3600000
    )
    public static final String AGENT_SCHEDULER_INTERVAL_MS = "agent.scheduler-interval-ms";

    @SystemConstantDef(
        key = "agent.max-warnings",
        name = "最大警告次数",
        description = "绩效管理最大警告次数，超过触发解雇流程",
        defaultValue = "3", valueType = "int", group = "agent",
        unit = "次", min = 1, max = 10
    )
    public static final String AGENT_MAX_WARNINGS = "agent.max-warnings";

    @SystemConstantDef(
        key = "agent.max-learned-knowledge",
        name = "最大知识条数",
        description = "Agent 最大学习知识数量",
        defaultValue = "100", valueType = "int", group = "agent",
        unit = "条", min = 10, max = 1000
    )
    public static final String AGENT_MAX_LEARNED_KNOWLEDGE = "agent.max-learned-knowledge";

    // ===== 文件相关 =====

    @SystemConstantDef(
        key = "file.max-size-mb",
        name = "单文件大小限制",
        description = "单个文件最大上传大小",
        defaultValue = "50", valueType = "int", group = "file",
        unit = "MB", min = 1, max = 500
    )
    public static final String FILE_MAX_SIZE_MB = "file.max-size-mb";

    @SystemConstantDef(
        key = "file.quota-mb",
        name = "Agent 存储配额",
        description = "每个 Agent 的文件存储配额",
        defaultValue = "500", valueType = "int", group = "file",
        unit = "MB", min = 10, max = 5000
    )
    public static final String FILE_QUOTA_MB = "file.quota-mb";

    @SystemConstantDef(
        key = "file.storage-dir",
        name = "存储目录",
        description = "文件存储根目录",
        defaultValue = "data/files", valueType = "string", group = "file",
        requireRestart = true
    )
    public static final String FILE_STORAGE_DIR = "file.storage-dir";

    // ===== 安全相关 =====

    @SystemConstantDef(
        key = "security.max-login-attempts",
        name = "最大登录尝试",
        description = "登录失败最大尝试次数，超过锁定账户",
        defaultValue = "5", valueType = "int", group = "security",
        unit = "次", min = 1, max = 20
    )
    public static final String SECURITY_MAX_LOGIN_ATTEMPTS = "security.max-login-attempts";

    @SystemConstantDef(
        key = "security.jwt-expiration-ms",
        name = "JWT 过期时间",
        description = "JWT Token 有效期",
        defaultValue = "86400000", valueType = "long", group = "security",
        unit = "毫秒", min = 3600000, max = 604800000
    )
    public static final String SECURITY_JWT_EXPIRATION_MS = "security.jwt-expiration-ms";

    @SystemConstantDef(
        key = "security.session-timeout-minutes",
        name = "会话超时",
        description = "Web 会话超时时间",
        defaultValue = "30", valueType = "int", group = "security",
        unit = "分钟", min = 5, max = 480
    )
    public static final String SECURITY_SESSION_TIMEOUT = "security.session-timeout-minutes";

    // ===== 限流相关 =====

    @SystemConstantDef(
        key = "rate-limit.global",
        name = "全局限流",
        description = "全局 API 每分钟请求限制",
        defaultValue = "120", valueType = "int", group = "rate-limit",
        unit = "次/分钟", min = 10, max = 1000
    )
    public static final String RATE_LIMIT_GLOBAL = "rate-limit.global";

    @SystemConstantDef(
        key = "rate-limit.auth",
        name = "认证限流",
        description = "登录接口每分钟请求限制",
        defaultValue = "10", valueType = "int", group = "rate-limit",
        unit = "次/分钟", min = 1, max = 100
    )
    public static final String RATE_LIMIT_AUTH = "rate-limit.auth";

    @SystemConstantDef(
        key = "rate-limit.write",
        name = "写操作限流",
        description = "写操作每分钟请求限制",
        defaultValue = "60", valueType = "int", group = "rate-limit",
        unit = "次/分钟", min = 5, max = 500
    )
    public static final String RATE_LIMIT_WRITE = "rate-limit.write";

    // ===== 性能相关 =====

    @SystemConstantDef(
        key = "performance.max-backups",
        name = "最大备份数",
        description = "文件备份最大保留数量",
        defaultValue = "10", valueType = "int", group = "performance",
        unit = "份", min = 1, max = 100
    )
    public static final String PERFORMANCE_MAX_BACKUPS = "performance.max-backups";

    @SystemConstantDef(
        key = "performance.max-output-size",
        name = "最大输出大小",
        description = "沙箱执行最大输出大小",
        defaultValue = "1048576", valueType = "long", group = "performance",
        unit = "字节", min = 1024, max = 10485760
    )
    public static final String PERFORMANCE_MAX_OUTPUT_SIZE = "performance.max-output-size";

    @SystemConstantDef(
        key = "performance.ws-session-timeout",
        name = "WebSocket 超时",
        description = "终端 WebSocket 会话超时时间",
        defaultValue = "1800000", valueType = "long", group = "performance",
        unit = "毫秒", min = 60000, max = 86400000
    )
    public static final String PERFORMANCE_WS_SESSION_TIMEOUT = "performance.ws-session-timeout";

    @SystemConstantDef(
        key = "performance.context-compact-threshold",
        name = "上下文压缩阈值",
        description = "触发自动压缩的消息数",
        defaultValue = "50", valueType = "int", group = "performance",
        unit = "条", min = 10, max = 500
    )
    public static final String PERFORMANCE_CONTEXT_COMPACT_THRESHOLD = "performance.context-compact-threshold";

    // ===== 通知相关 =====

    @SystemConstantDef(
        key = "notification.batch-size",
        name = "批量通知大小",
        description = "批量发送通知的批次大小",
        defaultValue = "100", valueType = "int", group = "notification",
        unit = "条", min = 10, max = 1000
    )
    public static final String NOTIFICATION_BATCH_SIZE = "notification.batch-size";

    @SystemConstantDef(
        key = "notification.verify-throttle-per-hour",
        name = "验证通知每小时上限",
        description = "游戏验证相关通知每小时最大发送次数",
        defaultValue = "2", valueType = "int", group = "notification",
        unit = "次/小时", min = 1, max = 10
    )
    public static final String NOTIFICATION_VERIFY_THROTTLE_PER_HOUR = "notification.verify-throttle-per-hour";

    // ===== MCP 相关 =====

    @SystemConstantDef(
        key = "mcp.discovery-timeout-ms",
        name = "MCP 发现超时",
        description = "MCP 工具发现超时时间",
        defaultValue = "10000", valueType = "long", group = "mcp",
        unit = "毫秒", min = 1000, max = 60000
    )
    public static final String MCP_DISCOVERY_TIMEOUT_MS = "mcp.discovery-timeout-ms";

    // ===== 文件收发相关 =====

    @SystemConstantDef(
        key = "file.version-limit",
        name = "版本保留数",
        description = "同名文件保留的最大版本数",
        defaultValue = "10", valueType = "int", group = "file",
        unit = "个", min = 1, max = 100
    )
    public static final String FILE_VERSION_LIMIT = "file.version-limit";

    // ===== 版本迭代相关 =====

    @SystemConstantDef(
        key = "version.pass-score",
        name = "版本验收通过分数",
        description = "版本质量评估通过的最低分数（1-10分）",
        defaultValue = "7", valueType = "int", group = "version",
        unit = "分", min = 1, max = 10
    )
    public static final String VERSION_PASS_SCORE = "version.pass-score";

    @SystemConstantDef(
        key = "version.max-iterations",
        name = "最大迭代次数",
        description = "项目最大版本迭代次数，防止无限循环",
        defaultValue = "10", valueType = "int", group = "version",
        unit = "次", min = 1, max = 100
    )
    public static final String VERSION_MAX_ITERATIONS = "version.max-iterations";

    @SystemConstantDef(
        key = "version.min-iterations",
        name = "最小迭代次数",
        description = "项目最少版本迭代次数，不可低于此数",
        defaultValue = "1", valueType = "int", group = "version",
        unit = "次", min = 1, max = 50
    )
    public static final String VERSION_MIN_ITERATIONS = "version.min-iterations";

    @SystemConstantDef(
        key = "version.iteration-strategy",
        name = "迭代策略",
        description = "版本迭代策略：incremental（增量迭代）、full（全量迭代）、adaptive（自适应迭代）",
        defaultValue = "adaptive", valueType = "string", group = "version"
    )
    public static final String VERSION_ITERATION_STRATEGY = "version.iteration-strategy";

    @SystemConstantDef(
        key = "version.iteration-timeout-hours",
        name = "迭代超时时间",
        description = "版本迭代超时告警时间（小时），超过此时间未完成则告警",
        defaultValue = "72", valueType = "int", group = "version",
        unit = "小时", min = 1, max = 720
    )
    public static final String VERSION_ITERATION_TIMEOUT_HOURS = "version.iteration-timeout-hours";

    @SystemConstantDef(
        key = "version.max-evaluation-history",
        name = "最大评估历史数",
        description = "保留的最大版本评估历史记录数",
        defaultValue = "100", valueType = "int", group = "version",
        unit = "条", min = 10, max = 1000
    )
    public static final String VERSION_MAX_EVALUATION_HISTORY = "version.max-evaluation-history";

    // ===== 督查规则 =====

    @SystemConstantDef(
        key = "supervision.iteration.timeout.hours",
        name = "迭代超时阈值",
        description = "版本迭代超过此时间视为超时，触发预警",
        defaultValue = "72", valueType = "int", group = "supervision",
        unit = "小时", min = 12, max = 720
    )
    public static final String SUPERVISION_ITERATION_TIMEOUT_HOURS = "supervision.iteration.timeout.hours";

    @SystemConstantDef(
        key = "supervision.task.overdue.threshold.hours",
        name = "任务逾期阈值",
        description = "任务超过预估工时此时间后视为逾期",
        defaultValue = "24", valueType = "int", group = "supervision",
        unit = "小时", min = 1, max = 168
    )
    public static final String SUPERVISION_TASK_OVERDUE_THRESHOLD_HOURS = "supervision.task.overdue.threshold.hours";

    @SystemConstantDef(
        key = "supervision.quality.score.threshold",
        name = "质量评分阈值",
        description = "迭代评分低于此值视为质量不达标",
        defaultValue = "6", valueType = "int", group = "supervision",
        unit = "分", min = 1, max = 10
    )
    public static final String SUPERVISION_QUALITY_SCORE_THRESHOLD = "supervision.quality.score.threshold";

    @SystemConstantDef(
        key = "supervision.rollback.rate.threshold",
        name = "回滚率阈值",
        description = "版本回滚率超过此百分比视为异常",
        defaultValue = "30", valueType = "int", group = "supervision",
        unit = "%", min = 5, max = 100
    )
    public static final String SUPERVISION_ROLLBACK_RATE_THRESHOLD = "supervision.rollback.rate.threshold";

    @SystemConstantDef(
        key = "supervision.agent.idle.threshold.hours",
        name = "Agent空闲阈值",
        description = "Agent无活动超过此时间视为空闲",
        defaultValue = "4", valueType = "int", group = "supervision",
        unit = "小时", min = 1, max = 48
    )
    public static final String SUPERVISION_AGENT_IDLE_THRESHOLD_HOURS = "supervision.agent.idle.threshold.hours";

    @SystemConstantDef(
        key = "supervision.alert.email.enabled",
        name = "邮件预警开关",
        description = "是否通过邮件发送督查预警",
        defaultValue = "false", valueType = "boolean", group = "supervision"
    )
    public static final String SUPERVISION_ALERT_EMAIL_ENABLED = "supervision.alert.email.enabled";

    @SystemConstantDef(
        key = "supervision.alert.feishu.enabled",
        name = "飞书预警开关",
        description = "是否通过飞书发送督查预警",
        defaultValue = "true", valueType = "boolean", group = "supervision"
    )
    public static final String SUPERVISION_ALERT_FEISHU_ENABLED = "supervision.alert.feishu.enabled";

    // ===== 上下文/Token 优化相关 =====

    @SystemConstantDef(
        key = "agent.context-window-size",
        name = "对话窗口大小",
        description = "保留的最大对话消息数，超出部分自动压缩为摘要",
        defaultValue = "50", valueType = "int", group = "agent",
        unit = "条", min = 10, max = 500
    )
    public static final String AGENT_CONTEXT_WINDOW_SIZE = "agent.context-window-size";

    @SystemConstantDef(
        key = "agent.token-budget-daily",
        name = "每日Token预算",
        description = "每个Agent每日Token消耗上限，0表示不限制",
        defaultValue = "0", valueType = "long", group = "agent",
        unit = "tokens", min = 0, max = 100000000
    )
    public static final String AGENT_TOKEN_BUDGET_DAILY = "agent.token-budget-daily";

    @SystemConstantDef(
        key = "agent.token-budget-alert-threshold",
        name = "Token预算告警阈值",
        description = "Token消耗达到预算的此百分比时触发告警",
        defaultValue = "80", valueType = "int", group = "agent",
        unit = "%", min = 10, max = 100
    )
    public static final String AGENT_TOKEN_BUDGET_ALERT_THRESHOLD = "agent.token-budget-alert-threshold";

    @SystemConstantDef(
        key = "agent.capability-prompt-cache-ttl",
        name = "能力Prompt缓存时间",
        description = "能力列表Prompt缓存有效期，避免每次调用都重建",
        defaultValue = "300", valueType = "int", group = "agent",
        unit = "秒", min = 10, max = 3600
    )
    public static final String AGENT_CAPABILITY_PROMPT_CACHE_TTL = "agent.capability-prompt-cache-ttl";

    @SystemConstantDef(
        key = "agent.collaboration-context-cache-ttl",
        name = "协作上下文缓存时间",
        description = "协作上下文缓存有效期，避免每次调用都重建",
        defaultValue = "60", valueType = "int", group = "agent",
        unit = "秒", min = 10, max = 300
    )
    public static final String AGENT_COLLABORATION_CONTEXT_CACHE_TTL = "agent.collaboration-context-cache-ttl";

    @SystemConstantDef(
        key = "agent.message-dedup-window-seconds",
        name = "消息去重窗口",
        description = "相同来源和类型的连续消息在此时间窗口内合并",
        defaultValue = "30", valueType = "int", group = "agent",
        unit = "秒", min = 5, max = 300
    )
    public static final String AGENT_MESSAGE_DEDUP_WINDOW_SECONDS = "agent.message-dedup-window-seconds";

    @SystemConstantDef(
        key = "agent.collaboration-context-inject",
        name = "协作上下文注入开关",
        description = "是否在Agent发送消息时自动注入团队协作上下文",
        defaultValue = "true", valueType = "boolean", group = "agent"
    )
    public static final String AGENT_COLLABORATION_CONTEXT_INJECT = "agent.collaboration-context-inject";

    @SystemConstantDef(
        key = "context.compact-token-threshold",
        name = "Token压缩触发阈值",
        description = "会话累计Token超过此值时自动触发上下文压缩",
        defaultValue = "80000", valueType = "int", group = "performance",
        unit = "tokens", min = 10000, max = 500000
    )
    public static final String CONTEXT_COMPACT_TOKEN_THRESHOLD = "context.compact-token-threshold";

    @SystemConstantDef(
        key = "agent.token-alert-per-call",
        name = "单次调用Token告警阈值",
        description = "单次AI调用Token超过此值时记录告警日志",
        defaultValue = "10000", valueType = "int", group = "agent",
        unit = "tokens", min = 1000, max = 500000
    )
    public static final String AGENT_TOKEN_ALERT_PER_CALL = "agent.token-alert-per-call";

    @SystemConstantDef(
        key = "context.recovery-max-length",
        name = "上下文恢复最大长度",
        description = "上下文恢复Prompt最大字符数，超出部分截断",
        defaultValue = "3000", valueType = "int", group = "performance",
        unit = "字符", min = 500, max = 10000
    )
    public static final String CONTEXT_RECOVERY_MAX_LENGTH = "context.recovery-max-length";

    @SystemConstantDef(
        key = "context.collaboration-max-length",
        name = "协作上下文最大长度",
        description = "注入到Agent Prompt中的协作上下文最大字符数",
        defaultValue = "2000", valueType = "int", group = "performance",
        unit = "字符", min = 500, max = 5000
    )
    public static final String CONTEXT_COLLABORATION_MAX_LENGTH = "context.collaboration-max-length";

    // ===== 检查点相关 =====

    @SystemConstantDef(
        key = "checkpoint.auto-create-threshold",
        name = "自动创建检查点阈值",
        description = "对话消息数达到此值时自动创建检查点",
        defaultValue = "30", valueType = "int", group = "checkpoint",
        unit = "条", min = 5, max = 200
    )
    public static final String CHECKPOINT_AUTO_CREATE_THRESHOLD = "checkpoint.auto-create-threshold";

    @SystemConstantDef(
        key = "checkpoint.max-keep-count",
        name = "最大检查点保留数",
        description = "每个 Agent 最多保留的检查点数量",
        defaultValue = "10", valueType = "int", group = "checkpoint",
        unit = "个", min = 1, max = 50
    )
    public static final String CHECKPOINT_MAX_KEEP_COUNT = "checkpoint.max-keep-count";

    @SystemConstantDef(
        key = "checkpoint.token-budget",
        name = "检查点注入Token预算",
        description = "检查点内容注入到上下文的最大Token数",
        defaultValue = "4000", valueType = "int", group = "checkpoint",
        unit = "tokens", min = 500, max = 20000
    )
    public static final String CHECKPOINT_TOKEN_BUDGET = "checkpoint.token-budget";

    // ===== 工具结果压缩相关 =====

    @SystemConstantDef(
        key = "compactor.enabled",
        name = "工具结果压缩开关",
        description = "是否启用工具结果微压缩",
        defaultValue = "true", valueType = "boolean", group = "compactor"
    )
    public static final String COMPACTOR_ENABLED = "compactor.enabled";

    @SystemConstantDef(
        key = "compactor.read-max-chars",
        name = "read结果最大字符数",
        description = "文件读取结果超过此字符数时压缩",
        defaultValue = "2000", valueType = "int", group = "compactor",
        unit = "字符", min = 500, max = 10000
    )
    public static final String COMPACTOR_READ_MAX_CHARS = "compactor.read-max-chars";

    // ===== Dream 知识提取相关 =====

    @SystemConstantDef(
        key = "dream.auto-trigger",
        name = "Dream自动触发开关",
        description = "里程碑完成时是否自动触发知识提取",
        defaultValue = "true", valueType = "boolean", group = "dream"
    )
    public static final String DREAM_AUTO_TRIGGER = "dream.auto-trigger";

    @SystemConstantDef(
        key = "dream.max-extract-items",
        name = "Dream最大提取条目",
        description = "单次知识提取的最大条目数",
        defaultValue = "20", valueType = "int", group = "dream",
        unit = "条", min = 5, max = 100
    )
    public static final String DREAM_MAX_EXTRACT_ITEMS = "dream.max-extract-items";

    // ===== 裁判评估相关 =====

    @SystemConstantDef(
        key = "goal.judge.enabled",
        name = "裁判验证开关",
        description = "是否启用独立裁判验证目标完成",
        defaultValue = "true", valueType = "boolean", group = "goal"
    )
    public static final String GOAL_JUDGE_ENABLED = "goal.judge.enabled";

    @SystemConstantDef(
        key = "goal.judge.max-react",
        name = "裁判最大重评估次数",
        description = "裁判判定未通过后的最大重评估次数",
        defaultValue = "3", valueType = "int", group = "goal",
        unit = "次", min = 0, max = 10
    )
    public static final String GOAL_JUDGE_MAX_REACT = "goal.judge.max-react";

    // ===== 子代理相关 =====

    @SystemConstantDef(
        key = "subagent.max-concurrent",
        name = "最大并发子代理数",
        description = "每个父代理最多同时运行的子代理数量",
        defaultValue = "5", valueType = "int", group = "subagent",
        unit = "个", min = 1, max = 20
    )
    public static final String SUBAGENT_MAX_CONCURRENT = "subagent.max-concurrent";

    @SystemConstantDef(
        key = "subagent.timeout-minutes",
        name = "子代理超时时间",
        description = "子代理运行超过此时间自动终止",
        defaultValue = "30", valueType = "int", group = "subagent",
        unit = "分钟", min = 5, max = 180
    )
    public static final String SUBAGENT_TIMEOUT_MINUTES = "subagent.timeout-minutes";

    // ===== Distill 工作流发现相关 =====

    @SystemConstantDef(
        key = "distill.max-extract-items",
        name = "Distill最大提取条目",
        description = "单次工作流发现的最大 Skill 数量",
        defaultValue = "10", valueType = "int", group = "distill",
        unit = "条", min = 1, max = 50
    )
    public static final String DISTILL_MAX_EXTRACT_ITEMS = "distill.max-extract-items";

    @SystemConstantDef(
        key = "distill.min-repeat-count",
        name = "Distill最小重复次数",
        description = "工作流模式至少重复出现此次数才认为可自动化",
        defaultValue = "2", valueType = "int", group = "distill",
        unit = "次", min = 2, max = 10
    )
    public static final String DISTILL_MIN_REPEAT_COUNT = "distill.min-repeat-count";

    // ===== 快照回滚相关 =====

    @SystemConstantDef(
        key = "snapshot.retention-days",
        name = "快照保留天数",
        description = "快照保留的最大天数，超过自动清理",
        defaultValue = "7", valueType = "int", group = "snapshot",
        unit = "天", min = 1, max = 30
    )
    public static final String SNAPSHOT_RETENTION_DAYS = "snapshot.retention-days";

    @SystemConstantDef(
        key = "snapshot.max-size-mb",
        name = "快照最大总大小",
        description = "每个 Agent 的快照最大总大小",
        defaultValue = "2", valueType = "int", group = "snapshot",
        unit = "MB", min = 1, max = 50
    )
    public static final String SNAPSHOT_MAX_SIZE_MB = "snapshot.max-size-mb";

    // ===== 任务门禁相关 =====

    @SystemConstantDef(
        key = "task-gate.enabled",
        name = "任务门禁开关",
        description = "是否启用任务门禁检查",
        defaultValue = "true", valueType = "boolean", group = "task-gate"
    )
    public static final String TASK_GATE_ENABLED = "task-gate.enabled";

    @SystemConstantDef(
        key = "task-gate.max-react-main",
        name = "主会话最大门禁反应次数",
        description = "主会话门禁最大拦截次数，超过放行",
        defaultValue = "3", valueType = "int", group = "task-gate",
        unit = "次", min = 0, max = 10
    )
    public static final String TASK_GATE_MAX_REACT_MAIN = "task-gate.max-react-main";

    @SystemConstantDef(
        key = "task-gate.max-react-subagent",
        name = "子代理最大门禁反应次数",
        description = "子代理门禁最大拦截次数，超过放行",
        defaultValue = "2", valueType = "int", group = "task-gate",
        unit = "次", min = 0, max = 5
    )
    public static final String TASK_GATE_MAX_REACT_SUBAGENT = "task-gate.max-react-subagent";

    // ===== 预算上下文注入相关 =====

    @SystemConstantDef(
        key = "context.budgeted-injection",
        name = "预算注入开关",
        description = "是否启用预算上下文注入（替代固定截断）",
        defaultValue = "true", valueType = "boolean", group = "context"
    )
    public static final String CONTEXT_BUDGETED_INJECTION = "context.budgeted-injection";

    @SystemConstantDef(
        key = "context.total-token-budget",
        name = "上下文总Token预算",
        description = "预算注入的总 Token 数",
        defaultValue = "8000", valueType = "int", group = "context",
        unit = "tokens", min = 2000, max = 50000
    )
    public static final String CONTEXT_TOTAL_TOKEN_BUDGET = "context.total-token-budget";

    // ===== 会话分叉相关 =====

    @SystemConstantDef(
        key = "fork.max-per-agent",
        name = "最大分叉数",
        description = "每个 Agent 最多同时存在的分叉数",
        defaultValue = "5", valueType = "int", group = "fork",
        unit = "个", min = 1, max = 20
    )
    public static final String FORK_MAX_PER_AGENT = "fork.max-per-agent";

    @SystemConstantDef(
        key = "fork.default-strategy",
        name = "默认合并策略",
        description = "分叉合并时的默认策略：replace（替换）、append（追加）、merge（智能合并）",
        defaultValue = "merge", valueType = "string", group = "fork"
    )
    public static final String FORK_DEFAULT_STRATEGY = "fork.default-strategy";
}
