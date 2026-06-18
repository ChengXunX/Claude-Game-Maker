-- V40: 设计审查记录表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'design_review_records' AND INDEX_NAME = 'idx_drr_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_drr_project ON design_review_records(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'design_review_records' AND INDEX_NAME = 'idx_drr_time');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_drr_time ON design_review_records(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
