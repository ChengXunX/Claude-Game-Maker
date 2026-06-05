-- V13: 创建 Agent 文件管理表

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
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_af_agent (agent_id),
    INDEX idx_af_project (project_id),
    INDEX idx_af_name (file_name),
    INDEX idx_af_source (source),
    INDEX idx_af_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
