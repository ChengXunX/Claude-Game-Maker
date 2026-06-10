-- 创建操作日志归档表
-- 结构与 operation_logs 完全一致，用于存储 90 天前的旧数据
CREATE TABLE IF NOT EXISTS operation_logs_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    agent_id VARCHAR(50),
    operation VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    target_name VARCHAR(200),
    detail TEXT,
    level VARCHAR(20),
    status VARCHAR(20),
    error_message TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_params TEXT,
    response_data TEXT,
    duration_ms BIGINT,
    project_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_archive_created (created_at),
    INDEX idx_archive_action (operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志归档表';
