-- 修复operation_logs表的operation字段默认值
ALTER TABLE operation_logs ALTER COLUMN operation SET DEFAULT 'CUSTOM';
