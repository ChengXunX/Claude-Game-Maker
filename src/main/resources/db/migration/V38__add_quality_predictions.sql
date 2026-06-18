-- V38: 质量预测记录表
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quality_predictions' AND INDEX_NAME = 'idx_qp_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_qp_project ON quality_predictions(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quality_predictions' AND INDEX_NAME = 'idx_qp_version');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_qp_version ON quality_predictions(version)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quality_predictions' AND INDEX_NAME = 'idx_qp_time');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_qp_time ON quality_predictions(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
