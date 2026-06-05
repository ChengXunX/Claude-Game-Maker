-- V11: 创建权限、能力、通知偏好相关表

-- 权限定义表
CREATE TABLE IF NOT EXISTS permission_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    `system` BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pd_key (permission_key),
    INDEX idx_pd_category (category),
    INDEX idx_pd_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户权限表
CREATE TABLE IF NOT EXISTS user_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    source VARCHAR(20) NOT NULL,
    granted_by VARCHAR(100),
    reason TEXT,
    expires_at TIMESTAMP NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_up_user (user_id),
    INDEX idx_up_perm (permission),
    UNIQUE INDEX idx_up_user_perm (user_id, permission)
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
    INDEX idx_pr_status (status),
    INDEX idx_pr_created (created_at)
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
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approval_type VARCHAR(50),
    execution_type VARCHAR(20) NOT NULL DEFAULT 'java',
    prompt_template TEXT,
    target_agent_role VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
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
    INDEX idx_cap_name_role (capability_name, agent_role),
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
    INDEX idx_cil_status (status),
    INDEX idx_cil_created (created_at),
    INDEX idx_cil_cap_name (capability_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    INDEX idx_ar_status (status),
    INDEX idx_ar_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知偏好表
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    do_not_disturb BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_start VARCHAR(10),
    quiet_end VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_np_user (user_id),
    INDEX idx_np_type (notification_type),
    UNIQUE INDEX idx_np_user_type_channel (user_id, notification_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    knowledge_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    project_id VARCHAR(100),
    access_level VARCHAR(20) NOT NULL DEFAULT 'public',
    required_permissions VARCHAR(500),
    tags VARCHAR(500),
    priority INT NOT NULL DEFAULT 5,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kb_category (category),
    INDEX idx_kb_project (project_id),
    INDEX idx_kb_access (access_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
