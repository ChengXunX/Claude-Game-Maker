-- 修复多个表的schema与实体不匹配问题

-- 1. 修复alert_records表（已手动执行）
-- ALTER TABLE alert_records ADD COLUMN agent_id VARCHAR(50) AFTER rule_id;
-- ALTER TABLE alert_records ADD COLUMN agent_name VARCHAR(100) AFTER agent_id;
-- ... (其他列已手动添加)

-- 2. 修复notifications表（已手动执行）
-- ALTER TABLE notifications ADD COLUMN channel VARCHAR(20) AFTER type;
-- ALTER TABLE notifications ADD COLUMN reference_id VARCHAR(100) AFTER link;
-- ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(50) AFTER reference_id;

-- 3. 修复knowledge_base表（已手动执行）
-- ALTER TABLE knowledge_base ADD COLUMN created_by VARCHAR(50) AFTER project_id;
-- ALTER TABLE knowledge_base ADD COLUMN usage_count INT DEFAULT 0 AFTER enabled;

-- 4. 修复performance_metrics表（已手动执行）
-- 实体中value字段已改为metric_value

-- 5. 修复operation_logs表（已手动执行）
-- ALTER TABLE operation_logs ADD COLUMN agent_id VARCHAR(50) AFTER username;
-- ... (其他列已手动添加)

-- 6. 修复agent_interventions表（已手动执行）
-- ALTER TABLE agent_interventions ADD COLUMN version INT DEFAULT 0;

-- 7. 修复alert_rules表（已手动执行）
-- ALTER TABLE alert_rules ADD COLUMN operator VARCHAR(20) NOT NULL DEFAULT '>' AFTER condition_type;
-- ALTER TABLE alert_rules ADD COLUMN rule_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' AFTER description;

-- 8. 修复device_trusts表（已手动执行）
-- ALTER TABLE device_trusts ADD COLUMN trusted_at TIMESTAMP NULL;
-- ALTER TABLE device_trusts ADD COLUMN last_used_at TIMESTAMP NULL;

-- 9. 创建pipelines表（已手动执行）
-- CREATE TABLE pipelines (...);

-- 10. 创建code_reviews表（已手动执行）
-- CREATE TABLE code_reviews (...);

-- 11. 修复git_repositories表（已手动执行）
-- ALTER TABLE git_repositories ADD COLUMN description VARCHAR(500);
-- ... (其他列已手动添加)

-- 12. 修复api_tokens表（已手动执行）
-- ALTER TABLE api_tokens ADD COLUMN token_name VARCHAR(100);
-- ... (其他列已手动添加)

-- 13. 修复token_usage表（已手动执行）
-- ALTER TABLE token_usage ADD COLUMN usage_date DATE;
-- ... (其他列已手动添加)
