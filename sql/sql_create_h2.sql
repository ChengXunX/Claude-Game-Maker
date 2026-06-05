-- ============================================
-- ChengXun Game Maker 数据库建表脚本 (H2)
-- 版本: 1.0.0
-- 创建时间: 2026-05-30
-- 说明: 适用于 H2 数据库（测试环境）
-- 使用方式: 在 Spring Boot 测试中自动执行
-- ============================================

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
CREATE INDEX IF NOT EXISTS idx_roles_is_system ON roles(is_system);

-- 角色权限表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission)
);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_group VARCHAR(50),
    value_type VARCHAR(20) DEFAULT 'string',
    description VARCHAR(255),
    is_system_builtin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_system_configs_key ON system_configs(config_key);
CREATE INDEX IF NOT EXISTS idx_system_configs_group ON system_configs(config_group);
CREATE INDEX IF NOT EXISTS idx_system_configs_builtin ON system_configs(is_system_builtin);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_logs_operation ON operation_logs(operation);
CREATE INDEX IF NOT EXISTS idx_operation_logs_created_at ON operation_logs(created_at);

-- Agent 日志表
CREATE TABLE IF NOT EXISTS agent_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    log_level VARCHAR(20) DEFAULT 'INFO',
    action VARCHAR(100),
    summary VARCHAR(500),
    message TEXT,
    details TEXT,
    project_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_agent_logs_agent_id ON agent_logs(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_logs_log_level ON agent_logs(log_level);
CREATE INDEX IF NOT EXISTS idx_agent_logs_created_at ON agent_logs(created_at);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_perf_agent_id ON agent_performance(agent_id);
CREATE INDEX IF NOT EXISTS idx_perf_role ON agent_performance(agent_role);
CREATE INDEX IF NOT EXISTS idx_perf_updated ON agent_performance(updated_at);

-- Agent 健康表
CREATE TABLE IF NOT EXISTS agent_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_health_agent ON agent_health(agent_id);
CREATE INDEX IF NOT EXISTS idx_health_status ON agent_health(health_status);
CREATE INDEX IF NOT EXISTS idx_health_time ON agent_health(check_time);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_intervention_agent ON agent_interventions(agent_id);
CREATE INDEX IF NOT EXISTS idx_intervention_user ON agent_interventions(user_id);
CREATE INDEX IF NOT EXISTS idx_intervention_type ON agent_interventions(intervention_type);
CREATE INDEX IF NOT EXISTS idx_intervention_status ON agent_interventions(status);
CREATE INDEX IF NOT EXISTS idx_intervention_created ON agent_interventions(created_at);
CREATE INDEX IF NOT EXISTS idx_intervention_no ON agent_interventions(intervention_no);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_interventions_agent_id ON interventions(agent_id);
CREATE INDEX IF NOT EXISTS idx_interventions_user_id ON interventions(user_id);
CREATE INDEX IF NOT EXISTS idx_interventions_processed ON interventions(processed);
CREATE INDEX IF NOT EXISTS idx_interventions_created_at ON interventions(created_at);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_review_agent ON performance_reviews(agent_id);
CREATE INDEX IF NOT EXISTS idx_review_producer ON performance_reviews(producer_id);
CREATE INDEX IF NOT EXISTS idx_review_period ON performance_reviews(review_period);
CREATE INDEX IF NOT EXISTS idx_review_score ON performance_reviews(overall_score);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dismissal_agent ON dismissal_requests(agent_id);
CREATE INDEX IF NOT EXISTS idx_dismissal_status ON dismissal_requests(status);
CREATE INDEX IF NOT EXISTS idx_dismissal_created ON dismissal_requests(created_at);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_replacement_old ON producer_replacements(old_producer_id);
CREATE INDEX IF NOT EXISTS idx_replacement_new ON producer_replacements(new_producer_id);
CREATE INDEX IF NOT EXISTS idx_replacement_status ON producer_replacements(status);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recruitment_producer ON recruitment_requests(producer_id);
CREATE INDEX IF NOT EXISTS idx_recruitment_role ON recruitment_requests(role);
CREATE INDEX IF NOT EXISTS idx_recruitment_status ON recruitment_requests(status);
CREATE INDEX IF NOT EXISTS idx_recruitment_created ON recruitment_requests(created_at);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_git_repositories_name ON git_repositories(name);
CREATE INDEX IF NOT EXISTS idx_git_repositories_status ON git_repositories(status);
CREATE INDEX IF NOT EXISTS idx_git_repositories_project ON git_repositories(project_id);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_review_agent ON code_reviews(agent_id);
CREATE INDEX IF NOT EXISTS idx_review_project ON code_reviews(project_id);
CREATE INDEX IF NOT EXISTS idx_review_status ON code_reviews(status);
CREATE INDEX IF NOT EXISTS idx_review_created ON code_reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_review_score ON code_reviews(score);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pipeline_project ON pipelines(project_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_status ON pipelines(status);
CREATE INDEX IF NOT EXISTS idx_pipeline_created ON pipelines(created_at);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stage_pipeline ON pipeline_stages(pipeline_id);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alert_rules_metric ON alert_rules(metric_name);
CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_alert_rules_category ON alert_rules(rule_category);
CREATE INDEX IF NOT EXISTS idx_alert_rules_severity ON alert_rules(severity_level);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alert_records_rule_id ON alert_records(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_records_status ON alert_records(status);
CREATE INDEX IF NOT EXISTS idx_alert_records_created_at ON alert_records(created_at);
CREATE INDEX IF NOT EXISTS idx_alert_records_category ON alert_records(rule_category);
CREATE INDEX IF NOT EXISTS idx_alert_records_severity ON alert_records(severity_level);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_name ON performance_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_type ON performance_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_recorded_at ON performance_metrics(recorded_at);
CREATE INDEX IF NOT EXISTS idx_metric_agent ON performance_metrics(agent_id);
CREATE INDEX IF NOT EXISTS idx_metric_created ON performance_metrics(created_at);

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
    read_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_code ON notification_templates(template_code);
CREATE INDEX IF NOT EXISTS idx_channel ON notification_templates(channel);
CREATE INDEX IF NOT EXISTS idx_category ON notification_templates(category);
CREATE INDEX IF NOT EXISTS idx_enabled ON notification_templates(enabled);

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
    agent_id VARCHAR(50),
    permissions TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_api_tokens_user_id ON api_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_api_tokens_token ON api_tokens(token);
CREATE INDEX IF NOT EXISTS idx_api_tokens_expires_at ON api_tokens(expires_at);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_token_usage_agent_id ON token_usage(agent_id);
CREATE INDEX IF NOT EXISTS idx_token_usage_created_at ON token_usage(created_at);

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_device_trusts_user_id ON device_trusts(user_id);
CREATE INDEX IF NOT EXISTS idx_device_trusts_fingerprint ON device_trusts(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_device_trusts_expires_at ON device_trusts(expires_at);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_doc_type ON document_index(doc_type);
CREATE INDEX IF NOT EXISTS idx_doc_agent ON document_index(agent_id);
CREATE INDEX IF NOT EXISTS idx_doc_project ON document_index(project_id);
CREATE INDEX IF NOT EXISTS idx_doc_created ON document_index(created_at);

-- ============================================
-- 13. Agent 预设表
-- ============================================

CREATE TABLE IF NOT EXISTS agent_presets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    reasoning_depth INT DEFAULT 3,
    capabilities TEXT,
    tags TEXT,
    supported_file_types TEXT,
    unsupported_features TEXT,
    max_context_size INT DEFAULT 100000,
    supports_image_generation BOOLEAN DEFAULT FALSE,
    supports_code_execution BOOLEAN DEFAULT TRUE,
    supports_file_operations BOOLEAN DEFAULT TRUE,
    api_provider VARCHAR(50) DEFAULT 'anthropic',
    is_system BOOLEAN DEFAULT FALSE,
    source_agent_id VARCHAR(200),
    source_project_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_preset_role ON agent_presets(role);
CREATE INDEX IF NOT EXISTS idx_preset_system ON agent_presets(is_system);

-- ============================================
-- 14. 项目成员表
-- ============================================

CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_user ON project_members(project_id, user_id);
CREATE INDEX IF NOT EXISTS idx_pm_project ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_pm_user ON project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_pm_role ON project_members(role);

-- ============================================
-- 15. 项目通知配置表
-- ============================================

CREATE TABLE IF NOT EXISTS project_notification_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    channel_override VARCHAR(20),
    recipients TEXT,
    notify_users TEXT,
    rate_limit_seconds INT DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_template ON project_notification_config(project_id, template_code);
CREATE INDEX IF NOT EXISTS idx_pnc_project ON project_notification_config(project_id);

-- ============================================
-- 16. 项目 Token 绑定表
-- ============================================

CREATE TABLE IF NOT EXISTS project_token_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    token_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    priority INT DEFAULT 100,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_token ON project_token_binding(project_id, token_id);
CREATE INDEX IF NOT EXISTS idx_ptb_project ON project_token_binding(project_id);

-- ============================================
-- 17. 项目告警配置表
-- ============================================

CREATE TABLE IF NOT EXISTS project_alert_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    rule_id BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    custom_threshold DOUBLE,
    custom_priority VARCHAR(20),
    custom_duration_seconds INT,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_rule ON project_alert_config(project_id, rule_id);
CREATE INDEX IF NOT EXISTS idx_pac_project ON project_alert_config(project_id);

-- ============================================
-- 18. 修改现有表：添加 project_id 列
-- ============================================

ALTER TABLE system_configs ADD COLUMN IF NOT EXISTS project_id VARCHAR(100) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_config_project ON system_configs(project_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key_project ON system_configs(config_key, project_id);

ALTER TABLE agent_health ADD COLUMN IF NOT EXISTS project_id VARCHAR(100) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_health_project ON agent_health(project_id);

ALTER TABLE agent_performance ADD COLUMN IF NOT EXISTS project_id VARCHAR(100) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_perf_project ON agent_performance(project_id);
-- 将 agent_id 唯一约束改为 (project_id, agent_id) 联合唯一
DROP INDEX IF EXISTS uk_agent_performance_agent_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_perf_project_agent ON agent_performance(project_id, agent_id);

-- ============================================
-- 19. AI助手会话表
-- ============================================

CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_updated ON chat_sessions(updated_at);

-- ============================================
-- 20. AI助手消息表
-- ============================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    thinking TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);

-- ============================================
-- 21. 工作流模板表（用户自定义模板持久化）
-- ============================================

CREATE TABLE IF NOT EXISTS workflow_templates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    steps_json TEXT,
    builtin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wf_tpl_builtin ON workflow_templates(builtin);

-- ============================================
-- 22. 游戏模板表（用户自定义模板持久化）
-- ============================================

CREATE TABLE IF NOT EXISTS game_templates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    game_type VARCHAR(64),
    config_json TEXT,
    builtin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_game_tpl_builtin ON game_templates(builtin);

-- ============================================
-- 23. 工作流运行时表
-- ============================================

-- 工作流实例表
CREATE TABLE IF NOT EXISTS workflow_instances (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    parameters_json TEXT,
    context_json TEXT,
    error_message TEXT,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wfi_template ON workflow_instances(template_id);
CREATE INDEX IF NOT EXISTS idx_wfi_project ON workflow_instances(project_id);
CREATE INDEX IF NOT EXISTS idx_wfi_status ON workflow_instances(status);
CREATE INDEX IF NOT EXISTS idx_wfi_created ON workflow_instances(created_at);

-- 工作流步骤执行表
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(100),
    agent_role VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    input_data_json TEXT,
    output_data_json TEXT,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    timeout_minutes INT DEFAULT 30,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wfse_instance ON workflow_step_executions(instance_id);
CREATE INDEX IF NOT EXISTS idx_wfse_status ON workflow_step_executions(status);
CREATE INDEX IF NOT EXISTS idx_wfse_agent ON workflow_step_executions(agent_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_wfse_instance_step ON workflow_step_executions(instance_id, step_id);

-- 工作流审批表
CREATE TABLE IF NOT EXISTS workflow_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    approver_id BIGINT,
    approver_name VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment TEXT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_wfa_instance ON workflow_approvals(instance_id);
CREATE INDEX IF NOT EXISTS idx_wfa_status ON workflow_approvals(status);
CREATE INDEX IF NOT EXISTS idx_wfa_approver ON workflow_approvals(approver_id);

-- 工作流审计日志表
CREATE TABLE IF NOT EXISTS workflow_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64),
    action VARCHAR(50) NOT NULL,
    actor_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    actor_id VARCHAR(100),
    actor_name VARCHAR(100),
    detail_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wfal_instance ON workflow_audit_logs(instance_id);
CREATE INDEX IF NOT EXISTS idx_wfal_action ON workflow_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_wfal_actor ON workflow_audit_logs(actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_wfal_created ON workflow_audit_logs(created_at);

-- ============================================
-- 完成
-- ============================================
-- H2 数据库建表完成
