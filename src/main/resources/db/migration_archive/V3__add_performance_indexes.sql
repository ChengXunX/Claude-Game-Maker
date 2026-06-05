-- ============================================
-- 性能优化索引迁移脚本
-- 版本: 3.0.0
-- 创建时间: 2026-05-29
-- 说明: 添加复合索引优化查询性能
-- ============================================

-- 告警记录表索引优化
-- 优化按规则和状态查询
CREATE INDEX idx_alert_records_rule_status ON alert_records(rule_id, status);

-- 优化按时间和状态查询（用于告警趋势分析）
CREATE INDEX idx_alert_records_created_status ON alert_records(created_at, status);

-- Agent干预表索引优化
-- 优化按Agent和状态查询
CREATE INDEX idx_agent_interventions_agent_status ON agent_interventions(agent_id, status);

-- 优化按时间和类型查询
CREATE INDEX idx_agent_interventions_created_type ON agent_interventions(created_at, intervention_type);

-- 优化按操作员查询
CREATE INDEX idx_agent_interventions_operator ON agent_interventions(operator_id, created_at);

-- 操作日志表索引优化
-- 优化按时间和用户查询（用于操作审计）
CREATE INDEX idx_operation_logs_created_user ON operation_logs(created_at, user_id);

-- 优化按操作类型查询
CREATE INDEX idx_operation_logs_operation_created ON operation_logs(operation, created_at);

-- Token使用统计表索引优化
-- 优化按Agent和时间范围查询（用于Token使用分析）
CREATE INDEX idx_token_usage_agent_created ON token_usage(agent_id, created_at);

-- 优化按模型查询（用于模型使用统计）
CREATE INDEX idx_token_usage_model_created ON token_usage(model, created_at);

-- Agent日志表索引优化
-- 优化按Agent和日志级别查询
CREATE INDEX idx_agent_logs_agent_level ON agent_logs(agent_id, log_level);

-- 优化按时间和日志级别查询
CREATE INDEX idx_agent_logs_created_level ON agent_logs(created_at, log_level);

-- 通知表索引优化
-- 优化按用户和已读状态查询
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

-- 优化按时间查询（用于通知清理）
CREATE INDEX idx_notifications_created ON notifications(created_at);

-- 性能指标表索引优化
-- 优化按指标名称和时间查询
CREATE INDEX idx_performance_metrics_name_time ON performance_metrics(metric_name, recorded_at);

-- 优化按指标类型查询
CREATE INDEX idx_performance_metrics_type_time ON performance_metrics(metric_type, recorded_at);

-- API Token表索引优化
-- 优化按Token查询（用于认证）
CREATE INDEX idx_api_tokens_token_expires ON api_tokens(token, expires_at);

-- 优化按用户查询
CREATE INDEX idx_api_tokens_user_expires ON api_tokens(user_id, expires_at);

-- 设备信任表索引优化
-- 优化按设备指纹查询
CREATE INDEX idx_device_trusts_fingerprint_user ON device_trusts(device_fingerprint, user_id);

-- 优化按过期时间查询（用于清理过期设备）
CREATE INDEX idx_device_trusts_expires ON device_trusts(expires_at);

-- Git仓库表索引优化
-- 优化按状态查询
CREATE INDEX idx_git_repositories_status_updated ON git_repositories(status, updated_at);
