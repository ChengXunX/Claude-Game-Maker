-- V37: 迭代适应记录表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iteration_adapt_records' AND INDEX_NAME = 'idx_iar_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_iar_project ON iteration_adapt_records(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iteration_adapt_records' AND INDEX_NAME = 'idx_iar_phase');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_iar_phase ON iteration_adapt_records(phase)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iteration_adapt_records' AND INDEX_NAME = 'idx_iar_time');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_iar_time ON iteration_adapt_records(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
