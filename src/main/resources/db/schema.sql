-- ============================================
-- ChengXun Game Maker 数据库Schema定义
-- 版本: 2.0.0
-- 更新时间: 2026-06-03
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
    INDEX idx_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色权限表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission),
    INDEX idx_role_permissions_role_id (role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_group VARCHAR(50),
    value_type VARCHAR(20) DEFAULT 'string',
    is_system_builtin BOOLEAN DEFAULT FALSE,
    description VARCHAR(500),
    project_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    INDEX idx_config_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(50),
    channel VARCHAR(20),
    is_read BOOLEAN DEFAULT FALSE,
    link VARCHAR(500),
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知模板表
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL UNIQUE,
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
    INDEX idx_template_channel (channel),
    INDEX idx_template_category (category),
    INDEX idx_template_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    rule_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    metric_name VARCHAR(100) NOT NULL,
    condition_type VARCHAR(20) NOT NULL,
    operator VARCHAR(20) NOT NULL DEFAULT '>',
    threshold DECIMAL(20,4) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    duration_seconds INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    rule_category VARCHAR(50) DEFAULT 'SYSTEM',
    cooldown_seconds INT DEFAULT 300,
    notification_channels VARCHAR(200) DEFAULT 'SYSTEM',
    auto_resolve BOOLEAN DEFAULT FALSE,
    severity_level INT DEFAULT 1,
    tags VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alert_rules_metric (metric_name),
    INDEX idx_alert_rules_enabled (enabled),
    INDEX idx_alert_rules_category (rule_category),
    INDEX idx_alert_rules_severity (severity_level),
    INDEX idx_alert_rule_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    agent_id VARCHAR(50),
    agent_name VARCHAR(100),
    project_id VARCHAR(100),
    title VARCHAR(200) NOT NULL DEFAULT 'Alert',
    detail TEXT,
    metric VARCHAR(100),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    rule_name VARCHAR(100),
    metric_value DECIMAL(20,4),
    trigger_value DECIMAL(20,4),
    threshold_value DECIMAL(20,4),
    threshold DECIMAL(20,4),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    acknowledged_by VARCHAR(50),
    acknowledged_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    rule_category VARCHAR(50),
    severity_level INT DEFAULT 1,
    resolved_by VARCHAR(50),
    resolution_notes TEXT,
    resolution TEXT,
    notified BOOLEAN DEFAULT FALSE,
    INDEX idx_alert_record_agent_id (agent_id),
    INDEX idx_alert_record_project_id (project_id),
    INDEX idx_alert_record_status (status),
    INDEX idx_alert_record_priority (priority),
    INDEX idx_alert_record_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    agent_id VARCHAR(50),
    action VARCHAR(50) NOT NULL DEFAULT 'CUSTOM',
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    target_name VARCHAR(200),
    detail TEXT,
    level VARCHAR(20) DEFAULT 'INFO',
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_params TEXT,
    response_data TEXT,
    duration_ms BIGINT,
    project_id VARCHAR(100),
    operation VARCHAR(100),
    method VARCHAR(10),
    url VARCHAR(500),
    response_status INT,
    execution_time BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_target (target_type),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    knowledge_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    project_id VARCHAR(100),
    created_by VARCHAR(50),
    access_level VARCHAR(20) NOT NULL DEFAULT 'public',
    required_permissions VARCHAR(500),
    tags VARCHAR(500),
    priority INT NOT NULL DEFAULT 5,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_knowledge_category (category),
    INDEX idx_knowledge_key (knowledge_key),
    INDEX idx_knowledge_project (project_id),
    INDEX idx_knowledge_access (access_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 性能指标表
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,4),
    unit VARCHAR(20),
    metric_type VARCHAR(50),
    agent_id VARCHAR(100),
    agent_name VARCHAR(100),
    project_id VARCHAR(100),
    api_path VARCHAR(500),
    tags TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_name (metric_name),
    INDEX idx_metric_type (metric_type),
    INDEX idx_metric_agent (agent_id),
    INDEX idx_metric_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent干预表
CREATE TABLE IF NOT EXISTS agent_interventions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intervention_no VARCHAR(50) NOT NULL UNIQUE,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    project_id VARCHAR(100),
    intervention_type VARCHAR(50) NOT NULL,
    instruction TEXT,
    reason TEXT,
    original_decision TEXT,
    new_decision TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    user_id BIGINT,
    username VARCHAR(50),
    user_role VARCHAR(20),
    acknowledged_at TIMESTAMP NULL,
    executed_at TIMESTAMP NULL,
    acknowledgement TEXT,
    execution_result TEXT,
    task_id VARCHAR(100),
    message_id VARCHAR(100),
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_intervention_agent (agent_id),
    INDEX idx_intervention_status (status),
    INDEX idx_intervention_type (intervention_type),
    INDEX idx_intervention_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 设备信任表
CREATE TABLE IF NOT EXISTS device_trusts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_name VARCHAR(100),
    ip_address VARCHAR(50),
    trusted_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    user_agent VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_user (user_id),
    INDEX idx_device_fingerprint (device_fingerprint),
    INDEX idx_device_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API Token表
CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_name VARCHAR(100) NOT NULL DEFAULT '',
    api_key VARCHAR(255) NOT NULL DEFAULT '',
    api_url VARCHAR(500),
    model VARCHAR(100),
    max_tokens INT,
    priority VARCHAR(20),
    agent_tags VARCHAR(500),
    assigned_agent_id VARCHAR(50),
    assigned_agent_name VARCHAR(100),
    usage_count INT DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    name VARCHAR(100),
    permissions TEXT,
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    INDEX idx_token_user (user_id),
    INDEX idx_token_agent (assigned_agent_id),
    INDEX idx_token_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Token用量表
CREATE TABLE IF NOT EXISTS token_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    user_id BIGINT,
    project_id VARCHAR(100),
    usage_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    input_tokens BIGINT DEFAULT 0,
    output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    call_count INT DEFAULT 0,
    estimated_cost DECIMAL(10,4) DEFAULT 0.0000,
    cost DECIMAL(10,4) DEFAULT 0.0000,
    model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    INDEX idx_usage_agent (agent_id),
    INDEX idx_usage_date (usage_date),
    INDEX idx_usage_model (model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- CICD流水线表
CREATE TABLE IF NOT EXISTS pipelines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    project_id VARCHAR(100),
    project_name VARCHAR(100),
    pipeline_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    current_stage VARCHAR(50),
    progress INT DEFAULT 0,
    config TEXT,
    git_branch VARCHAR(100),
    git_commit VARCHAR(100),
    trigger_type VARCHAR(50),
    triggered_by BIGINT,
    triggered_by_name VARCHAR(50),
    auto_triggered BOOLEAN DEFAULT FALSE,
    auto_trigger_config TEXT,
    requires_approval BOOLEAN DEFAULT FALSE,
    approval_status VARCHAR(20),
    approved_by BIGINT,
    approved_by_name VARCHAR(50),
    approval_comment TEXT,
    approved_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_seconds INT,
    execution_log TEXT,
    error_message TEXT,
    production_deploy BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pipeline_project (project_id),
    INDEX idx_pipeline_status (status),
    INDEX idx_pipeline_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码审查表
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    project_id VARCHAR(100),
    git_repository_id BIGINT,
    git_repository_name VARCHAR(100),
    repository_type VARCHAR(50),
    branch VARCHAR(100),
    commit_hash VARCHAR(100),
    diff_content TEXT,
    changed_files INT,
    review_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewer VARCHAR(50),
    review_comment TEXT,
    score INT,
    issue_count INT DEFAULT 0,
    warning_count INT DEFAULT 0,
    auto_review_result TEXT,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    agent_id VARCHAR(50),
    agent_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_review_project (project_id),
    INDEX idx_review_status (status),
    INDEX idx_review_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Git仓库表
CREATE TABLE IF NOT EXISTS git_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    project_id VARCHAR(100) NOT NULL DEFAULT '',
    repository_type VARCHAR(50) NOT NULL DEFAULT 'MAIN',
    url VARCHAR(500) NOT NULL,
    remote_url VARCHAR(500),
    branch VARCHAR(100) DEFAULT 'main',
    local_path VARCHAR(500),
    relative_path VARCHAR(500),
    default_branch VARCHAR(100) DEFAULT 'main',
    current_branch VARCHAR(100),
    last_sync_at TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    assigned_agents TEXT,
    review_rules TEXT,
    auto_review_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_repo_name (name),
    INDEX idx_repo_project (project_id),
    INDEX idx_repo_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent日志表
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
    INDEX idx_agent_log_agent_id (agent_id),
    INDEX idx_agent_log_action (action),
    INDEX idx_agent_log_created (created_at),
    INDEX idx_agent_log_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
