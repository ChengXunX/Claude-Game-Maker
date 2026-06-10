-- 操作日志表索引优化
-- 为 username 字段添加索引，优化关键词搜索性能
CREATE INDEX IF NOT EXISTS idx_audit_username ON operation_logs(username);
