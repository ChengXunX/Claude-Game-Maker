-- 修复 alert_rules 表结构
-- 添加缺失的字段，与 AlertRule 实体类保持一致

ALTER TABLE alert_rules ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' AFTER threshold;

CREATE INDEX idx_alert_rule_priority ON alert_rules(priority);
