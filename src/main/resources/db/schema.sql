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

-- 工作流模板表（用户自定义模板持久化）
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

-- 游戏模板表（用户自定义模板持久化）
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

-- 工作流实例表（工作流运行时状态持久化）
CREATE TABLE IF NOT EXISTS workflow_instances (
    id VARCHAR(64) PRIMARY KEY COMMENT '实例ID（UUID）',
    template_id VARCHAR(64) NOT NULL COMMENT '关联的模板ID',
    project_id VARCHAR(100) COMMENT '所属项目ID',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED',
    parameters_json TEXT COMMENT '启动参数JSON',
    context_json TEXT COMMENT '全局上下文JSON（步骤间共享数据）',
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

-- 工作流步骤执行表（每个步骤的执行记录）
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '执行ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) NOT NULL COMMENT '步骤ID（模板中定义的）',
    agent_id VARCHAR(100) COMMENT '分配的Agent运行时ID',
    agent_role VARCHAR(50) COMMENT 'Agent角色',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/WAITING_DEPENDENCIES/READY/RUNNING/COMPLETED/FAILED/SKIPPED',
    input_data_json TEXT COMMENT '输入数据JSON（从依赖步骤收集）',
    output_data_json TEXT COMMENT '输出数据JSON（本步骤执行结果）',
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

-- 工作流审批表（审批流程记录）
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

-- 工作流审计日志表（所有关键操作的审计记录）
CREATE TABLE IF NOT EXISTS workflow_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) COMMENT '关联的步骤ID（可为空，表示实例级操作）',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: INSTANCE_CREATED/INSTANCE_STARTED/INSTANCE_PAUSED/INSTANCE_RESUMED/INSTANCE_COMPLETED/INSTANCE_FAILED/INSTANCE_CANCELLED/STEP_STARTED/STEP_COMPLETED/STEP_FAILED/STEP_RETRIED/STEP_TIMEOUT/APPROVAL_REQUESTED/APPROVAL_APPROVED/APPROVAL_REJECTED',
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

-- ----------------------------
-- 权限扩展表（从 V1 迁移同步）
-- ----------------------------

-- 权限定义表
CREATE TABLE IF NOT EXISTS permission_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    system TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_key (permission_key),
    INDEX idx_pd_category (category),
    INDEX idx_pd_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户权限表（单用户权限扩展）
CREATE TABLE IF NOT EXISTS user_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    source VARCHAR(20) NOT NULL,
    granted_by VARCHAR(100),
    reason TEXT,
    expires_at TIMESTAMP NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission),
    INDEX idx_up_user (user_id),
    INDEX idx_up_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 权限申请表
CREATE TABLE IF NOT EXISTS permission_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT,
    approver_name VARCHAR(100),
    approval_comment TEXT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pr_user (user_id),
    INDEX idx_pr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统常量表
CREATE TABLE IF NOT EXISTS system_constants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constant_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    value VARCHAR(500) NOT NULL,
    default_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'string',
    group_name VARCHAR(50) NOT NULL,
    unit VARCHAR(20),
    min_value BIGINT,
    max_value BIGINT,
    require_restart TINYINT(1) NOT NULL DEFAULT 0,
    system_builtin TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_constant_key (constant_key),
    INDEX idx_sc_group (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Agent 扩展表（从 V1 迁移同步）
-- ----------------------------

-- Agent 健康状态表
CREATE TABLE IF NOT EXISTS agent_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100),
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    health_status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    avg_response_time_ms BIGINT,
    max_response_time_ms BIGINT,
    total_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    error_rate DOUBLE DEFAULT 0,
    memory_usage_mb BIGINT,
    cpu_usage_percent DOUBLE,
    active_tasks INT DEFAULT 0,
    completed_tasks BIGINT DEFAULT 0,
    task_completion_rate DOUBLE DEFAULT 0,
    last_activity_time TIMESTAMP NULL,
    last_error_time TIMESTAMP NULL,
    last_error_message TEXT,
    consecutive_errors INT DEFAULT 0,
    uptime_seconds BIGINT DEFAULT 0,
    check_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_health_agent (agent_id),
    INDEX idx_health_status (health_status),
    INDEX idx_health_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 绩效表
CREATE TABLE IF NOT EXISTS agent_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100),
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    failed_tasks INT DEFAULT 0,
    in_progress_tasks INT DEFAULT 0,
    avg_completion_time_ms BIGINT DEFAULT 0,
    min_completion_time_ms BIGINT DEFAULT 0,
    max_completion_time_ms BIGINT DEFAULT 0,
    avg_quality_score DOUBLE DEFAULT 0,
    max_quality_score DOUBLE DEFAULT 0,
    min_quality_score DOUBLE DEFAULT 0,
    review_pass_rate DOUBLE DEFAULT 0,
    current_load INT DEFAULT 0,
    avg_load DOUBLE DEFAULT 0,
    overall_score DOUBLE DEFAULT 0,
    reliability_score DOUBLE DEFAULT 0,
    efficiency_score DOUBLE DEFAULT 0,
    first_task_at TIMESTAMP NULL,
    last_task_at TIMESTAMP NULL,
    last_evaluated_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_id (agent_id),
    INDEX idx_perf_role (agent_role),
    INDEX idx_perf_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 文件表
CREATE TABLE IF NOT EXISTS agent_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(200) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100),
    source VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    parent_version_id BIGINT,
    file_hash VARCHAR(64),
    created_by VARCHAR(200),
    remark VARCHAR(500),
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_af_agent (agent_id),
    INDEX idx_af_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 能力表
CREATE TABLE IF NOT EXISTS agent_capabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_role VARCHAR(50) NOT NULL,
    capability_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    param_schema TEXT,
    method_name VARCHAR(100),
    requires_approval TINYINT(1) NOT NULL DEFAULT 0,
    approval_type VARCHAR(50),
    execution_type VARCHAR(20) NOT NULL DEFAULT 'java',
    prompt_template TEXT,
    target_agent_role VARCHAR(50),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 5,
    category VARCHAR(50),
    project_id VARCHAR(100),
    max_retry INT NOT NULL DEFAULT 0,
    cooldown_seconds INT NOT NULL DEFAULT 0,
    timeout_seconds INT NOT NULL DEFAULT 0,
    related_skill_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cap_role (agent_role),
    INDEX idx_cap_role_project (agent_role, project_id),
    INDEX idx_cap_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 能力调用日志表
CREATE TABLE IF NOT EXISTS capability_invocation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(200) NOT NULL,
    project_id VARCHAR(100),
    capability_name VARCHAR(100) NOT NULL,
    params TEXT,
    result TEXT,
    status VARCHAR(30) NOT NULL,
    approval_request_id BIGINT,
    error_message TEXT,
    duration_ms BIGINT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_cil_agent (agent_id),
    INDEX idx_cil_project (project_id),
    INDEX idx_cil_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent MCP 绑定表
CREATE TABLE IF NOT EXISTS agent_mcp_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_role VARCHAR(50) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    server_id BIGINT NOT NULL,
    tool_id BIGINT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_amb_agent (agent_role, project_id),
    INDEX idx_amb_server (server_id),
    INDEX idx_amb_tool (tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MCP 服务器表
CREATE TABLE IF NOT EXISTS mcp_servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    server_type VARCHAR(50) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    description TEXT,
    config TEXT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MCP 工具表
CREATE TABLE IF NOT EXISTS mcp_tools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    input_schema TEXT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mt_server (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 通知扩展表（从 V1 迁移同步）
-- ----------------------------

-- 通知偏好表
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_np_user_type_channel (user_id, notification_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 项目扩展表（从 V1 迁移同步）
-- ----------------------------

-- 项目成员表
CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_project_user (project_id, user_id),
    INDEX idx_pm_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目 Token 绑定表
CREATE TABLE IF NOT EXISTS project_token_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    token_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ptb_project_token (project_id, token_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目告警配置表
CREATE TABLE IF NOT EXISTS project_alert_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    threshold DOUBLE,
    notify_channels VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pac_project_type (project_id, alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目通知配置表
CREATE TABLE IF NOT EXISTS project_notification_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    config TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pnc_project_type (project_id, notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- CICD 扩展表（从 V1 迁移同步）
-- ----------------------------

-- 流水线阶段表
CREATE TABLE IF NOT EXISTS pipeline_stages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_id BIGINT NOT NULL,
    stage_name VARCHAR(100) NOT NULL,
    stage_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    order_index INT NOT NULL DEFAULT 0,
    config TEXT,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ps_pipeline (pipeline_id),
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 审批和文档表（从 V1 迁移同步）
-- ----------------------------

-- 审批请求表
CREATE TABLE IF NOT EXISTS approval_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100),
    requester_id VARCHAR(200) NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    request_data TEXT,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id VARCHAR(200),
    approver_name VARCHAR(100),
    approval_comment TEXT,
    priority INT NOT NULL DEFAULT 5,
    approved_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ar_project (project_id),
    INDEX idx_ar_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文档索引表
CREATE TABLE IF NOT EXISTS document_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    agent_id VARCHAR(100),
    project_id VARCHAR(100),
    title VARCHAR(500),
    summary VARCHAR(500),
    keywords VARCHAR(500),
    file_size BIGINT,
    content_hash VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1,
    INDEX idx_di_agent (agent_id),
    INDEX idx_di_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 绩效管理扩展表（从 V1 迁移同步）
-- ----------------------------

-- 绩效评审表
CREATE TABLE IF NOT EXISTS performance_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_no VARCHAR(50),
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    producer_id VARCHAR(50) NOT NULL,
    producer_name VARCHAR(100),
    project_id VARCHAR(100),
    project_name VARCHAR(200),
    review_period VARCHAR(20) NOT NULL,
    quality_score INT,
    efficiency_score INT,
    collaboration_score INT,
    innovation_score INT,
    overall_score INT,
    strengths TEXT,
    improvements TEXT,
    comments TEXT,
    highlights TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    is_warning TINYINT(1) DEFAULT 0,
    warning_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_review_no (review_no),
    INDEX idx_pr_agent (agent_id),
    INDEX idx_pr_producer (producer_id),
    INDEX idx_pr_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 解雇申请表
CREATE TABLE IF NOT EXISTS dismissal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_no VARCHAR(50),
    agent_id VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),
    is_system_role TINYINT(1) DEFAULT 0,
    producer_id VARCHAR(50) NOT NULL,
    producer_name VARCHAR(100),
    project_id VARCHAR(100),
    project_name VARCHAR(200),
    reason_type VARCHAR(50),
    reason TEXT,
    warning_count INT DEFAULT 0,
    last_warning_at DATETIME,
    last_warning_reason TEXT,
    performance_history TEXT,
    consecutive_low_score_periods INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT,
    approver_name VARCHAR(50),
    approved_at DATETIME,
    approval_comment TEXT,
    rejection_reason TEXT,
    executed_at DATETIME,
    executed_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dr_request_no (request_no),
    INDEX idx_dr_agent (agent_id),
    INDEX idx_dr_producer (producer_id),
    INDEX idx_dr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 制作人更换记录表
CREATE TABLE IF NOT EXISTS producer_replacements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    replacement_no VARCHAR(50),
    project_id VARCHAR(100) NOT NULL,
    project_name VARCHAR(200),
    old_producer_id VARCHAR(50) NOT NULL,
    old_producer_name VARCHAR(100),
    new_producer_id VARCHAR(50) NOT NULL,
    new_producer_name VARCHAR(100),
    reason_type VARCHAR(50),
    reason TEXT,
    new_guidelines TEXT,
    admin_id BIGINT,
    admin_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pr_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 招聘申请表
CREATE TABLE IF NOT EXISTS recruitment_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_no VARCHAR(50),
    producer_id VARCHAR(50) NOT NULL,
    producer_name VARCHAR(100),
    project_id VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    role_name VARCHAR(100),
    agent_name VARCHAR(100),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT,
    approver_name VARCHAR(100),
    approval_comment TEXT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rr_producer (producer_id),
    INDEX idx_rr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Chat 和其他功能表（从 V1 迁移同步）
-- ----------------------------

-- 干预表（简化版）
CREATE TABLE IF NOT EXISTS interventions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    user_id BIGINT,
    type VARCHAR(50) NOT NULL,
    content TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_iv_agent (agent_id),
    INDEX idx_iv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat 会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cs_user (user_id),
    INDEX idx_cs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat 消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    thinking TEXT,
    tool_uses TEXT,
    tasks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cm_session (session_id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 预设表
CREATE TABLE IF NOT EXISTS agent_presets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    preset_name VARCHAR(100) NOT NULL,
    description TEXT,
    agent_role VARCHAR(50) NOT NULL,
    config TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
