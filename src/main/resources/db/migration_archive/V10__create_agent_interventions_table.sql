-- ============================================
-- 创建 Agent 干预记录表
-- 版本: 10
-- 创建时间: 2026-05-30
-- ============================================

-- Agent 干预记录表
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
    INDEX idx_intervention_no (intervention_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
