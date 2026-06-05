-- 修复多个表的schema与实体不匹配问题

-- 修复alert_records表（已手动执行，此文件记录变更）
-- ALTER TABLE alert_records ADD COLUMN agent_id VARCHAR(50) AFTER rule_id;
-- ALTER TABLE alert_records ADD COLUMN agent_name VARCHAR(100) AFTER agent_id;
-- ... (其他列已手动添加)

-- 修复notifications表（已手动执行）
-- ALTER TABLE notifications ADD COLUMN channel VARCHAR(20) AFTER type;
-- ALTER TABLE notifications ADD COLUMN reference_id VARCHAR(100) AFTER link;
-- ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(50) AFTER reference_id;

-- 修复knowledge_base表
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) AFTER project_id;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS usage_count INT DEFAULT 0 AFTER enabled;

-- 修复performance_metrics表（value列已改为metric_value，实体已更新）
-- ALTER TABLE performance_metrics CHANGE COLUMN value metric_value DECIMAL(20,4);
