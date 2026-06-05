-- 修复 performance_metrics 表结构
-- 添加缺失的字段，与 PerformanceMetric 实体类保持一致

ALTER TABLE performance_metrics ADD COLUMN agent_id VARCHAR(100) AFTER metric_type;
ALTER TABLE performance_metrics ADD COLUMN agent_name VARCHAR(100) AFTER agent_id;
ALTER TABLE performance_metrics ADD COLUMN project_id VARCHAR(100) AFTER agent_name;
ALTER TABLE performance_metrics ADD COLUMN api_path VARCHAR(500) AFTER project_id;
ALTER TABLE performance_metrics ADD COLUMN unit VARCHAR(20) AFTER metric_value;
ALTER TABLE performance_metrics ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER tags;

CREATE INDEX idx_metric_agent ON performance_metrics(agent_id);
CREATE INDEX idx_metric_created ON performance_metrics(created_at);
