-- 告警规则表新增通知方式字段
ALTER TABLE alert_rules ADD COLUMN IF NOT EXISTS notify_method VARCHAR(20) DEFAULT 'SYSTEM';
