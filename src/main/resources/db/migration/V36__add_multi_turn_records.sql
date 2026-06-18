-- V36: 多轮推理记录表
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'multi_turn_records' AND INDEX_NAME = 'idx_mtr_agent');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_mtr_agent ON multi_turn_records(agent_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'multi_turn_records' AND INDEX_NAME = 'idx_mtr_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_mtr_project ON multi_turn_records(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'multi_turn_records' AND INDEX_NAME = 'idx_mtr_status');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_mtr_status ON multi_turn_records(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'multi_turn_records' AND INDEX_NAME = 'idx_mtr_time');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_mtr_time ON multi_turn_records(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
