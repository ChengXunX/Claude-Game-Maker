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
    summary TEXT,
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
-- 注意：rule_id 允许 NULL，因为 saveAlert() 用于 Agent 风险预警等不需要规则匹配的场景
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NULL COMMENT '关联的告警规则ID，允许NULL用于非规则触发的告警',
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
    user_id BIGINT,
    token VARCHAR(255) DEFAULT '',
    token_name VARCHAR(100) NOT NULL DEFAULT '',
    api_key VARCHAR(255) NOT NULL DEFAULT '',
    api_url VARCHAR(500),
    model VARCHAR(100),
    max_tokens INT,
    context_window INT DEFAULT 200000,
    priority INT,
    agent_tags VARCHAR(500),
    usage_count BIGINT,
    total_tokens_used BIGINT DEFAULT 0,
    quota_type VARCHAR(20) DEFAULT 'UNLIMITED',
    quota_total BIGINT DEFAULT 0,
    window_seconds INT DEFAULT 0,
    max_concurrent_agents INT DEFAULT 0,
    provider_type VARCHAR(30) DEFAULT 'ANTHROPIC',
    resource_type VARCHAR(20) DEFAULT 'TEXT',
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    name VARCHAR(100),
    permissions TEXT,
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_api_tokens_user_id ON api_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_api_tokens_token ON api_tokens(token);
CREATE INDEX IF NOT EXISTS idx_api_tokens_expires_at ON api_tokens(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_token ON api_tokens(token);

-- Token 使用记录表（滑动窗口配额计算）
CREATE TABLE IF NOT EXISTS token_usage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id BIGINT NOT NULL,
    tokens_used BIGINT NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_usage_token_time ON token_usage_records(token_id, used_at);

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
    thinking_mode INT DEFAULT NULL,
    capabilities TEXT,
    tags TEXT,
    supported_file_types TEXT,
    unsupported_features TEXT,
    max_context_size INT DEFAULT 100000,
    supports_image_generation BOOLEAN DEFAULT FALSE,
    supports_code_execution BOOLEAN DEFAULT TRUE,
    supports_file_operations BOOLEAN DEFAULT TRUE,
    api_provider VARCHAR(50) DEFAULT 'anthropic',
    prompt CLOB,
    notify_targets VARCHAR(500) DEFAULT 'producer',
    reviewer VARCHAR(50),
    role_name VARCHAR(100),
    prompt_version INT DEFAULT 0,
    last_evolution_source VARCHAR(50),
    last_evolution_at TIMESTAMP,
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
    source VARCHAR(20) DEFAULT 'web',
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
    feishu_message_id VARCHAR(50),
    feishu_open_id VARCHAR(100),
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

-- ============================================
-- 项目级Agent配置表（新增）
-- ============================================
CREATE TABLE IF NOT EXISTS project_agent_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    agent_role VARCHAR(50) NOT NULL,
    custom_system_prompt TEXT,
    custom_capability_prompt TEXT,
    responsibility_weights TEXT,
    project_context TEXT,
    optimization_suggestions TEXT,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pac_project ON project_agent_configs(project_id);
CREATE INDEX IF NOT EXISTS idx_pac_agent ON project_agent_configs(project_id, agent_role);
CREATE INDEX IF NOT EXISTS idx_pac_active ON project_agent_configs(project_id, agent_role, is_active);

-- 版本评估持久化表
CREATE TABLE IF NOT EXISTS version_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    milestone_id VARCHAR(100) NOT NULL,
    milestone_title VARCHAR(200),
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    efficiency_score INT DEFAULT 0,
    quality_score INT DEFAULT 0,
    overall_score INT DEFAULT 0,
    missing_roles TEXT,
    redundant_roles TEXT,
    recommendations TEXT,
    agent_evaluations_json TEXT
);
CREATE INDEX IF NOT EXISTS idx_ve_project ON version_evaluations(project_id);
CREATE INDEX IF NOT EXISTS idx_ve_milestone ON version_evaluations(milestone_id);

-- 系统常量表
CREATE TABLE IF NOT EXISTS system_constants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constant_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    default_value VARCHAR(500),
    value_type VARCHAR(20) DEFAULT 'string',
    group_name VARCHAR(50),
    unit VARCHAR(20),
    min_value BIGINT,
    max_value BIGINT,
    require_restart BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sc_group ON system_constants(group_name);

-- 质量门禁配置表
CREATE TABLE IF NOT EXISTS quality_gate_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gate_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    level INT NOT NULL DEFAULT 1,
    min_score INT NOT NULL DEFAULT 60,
    blocking BOOLEAN DEFAULT FALSE,
    check_items TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_qgc_level ON quality_gate_configs(level);
CREATE INDEX IF NOT EXISTS idx_qgc_enabled ON quality_gate_configs(enabled);

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    tags VARCHAR(500),
    source VARCHAR(50) DEFAULT 'system',
    usage_count INT DEFAULT 0,
    effectiveness_score DOUBLE DEFAULT 0.0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kb_category ON knowledge_base(category);
CREATE INDEX IF NOT EXISTS idx_kb_source ON knowledge_base(source);
CREATE INDEX IF NOT EXISTS idx_kb_enabled ON knowledge_base(enabled);

-- ===== 项目讨论系统 =====

CREATE TABLE IF NOT EXISTS project_discussions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(50),
    title VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    meeting_minutes TEXT,
    synced_to_producer BOOLEAN DEFAULT FALSE,
    synced_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pd_project ON project_discussions(project_id);
CREATE INDEX IF NOT EXISTS idx_pd_user ON project_discussions(user_id);
CREATE INDEX IF NOT EXISTS idx_pd_status ON project_discussions(status);
CREATE INDEX IF NOT EXISTS idx_pd_updated ON project_discussions(updated_at);

CREATE TABLE IF NOT EXISTS project_discussion_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    discussion_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sender VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pdm_discussion ON project_discussion_messages(discussion_id);
CREATE INDEX IF NOT EXISTS idx_pdm_created ON project_discussion_messages(created_at);

CREATE TABLE IF NOT EXISTS feishu_user_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100),
    user_id BIGINT NOT NULL,
    binding_code VARCHAR(10),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fub_open_id ON feishu_user_bindings(open_id);
CREATE INDEX IF NOT EXISTS idx_fub_user_id ON feishu_user_bindings(user_id);
CREATE INDEX IF NOT EXISTS idx_fub_binding_code ON feishu_user_bindings(binding_code);

-- 多轮推理记录表
CREATE TABLE IF NOT EXISTS multi_turn_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(200) NOT NULL,
    project_id VARCHAR(200),
    task_id VARCHAR(200),
    task_description TEXT,
    turn_number INT DEFAULT 1,
    max_turns INT DEFAULT 3,
    think_result TEXT,
    plan_result TEXT,
    act_result TEXT,
    verify_result TEXT,
    verify_passed BOOLEAN,
    status VARCHAR(20) DEFAULT 'THINKING',
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mtr_agent ON multi_turn_records(agent_id);
CREATE INDEX IF NOT EXISTS idx_mtr_project ON multi_turn_records(project_id);
CREATE INDEX IF NOT EXISTS idx_mtr_status ON multi_turn_records(status);
CREATE INDEX IF NOT EXISTS idx_mtr_time ON multi_turn_records(created_at);

-- 迭代适应记录表
CREATE TABLE IF NOT EXISTS iteration_adapt_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    phase VARCHAR(20),
    version VARCHAR(50),
    iteration_count INT,
    old_strategy VARCHAR(50),
    new_strategy VARCHAR(50),
    old_pass_score INT,
    new_pass_score INT,
    reason VARCHAR(500),
    applied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_iar_project ON iteration_adapt_records(project_id);
CREATE INDEX IF NOT EXISTS idx_iar_phase ON iteration_adapt_records(phase);
CREATE INDEX IF NOT EXISTS idx_iar_time ON iteration_adapt_records(created_at);

-- 质量预测记录表
CREATE TABLE IF NOT EXISTS quality_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    project_name VARCHAR(200),
    version VARCHAR(50),
    pass_probability INT,
    risk_level VARCHAR(20),
    risk_factors TEXT,
    improvement_suggestions TEXT,
    factors_detail TEXT,
    historical_pass_rate DOUBLE,
    milestone_completion_rate DOUBLE,
    verification_fail_count INT,
    avg_agent_error_rate DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_qp_project ON quality_predictions(project_id);
CREATE INDEX IF NOT EXISTS idx_qp_version ON quality_predictions(version);
CREATE INDEX IF NOT EXISTS idx_qp_time ON quality_predictions(created_at);

-- 知识图谱节点表
CREATE TABLE IF NOT EXISTS knowledge_graph_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    node_type VARCHAR(30) NOT NULL,
    node_ref_id VARCHAR(200),
    display_name VARCHAR(200),
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kgn_project ON knowledge_graph_nodes(project_id);
CREATE INDEX IF NOT EXISTS idx_kgn_type ON knowledge_graph_nodes(node_type);
CREATE INDEX IF NOT EXISTS idx_kgn_ref ON knowledge_graph_nodes(node_ref_id);

-- 知识图谱边表
CREATE TABLE IF NOT EXISTS knowledge_graph_edges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    from_node_id BIGINT NOT NULL,
    to_node_id BIGINT NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kge_project ON knowledge_graph_edges(project_id);
CREATE INDEX IF NOT EXISTS idx_kge_from ON knowledge_graph_edges(from_node_id);
CREATE INDEX IF NOT EXISTS idx_kge_to ON knowledge_graph_edges(to_node_id);
CREATE INDEX IF NOT EXISTS idx_kge_type ON knowledge_graph_edges(relation_type);

-- 设计审查记录表
CREATE TABLE IF NOT EXISTS design_review_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    project_name VARCHAR(200),
    score INT,
    passed BOOLEAN,
    summary TEXT,
    strengths TEXT,
    issues TEXT,
    report TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_drr_project ON design_review_records(project_id);
CREATE INDEX IF NOT EXISTS idx_drr_time ON design_review_records(created_at);

-- ============================================
-- MCP (Model Context Protocol) 表
-- ============================================

-- MCP 服务器配置表
CREATE TABLE IF NOT EXISTS mcp_servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    transport_type VARCHAR(20) NOT NULL,
    command VARCHAR(500),
    args TEXT,
    env TEXT,
    url VARCHAR(500),
    headers TEXT,
    ai_api_key VARCHAR(500),
    ai_api_url VARCHAR(500),
    ai_model VARCHAR(100),
    category VARCHAR(50),
    auth_mode VARCHAR(20) DEFAULT 'env',
    auth_header_name VARCHAR(100) DEFAULT 'Authorization',
    required_params TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    `template` BOOLEAN NOT NULL DEFAULT FALSE,
    template_key VARCHAR(50),
    project_id VARCHAR(100),
    tool_count INT NOT NULL DEFAULT 0,
    last_test_at TIMESTAMP NULL,
    last_test_result VARCHAR(500),
    connected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_mcp_server_template_key ON mcp_servers(template_key);
CREATE INDEX IF NOT EXISTS idx_mcp_server_project ON mcp_servers(project_id);
CREATE INDEX IF NOT EXISTS idx_mcp_server_enabled ON mcp_servers(enabled);
CREATE INDEX IF NOT EXISTS idx_mcp_server_name ON mcp_servers(name);

-- MCP 工具表
CREATE TABLE IF NOT EXISTS mcp_tools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    input_schema TEXT,
    default_params TEXT,
    param_hints TEXT,
    category VARCHAR(50),
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    call_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_server ON mcp_tools(server_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_name ON mcp_tools(tool_name);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_enabled ON mcp_tools(enabled);

-- Agent MCP 绑定表
CREATE TABLE IF NOT EXISTS agent_mcp_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_role VARCHAR(50) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    server_id BIGINT NOT NULL,
    tool_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_amb_agent ON agent_mcp_bindings(agent_role, project_id);
CREATE INDEX IF NOT EXISTS idx_amb_server ON agent_mcp_bindings(server_id);
CREATE INDEX IF NOT EXISTS idx_amb_tool ON agent_mcp_bindings(tool_id);

-- ============================================
-- 游戏验证记录表
-- ============================================

CREATE TABLE IF NOT EXISTS game_verify_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    milestone_id VARCHAR(36),
    verify_type VARCHAR(20) NOT NULL,
    overall_score INT DEFAULT 0,
    structural_passed BOOLEAN DEFAULT FALSE,
    build_passed BOOLEAN DEFAULT FALSE,
    build_type VARCHAR(20),
    build_duration_ms BIGINT DEFAULT 0,
    build_output TEXT,
    runtime_passed BOOLEAN DEFAULT FALSE,
    runtime_port INT,
    runtime_duration_ms BIGINT DEFAULT 0,
    runtime_output TEXT,
    console_errors TEXT,
    resource_errors TEXT,
    quality_score INT DEFAULT 0,
    runnable_score INT DEFAULT 0,
    playable_score INT DEFAULT 0,
    completeness_score INT DEFAULT 0,
    uiux_score INT DEFAULT 0,
    code_quality_score INT DEFAULT 0,
    quality_summary TEXT,
    quality_issues TEXT,
    quality_suggestions TEXT,
    raw_ai_response TEXT,
    overall_passed BOOLEAN DEFAULT FALSE,
    -- G8 新增：真实运行+截图+视觉分析字段
    screenshots_json TEXT,
    visual_score INT DEFAULT 0,
    render_health_score INT DEFAULT 0,
    visual_playable_score INT DEFAULT 0,
    visual_uiux_score INT DEFAULT 0,
    visual_summary TEXT,
    visual_issues_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_gvr_project ON game_verify_records(project_id);
CREATE INDEX IF NOT EXISTS idx_gvr_milestone ON game_verify_records(milestone_id);
CREATE INDEX IF NOT EXISTS idx_gvr_created ON game_verify_records(created_at);

-- 游戏截图记录表（G8 新增）
CREATE TABLE IF NOT EXISTS game_screenshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    verify_record_id BIGINT,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_size_kb INT DEFAULT 0,
    frame_index INT DEFAULT 0,
    captured_at TIMESTAMP NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_gs_project ON game_screenshots(project_id);
CREATE INDEX IF NOT EXISTS idx_gs_verify_record ON game_screenshots(verify_record_id);
CREATE INDEX IF NOT EXISTS idx_gs_captured ON game_screenshots(captured_at);

-- 游戏验证结果表（QUICK 验证，遗留表，G8 补全+加视觉分析字段）
CREATE TABLE IF NOT EXISTS game_verify_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    project_name VARCHAR(200),
    success BOOLEAN DEFAULT FALSE,
    message TEXT,
    error TEXT,
    warnings_json TEXT,
    verified_at TIMESTAMP NOT NULL,
    verify_type VARCHAR(20) DEFAULT 'QUICK',
    overall_score INT DEFAULT 0,
    runnable_score INT DEFAULT 0,
    playable_score INT DEFAULT 0,
    completeness_score INT DEFAULT 0,
    uiux_score INT DEFAULT 0,
    code_quality_score INT DEFAULT 0,
    summary TEXT,
    strengths_json TEXT,
    issues_json TEXT,
    suggestions_json TEXT,
    -- G8 新增
    screenshots_json TEXT,
    render_health_score INT DEFAULT 0,
    visual_playable_score INT DEFAULT 0,
    visual_uiux_score INT DEFAULT 0,
    visual_score INT DEFAULT 0,
    visual_summary TEXT,
    visual_issues_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_gvr2_project ON game_verify_results(project_id);
CREATE INDEX IF NOT EXISTS idx_gvr2_type ON game_verify_results(verify_type);
CREATE INDEX IF NOT EXISTS idx_gvr2_verified ON game_verify_results(verified_at);

-- ============================================
-- 运行时错误记录表
-- ============================================

CREATE TABLE IF NOT EXISTS runtime_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    verify_record_id BIGINT,
    error_type VARCHAR(20) NOT NULL,
    error_message TEXT NOT NULL,
    line_number INT,
    file_path VARCHAR(500),
    auto_fixed BOOLEAN DEFAULT FALSE,
    fix_task_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_re_project ON runtime_errors(project_id);
CREATE INDEX IF NOT EXISTS idx_re_type ON runtime_errors(error_type);
CREATE INDEX IF NOT EXISTS idx_re_created ON runtime_errors(created_at);

-- ============================================
-- game_projects 表新增字段
-- ============================================

ALTER TABLE game_projects ADD COLUMN IF NOT EXISTS tech_stack VARCHAR(100);
ALTER TABLE game_projects ADD COLUMN IF NOT EXISTS game_template VARCHAR(50);
ALTER TABLE game_projects ADD COLUMN IF NOT EXISTS last_verify_score INT;
ALTER TABLE game_projects ADD COLUMN IF NOT EXISTS last_verify_time TIMESTAMP;
ALTER TABLE game_projects ADD COLUMN IF NOT EXISTS gdd_version INT DEFAULT 0;
