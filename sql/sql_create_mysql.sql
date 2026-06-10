-- ============================================
-- ChengXun Game Maker 数据库建表脚本 (MySQL)
-- 版本: 1.0.0
-- 创建时间: 2026-05-30
-- 说明: 适用于 MySQL 8.0+ 数据库
-- 使用方式: mysql -u root -p < sql_create_mysql.sql
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS game_maker
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE game_maker;

-- ============================================
-- 1. 基础表结构
-- ============================================

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description VARCHAR(255),
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_roles_name (name),
    INDEX idx_roles_is_system (is_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 角色权限表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission),
    INDEX idx_role_permissions_role_id (role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限表';

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role_id BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    must_change_password BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_status (status),
    INDEX idx_users_role_id (role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_group VARCHAR(50),
    value_type VARCHAR(20) DEFAULT 'string',
    description VARCHAR(255),
    is_system_builtin BOOLEAN DEFAULT FALSE,
    project_id VARCHAR(100) DEFAULT NULL COMMENT '所属项目 ID（null=全局默认）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_system_configs_key (config_key),
    INDEX idx_system_configs_group (config_group),
    INDEX idx_system_configs_builtin (is_system_builtin),
    INDEX idx_config_project (project_id),
    UNIQUE KEY uk_config_key_project (config_key, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 2. 日志和审计表
-- ============================================

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100) NOT NULL,
    method VARCHAR(10),
    url VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_params TEXT,
    response_status INT,
    execution_time BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_logs_user_id (user_id),
    INDEX idx_operation_logs_operation (operation),
    INDEX idx_operation_logs_created_at (created_at),
    INDEX idx_operation_logs_created_user (created_at, user_id),
    INDEX idx_operation_logs_operation_created (operation, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- Agent 日志表
CREATE TABLE IF NOT EXISTS agent_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50),
    agent_name VARCHAR(100),
    action VARCHAR(50),
    level VARCHAR(20) DEFAULT 'INFO',
    summary VARCHAR(200),
    detail TEXT,
    project_id VARCHAR(100),
    task_id VARCHAR(100),
    decision TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_logs_agent_id (agent_id),
    INDEX idx_agent_logs_action (action),
    INDEX idx_agent_logs_level (level),
    INDEX idx_agent_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent日志表';

-- ============================================
-- 3. Agent 相关表
-- ============================================

-- Agent 性能表
CREATE TABLE IF NOT EXISTS agent_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    project_id VARCHAR(100),

    -- 任务统计
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    failed_tasks INT DEFAULT 0,
    in_progress_tasks INT DEFAULT 0,

    -- 效率指标
    avg_completion_time_ms BIGINT DEFAULT 0,
    min_completion_time_ms BIGINT DEFAULT 0,
    max_completion_time_ms BIGINT DEFAULT 0,

    -- 质量指标
    avg_quality_score DOUBLE DEFAULT 0.0,
    max_quality_score DOUBLE DEFAULT 0.0,
    min_quality_score DOUBLE DEFAULT 0.0,
    review_pass_rate DOUBLE DEFAULT 0.0,

    -- 负载指标
    current_load INT DEFAULT 0,
    avg_load DOUBLE DEFAULT 0.0,

    -- 综合评分
    overall_score DOUBLE DEFAULT 0.0,
    reliability_score DOUBLE DEFAULT 0.0,
    efficiency_score DOUBLE DEFAULT 0.0,

    -- 时间戳
    first_task_at TIMESTAMP NULL,
    last_task_at TIMESTAMP NULL,
    last_evaluated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_perf_agent_id (agent_id),
    INDEX idx_perf_role (agent_role),
    INDEX idx_perf_updated (updated_at),
    INDEX idx_perf_project (project_id),
    UNIQUE KEY uk_perf_project_agent (project_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent性能表';

-- Agent 健康表
CREATE TABLE IF NOT EXISTS agent_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    project_id VARCHAR(100) DEFAULT NULL COMMENT '所属项目 ID',
    health_status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    avg_response_time_ms BIGINT,
    max_response_time_ms BIGINT,
    total_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    error_rate DOUBLE DEFAULT 0.0,
    memory_usage_mb BIGINT,
    cpu_usage_percent DOUBLE,
    active_tasks INT DEFAULT 0,
    completed_tasks BIGINT DEFAULT 0,
    task_completion_rate DOUBLE DEFAULT 0.0,
    last_activity_time TIMESTAMP NULL,
    last_error_time TIMESTAMP NULL,
    last_error_message TEXT,
    consecutive_errors INT DEFAULT 0,
    uptime_seconds BIGINT DEFAULT 0,
    check_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_health_agent (agent_id),
    INDEX idx_health_status (health_status),
    INDEX idx_health_time (check_time),
    INDEX idx_health_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent健康表';

-- Agent 干预表（新版）
CREATE TABLE IF NOT EXISTS agent_interventions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intervention_no VARCHAR(50) UNIQUE,

    -- Agent 信息
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    project_id VARCHAR(100),

    -- 干预人信息
    user_id BIGINT,
    username VARCHAR(50),
    user_role VARCHAR(20),

    -- 干预内容
    intervention_type VARCHAR(50) NOT NULL,
    reason TEXT,
    instruction TEXT,
    original_decision TEXT,
    new_decision TEXT,
    task_id VARCHAR(100),
    message_id VARCHAR(100),

    -- 执行状态
    status VARCHAR(20) DEFAULT 'PENDING',
    acknowledged_at TIMESTAMP NULL,
    acknowledgement TEXT,
    executed_at TIMESTAMP NULL,
    execution_result TEXT,

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_intervention_agent (agent_id),
    INDEX idx_intervention_user (user_id),
    INDEX idx_intervention_type (intervention_type),
    INDEX idx_intervention_status (status),
    INDEX idx_intervention_created (created_at),
    INDEX idx_intervention_no (intervention_no),
    INDEX idx_intervention_agent_status (agent_id, status),
    INDEX idx_intervention_created_type (created_at, intervention_type),
    INDEX idx_intervention_operator (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent干预表';

-- 干预记录表（旧版，保留向后兼容）
CREATE TABLE IF NOT EXISTS interventions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    urgency VARCHAR(20) DEFAULT 'NORMAL',
    duration VARCHAR(20) DEFAULT 'ONCE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP NULL,

    INDEX idx_interventions_agent_id (agent_id),
    INDEX idx_interventions_user_id (user_id),
    INDEX idx_interventions_processed (processed),
    INDEX idx_interventions_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='干预记录表（旧版）';

-- ============================================
-- 4. 绩效管理表
-- ============================================

-- 绩效评审表
CREATE TABLE IF NOT EXISTS performance_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    producer_id VARCHAR(50),
    producer_name VARCHAR(100),
    project_id VARCHAR(100),
    project_name VARCHAR(100),
    review_period VARCHAR(50),
    quality_score INT,
    efficiency_score INT,
    collaboration_score INT,
    innovation_score INT,
    overall_score DOUBLE,
    strengths TEXT,
    improvements TEXT,
    comments TEXT,
    highlights TEXT,
    is_warning BOOLEAN DEFAULT FALSE,
    warning_level INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_perf_review_agent (agent_id),
    INDEX idx_perf_review_producer (producer_id),
    INDEX idx_perf_review_period (review_period),
    INDEX idx_perf_review_score (overall_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效评审表';

-- 解雇申请表
CREATE TABLE IF NOT EXISTS dismissal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    producer_id VARCHAR(50),
    producer_name VARCHAR(100),
    reason TEXT NOT NULL,
    warning_count INT DEFAULT 0,
    consecutive_low_periods INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewer_name VARCHAR(50),
    reviewed_at TIMESTAMP NULL,
    review_comments TEXT,
    executed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dismissal_agent (agent_id),
    INDEX idx_dismissal_status (status),
    INDEX idx_dismissal_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='解雇申请表';

-- 制作人替换表
CREATE TABLE IF NOT EXISTS producer_replacements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_producer_id VARCHAR(50) NOT NULL,
    old_producer_name VARCHAR(100),
    new_producer_id VARCHAR(50),
    new_producer_name VARCHAR(100),
    project_id VARCHAR(100),
    project_name VARCHAR(100),
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewer_name VARCHAR(50),
    reviewed_at TIMESTAMP NULL,
    executed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_replacement_old (old_producer_id),
    INDEX idx_replacement_new (new_producer_id),
    INDEX idx_replacement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='制作人替换表';

-- ============================================
-- 5. 招聘管理表
-- ============================================

-- 招聘申请表
CREATE TABLE IF NOT EXISTS recruitment_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producer_id VARCHAR(50) NOT NULL,
    producer_name VARCHAR(100),
    project_id VARCHAR(100),
    project_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    role_name VARCHAR(100),
    description TEXT,
    requirements TEXT,
    capabilities TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewer_name VARCHAR(50),
    reviewed_at TIMESTAMP NULL,
    review_comments TEXT,
    created_agent_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recruitment_producer (producer_id),
    INDEX idx_recruitment_role (role),
    INDEX idx_recruitment_status (status),
    INDEX idx_recruitment_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘申请表';

-- ============================================
-- 6. 代码管理表
-- ============================================

-- Git 仓库表
CREATE TABLE IF NOT EXISTS git_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    branch VARCHAR(100) DEFAULT 'main',
    local_path VARCHAR(500),
    project_id VARCHAR(100),
    auto_review_enabled BOOLEAN DEFAULT FALSE,
    last_sync_at TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_git_repositories_name (name),
    INDEX idx_git_repositories_status (status),
    INDEX idx_git_repositories_project (project_id),
    INDEX idx_git_repositories_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Git仓库表';

-- 代码审查表
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    project_id VARCHAR(100),
    git_repository_id BIGINT,
    git_repository_name VARCHAR(100),
    repository_type VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    branch VARCHAR(100),
    commit_hash VARCHAR(100),
    changed_files TEXT,
    diff_content TEXT,
    score INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewer_id BIGINT,
    reviewer_name VARCHAR(50),
    review_comments TEXT,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_review_agent (agent_id),
    INDEX idx_review_project (project_id),
    INDEX idx_review_status (status),
    INDEX idx_review_created (created_at),
    INDEX idx_review_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码审查表';

-- ============================================
-- 7. CICD 和工作流表
-- ============================================

-- 流水线表
CREATE TABLE IF NOT EXISTS pipelines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    project_id VARCHAR(100),
    project_name VARCHAR(100),
    config TEXT,
    status VARCHAR(20) DEFAULT 'IDLE',
    trigger_type VARCHAR(50),
    last_run_at TIMESTAMP NULL,
    last_run_status VARCHAR(20),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pipeline_project (project_id),
    INDEX idx_pipeline_status (status),
    INDEX idx_pipeline_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水线表';

-- 流水线阶段表
CREATE TABLE IF NOT EXISTS pipeline_stages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    stage_order INT NOT NULL,
    stage_type VARCHAR(50),
    config TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_ms BIGINT,
    result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stage_pipeline (pipeline_id),
    INDEX idx_stage_order (pipeline_id, stage_order),
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水线阶段表';

-- ============================================
-- 8. 监控和告警表
-- ============================================

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    metric_name VARCHAR(100) NOT NULL,
    condition_type VARCHAR(20) NOT NULL,
    threshold DECIMAL(20,4) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    rule_type VARCHAR(50),
    rule_category VARCHAR(50) DEFAULT 'SYSTEM',
    cooldown_seconds INT DEFAULT 300,
    notification_channels VARCHAR(200) DEFAULT 'SYSTEM',
    auto_resolve BOOLEAN DEFAULT FALSE,
    severity_level INT DEFAULT 1,
    tags VARCHAR(500),
    duration_seconds INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alert_rules_metric (metric_name),
    INDEX idx_alert_rules_enabled (enabled),
    INDEX idx_alert_rules_category (rule_category),
    INDEX idx_alert_rules_severity (severity_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(100),
    rule_category VARCHAR(50),
    metric_value DECIMAL(20,4),
    threshold DECIMAL(20,4),
    severity_level INT DEFAULT 1,
    priority VARCHAR(20),
    title VARCHAR(200),
    detail TEXT,
    agent_id VARCHAR(50),
    agent_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    resolved_by VARCHAR(50),
    resolution_notes TEXT,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alert_records_rule_id (rule_id),
    INDEX idx_alert_records_status (status),
    INDEX idx_alert_records_created_at (created_at),
    INDEX idx_alert_records_rule_status (rule_id, status),
    INDEX idx_alert_records_created_status (created_at, status),
    INDEX idx_alert_records_category (rule_category),
    INDEX idx_alert_records_severity (severity_level),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

-- 性能指标表
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,4),
    metric_type VARCHAR(50),
    agent_id VARCHAR(100),
    agent_name VARCHAR(100),
    project_id VARCHAR(100),
    api_path VARCHAR(500),
    unit VARCHAR(20),
    tags TEXT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_performance_metrics_name (metric_name),
    INDEX idx_performance_metrics_type (metric_type),
    INDEX idx_performance_metrics_recorded_at (recorded_at),
    INDEX idx_metric_agent (agent_id),
    INDEX idx_metric_created (created_at),
    INDEX idx_performance_metrics_name_time (metric_name, recorded_at),
    INDEX idx_performance_metrics_type_time (metric_type, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='性能指标表';

-- ============================================
-- 9. 通知和消息表
-- ============================================

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(50),
    channel VARCHAR(20) DEFAULT 'SYSTEM',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_created_at (created_at),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 通知模板表
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(200),
    content TEXT NOT NULL,
    description VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    system_builtin BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_channel (channel),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- ============================================
-- 10. Token 和资源管理表
-- ============================================

-- API Token 表
CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100),
    api_key TEXT,
    api_url VARCHAR(500),
    model VARCHAR(100),
    max_tokens INT DEFAULT 4096,
    context_window INT DEFAULT 200000,
    agent_id VARCHAR(50),
    permissions TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_tokens_user_id (user_id),
    INDEX idx_api_tokens_token (token),
    INDEX idx_api_tokens_expires_at (expires_at),
    INDEX idx_api_tokens_token_expires (token, expires_at),
    INDEX idx_api_tokens_user_expires (user_id, expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API Token表';

-- Token 使用统计表
CREATE TABLE IF NOT EXISTS token_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    input_tokens BIGINT DEFAULT 0,
    output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    estimated_cost DECIMAL(10,4) DEFAULT 0.00,
    cost DECIMAL(10,4) DEFAULT 0.00,
    model VARCHAR(100),
    usage_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token_usage_agent_id (agent_id),
    INDEX idx_token_usage_created_at (created_at),
    INDEX idx_token_usage_agent_created (agent_id, created_at),
    INDEX idx_token_usage_model_created (model, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token使用统计表';

-- ============================================
-- 11. 安全和设备管理表
-- ============================================

-- 设备信任表
CREATE TABLE IF NOT EXISTS device_trusts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_name VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_trusts_user_id (user_id),
    INDEX idx_device_trusts_fingerprint (device_fingerprint),
    INDEX idx_device_trusts_expires_at (expires_at),
    INDEX idx_device_trusts_fingerprint_user (device_fingerprint, user_id),
    INDEX idx_device_trusts_expires (expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备信任表';

-- ============================================
-- 12. 知识库和文档表
-- ============================================

-- 文档索引表
CREATE TABLE IF NOT EXISTS document_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    summary VARCHAR(500),
    agent_id VARCHAR(50),
    project_id VARCHAR(100),
    file_path VARCHAR(500),
    tags VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc_type (doc_type),
    INDEX idx_doc_agent (agent_id),
    INDEX idx_doc_project (project_id),
    INDEX idx_doc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档索引表';

-- ============================================
-- 13. Agent 预设表
-- ============================================

-- Agent 预设表（全局 Agent 能力模板）
CREATE TABLE IF NOT EXISTS agent_presets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '预设名称',
    role VARCHAR(50) NOT NULL COMMENT 'Agent 角色',
    description VARCHAR(500) COMMENT '预设描述',
    reasoning_depth INT DEFAULT 3 COMMENT '推理深度 1-5',
    capabilities TEXT COMMENT '能力标签 JSON',
    tags TEXT COMMENT '自定义标签 JSON',
    supported_file_types TEXT COMMENT '支持的文件类型 JSON',
    unsupported_features TEXT COMMENT '不支持的功能 JSON',
    max_context_size INT DEFAULT 100000 COMMENT '最大上下文大小',
    supports_image_generation BOOLEAN DEFAULT FALSE COMMENT '是否支持图片生成',
    supports_code_execution BOOLEAN DEFAULT TRUE COMMENT '是否支持代码执行',
    supports_file_operations BOOLEAN DEFAULT TRUE COMMENT '是否支持文件操作',
    api_provider VARCHAR(50) DEFAULT 'anthropic' COMMENT 'API 提供商',
    prompt TEXT COMMENT '角色系统提示词（完整角色定义）',
    notify_targets VARCHAR(500) DEFAULT 'producer' COMMENT '完成任务后的通知目标角色（逗号分隔）',
    reviewer VARCHAR(50) COMMENT '审查者角色',
    role_name VARCHAR(100) COMMENT '角色中文名称',
    prompt_version INT DEFAULT 0 COMMENT '提示词版本号（每次进化/编辑+1）',
    last_evolution_source VARCHAR(50) COMMENT '上次进化来源：manual/ai/evolution',
    last_evolution_at DATETIME COMMENT '上次进化时间',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统内置',
    source_agent_id VARCHAR(200) COMMENT '来源 Agent ID',
    source_project_id VARCHAR(100) COMMENT '来源项目 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_preset_role (role),
    INDEX idx_preset_system (is_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 预设表';

-- ============================================
-- 14. 项目成员表
-- ============================================

-- 项目成员表（项目级权限控制）
CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL COMMENT '项目 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER' COMMENT '项目角色: OWNER/MANAGER/DEVELOPER/VIEWER',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_user (project_id, user_id),
    INDEX idx_pm_project (project_id),
    INDEX idx_pm_user (user_id),
    INDEX idx_pm_role (role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- ============================================
-- 15. 项目通知配置表
-- ============================================

CREATE TABLE IF NOT EXISTS project_notification_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL COMMENT '项目 ID',
    template_code VARCHAR(50) NOT NULL COMMENT '关联的模板编码',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用该通知',
    channel_override VARCHAR(20) COMMENT '通知渠道覆盖',
    recipients TEXT COMMENT '自定义通知目标 JSON',
    notify_users TEXT COMMENT '自定义通知人 JSON',
    rate_limit_seconds INT DEFAULT 0 COMMENT '通知频率限制（秒）',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_template (project_id, template_code),
    INDEX idx_pnc_project (project_id),
    INDEX idx_pnc_template (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目通知配置表';

-- ============================================
-- 16. 项目 Token 绑定表
-- ============================================

CREATE TABLE IF NOT EXISTS project_token_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL COMMENT '项目 ID',
    token_id BIGINT NOT NULL COMMENT 'Token ID',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为项目默认 Token',
    priority INT DEFAULT 100 COMMENT '优先级',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_token (project_id, token_id),
    INDEX idx_ptb_project (project_id),
    INDEX idx_ptb_token (token_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目 Token 绑定表';

-- ============================================
-- 17. 项目告警配置表
-- ============================================

CREATE TABLE IF NOT EXISTS project_alert_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL COMMENT '项目 ID',
    rule_id BIGINT NOT NULL COMMENT '告警规则 ID',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    custom_threshold DOUBLE COMMENT '自定义阈值覆盖',
    custom_priority VARCHAR(20) COMMENT '自定义优先级覆盖',
    custom_duration_seconds INT COMMENT '自定义持续时间覆盖',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_rule (project_id, rule_id),
    INDEX idx_pac_project (project_id),
    INDEX idx_pac_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目告警配置表';

-- ============================================
-- 18. AI助手会话表
-- ============================================

CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    title VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_chat_sessions_user (user_id),
    INDEX idx_chat_sessions_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手会话表';

-- ============================================
-- 19. AI助手消息表
-- ============================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '会话 ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT COMMENT '消息内容',
    thinking TEXT COMMENT '思考过程',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chat_messages_session (session_id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手消息表';

-- ============================================
-- 20. 工作流模板表（用户自定义模板持久化）
-- ============================================

CREATE TABLE IF NOT EXISTS workflow_templates (
    id VARCHAR(64) PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    description VARCHAR(512) COMMENT '模板描述',
    steps_json TEXT COMMENT '步骤定义JSON',
    builtin BOOLEAN DEFAULT FALSE COMMENT '是否为内置模板',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_wf_tpl_builtin (builtin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流模板表';

-- ============================================
-- 21. 游戏模板表（用户自定义模板持久化）
-- ============================================

CREATE TABLE IF NOT EXISTS game_templates (
    id VARCHAR(64) PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    description VARCHAR(512) COMMENT '模板描述',
    game_type VARCHAR(64) COMMENT '游戏类型',
    config_json TEXT COMMENT '模板配置JSON',
    builtin BOOLEAN DEFAULT FALSE COMMENT '是否为内置模板',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_game_tpl_builtin (builtin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏模板表';

-- ============================================
-- 22. 工作流运行时表
-- ============================================

-- 工作流实例表
CREATE TABLE IF NOT EXISTS workflow_instances (
    id VARCHAR(64) PRIMARY KEY COMMENT '实例ID（UUID）',
    template_id VARCHAR(64) NOT NULL COMMENT '关联的模板ID',
    project_id VARCHAR(100) COMMENT '所属项目ID',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED',
    parameters_json TEXT COMMENT '启动参数JSON',
    context_json TEXT COMMENT '全局上下文JSON',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NULL COMMENT '启动时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_wfi_template (template_id),
    INDEX idx_wfi_project (project_id),
    INDEX idx_wfi_status (status),
    INDEX idx_wfi_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流实例表';

-- 工作流步骤执行表
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '执行ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) NOT NULL COMMENT '步骤ID',
    agent_id VARCHAR(100) COMMENT '分配的Agent运行时ID',
    agent_role VARCHAR(50) COMMENT 'Agent角色',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    input_data_json TEXT COMMENT '输入数据JSON',
    output_data_json TEXT COMMENT '输出数据JSON',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '当前重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    timeout_minutes INT DEFAULT 30 COMMENT '超时时间（分钟）',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_wfse_instance (instance_id),
    INDEX idx_wfse_status (status),
    INDEX idx_wfse_agent (agent_id),
    UNIQUE KEY uk_wfse_instance_step (instance_id, step_id),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流步骤执行表';

-- 工作流审批表
CREATE TABLE IF NOT EXISTS workflow_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) NOT NULL COMMENT '关联的步骤ID',
    approver_id BIGINT COMMENT '审批人用户ID',
    approver_name VARCHAR(50) COMMENT '审批人用户名',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    comment TEXT COMMENT '审批意见',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    decided_at TIMESTAMP NULL COMMENT '决定时间',
    INDEX idx_wfa_instance (instance_id),
    INDEX idx_wfa_status (status),
    INDEX idx_wfa_approver (approver_id),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流审批表';

-- 工作流审计日志表
CREATE TABLE IF NOT EXISTS workflow_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) COMMENT '关联的步骤ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    actor_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '操作者类型: SYSTEM/USER/AGENT',
    actor_id VARCHAR(100) COMMENT '操作者ID',
    actor_name VARCHAR(100) COMMENT '操作者名称',
    detail_json TEXT COMMENT '详情JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_wfal_instance (instance_id),
    INDEX idx_wfal_action (action),
    INDEX idx_wfal_actor (actor_type, actor_id),
    INDEX idx_wfal_created (created_at),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流审计日志表';

-- ============================================
-- 完成
-- ============================================
SELECT 'MySQL 数据库建表完成！' AS message;

-- ============================================
-- 项目级Agent配置表（新增）
-- ============================================
CREATE TABLE IF NOT EXISTS project_agent_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    project_id VARCHAR(100) NOT NULL COMMENT '项目ID',
    agent_role VARCHAR(50) NOT NULL COMMENT 'Agent角色',
    custom_system_prompt TEXT COMMENT '自定义系统提示词',
    custom_capability_prompt TEXT COMMENT '自定义能力提示词',
    responsibility_weights TEXT COMMENT '职责权重（JSON格式）',
    project_context TEXT COMMENT '项目特定上下文',
    optimization_suggestions TEXT COMMENT 'AI优化建议',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_pac_project (project_id),
    INDEX idx_pac_agent (project_id, agent_role),
    INDEX idx_pac_active (project_id, agent_role, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目级Agent配置表';

-- ============================================
-- 游戏分析任务表（新增）
-- ============================================
CREATE TABLE IF NOT EXISTS game_analysis_tasks (
    task_id VARCHAR(50) PRIMARY KEY COMMENT '任务ID',
    project_id VARCHAR(100) NOT NULL COMMENT '项目ID',
    project_name VARCHAR(200) COMMENT '项目名称',
    project_dir VARCHAR(500) COMMENT '项目目录',
    project_goal TEXT COMMENT '项目目标',
    requested_by VARCHAR(100) COMMENT '请求者',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING,RUNNING,COMPLETED,FAILED',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    error_message TEXT COMMENT '错误信息',
    overall_score INT COMMENT '综合评分',
    runnable_score INT COMMENT '可运行性评分',
    playable_score INT COMMENT '可玩性评分',
    completeness_score INT COMMENT '完整性评分',
    uiux_score INT COMMENT 'UI/UX评分',
    code_quality_score INT COMMENT '代码质量评分',
    summary TEXT COMMENT '分析摘要',
    strengths_json TEXT COMMENT '优点（JSON格式）',
    issues_json TEXT COMMENT '问题（JSON格式）',
    suggestions_json TEXT COMMENT '建议（JSON格式）',
    INDEX idx_gat_project (project_id),
    INDEX idx_gat_status (status),
    INDEX idx_gat_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏分析任务表';

-- 版本评估持久化表
CREATE TABLE IF NOT EXISTS version_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_id VARCHAR(100) NOT NULL COMMENT '项目ID',
    milestone_id VARCHAR(100) NOT NULL COMMENT '里程碑ID',
    milestone_title VARCHAR(200) COMMENT '里程碑标题',
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    efficiency_score INT DEFAULT 0 COMMENT '效率评分(0-100)',
    quality_score INT DEFAULT 0 COMMENT '质量评分(0-100)',
    overall_score INT DEFAULT 0 COMMENT '综合评分(0-100)',
    missing_roles TEXT COMMENT '缺失角色(JSON数组)',
    redundant_roles TEXT COMMENT '冗余角色(JSON数组)',
    recommendations TEXT COMMENT '建议列表(JSON数组)',
    agent_evaluations_json TEXT COMMENT 'Agent评估详情(JSON)',
    INDEX idx_ve_project (project_id),
    INDEX idx_ve_milestone (milestone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版本评估持久化表';
