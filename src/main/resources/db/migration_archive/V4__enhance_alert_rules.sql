-- ============================================
-- 告警规则增强迁移脚本
-- 版本: 4.0.0
-- 创建时间: 2026-05-29
-- 说明: 增强告警规则表，添加更多配置选项
-- ============================================

-- 添加告警规则增强字段
ALTER TABLE alert_rules ADD COLUMN rule_category VARCHAR(50) DEFAULT 'SYSTEM' COMMENT '规则类别：AGENT, DATABASE, CACHE, SYSTEM';
ALTER TABLE alert_rules ADD COLUMN cooldown_seconds INT DEFAULT 300 COMMENT '告警冷却时间（秒），避免重复告警';
ALTER TABLE alert_rules ADD COLUMN notification_channels VARCHAR(200) DEFAULT 'SYSTEM' COMMENT '通知渠道：SYSTEM, FEISHU, EMAIL, MULTI';
ALTER TABLE alert_rules ADD COLUMN auto_resolve BOOLEAN DEFAULT FALSE COMMENT '是否自动解决告警';
ALTER TABLE alert_rules ADD COLUMN severity_level INT DEFAULT 1 COMMENT '严重程度等级：1-5，5最严重';
ALTER TABLE alert_rules ADD COLUMN tags VARCHAR(500) COMMENT '标签，用于分类和筛选';

-- 添加索引
CREATE INDEX idx_alert_rules_category ON alert_rules(rule_category);
CREATE INDEX idx_alert_rules_severity ON alert_rules(severity_level);

-- 插入默认告警规则 - Agent相关
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level) VALUES
('Agent响应超时', 'Agent工作时间超过30秒', 'agent.work.duration', 'GT', 30000, 'AGENT', true, 300, 'SYSTEM,FEISHU', 3),
('Agent错误率过高', 'Agent错误率超过10%', 'agent.error.rate', 'GT', 10, 'AGENT', true, 600, 'SYSTEM,FEISHU', 4),
('Agent队列积压', 'Agent待处理任务超过50个', 'agent.queue.size', 'GT', 50, 'AGENT', true, 300, 'SYSTEM', 2),
('Agent内存使用过高', 'Agent内存使用超过500MB', 'agent.memory.used', 'GT', 500, 'AGENT', true, 600, 'SYSTEM,FEISHU', 3);

-- 插入默认告警规则 - 数据库相关
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level) VALUES
('慢SQL过多', '慢SQL数量超过10条/分钟', 'sql.slow.count', 'GT', 10, 'DATABASE', true, 300, 'SYSTEM,FEISHU', 3),
('数据库连接池耗尽', '数据库连接池使用率超过90%', 'db.pool.usage', 'GT', 90, 'DATABASE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5),
('数据库查询超时', '数据库查询平均时间超过1秒', 'db.query.avgtime', 'GT', 1000, 'DATABASE', true, 300, 'SYSTEM', 3);

-- 插入默认告警规则 - 缓存相关
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level) VALUES
('Redis连接失败', 'Redis连接异常', 'redis.connection.error', 'GT', 0, 'CACHE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5),
('Redis内存使用过高', 'Redis内存使用超过80%', 'redis.memory.usage', 'GT', 80, 'CACHE', true, 300, 'SYSTEM,FEISHU', 3),
('缓存命中率过低', '缓存命中率低于50%', 'cache.hit.rate', 'LT', 50, 'CACHE', true, 600, 'SYSTEM', 2);

-- 插入默认告警规则 - 系统相关
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level) VALUES
('内存使用率过高', 'JVM内存使用率超过80%', 'jvm.memory.usage', 'GT', 80, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 3),
('CPU使用率过高', 'CPU使用率超过90%', 'system.cpu.usage', 'GT', 90, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 4),
('磁盘空间不足', '磁盘剩余空间低于20%', 'system.disk.free', 'LT', 20, 'SYSTEM', true, 600, 'SYSTEM,FEISHU,EMAIL', 4),
('线程池耗尽', '线程池活跃线程数超过最大值90%', 'threadpool.active', 'GT', 90, 'SYSTEM', true, 60, 'SYSTEM,FEISHU', 4);

-- 更新现有告警记录表，添加新字段
ALTER TABLE alert_records ADD COLUMN rule_category VARCHAR(50) COMMENT '规则类别，与alert_rules对应';
ALTER TABLE alert_records ADD COLUMN severity_level INT DEFAULT 1 COMMENT '严重程度等级';
ALTER TABLE alert_records ADD COLUMN resolved_by VARCHAR(50) COMMENT '解决人';
ALTER TABLE alert_records ADD COLUMN resolution_notes TEXT COMMENT '解决说明';

-- 添加索引
CREATE INDEX idx_alert_records_category ON alert_records(rule_category);
CREATE INDEX idx_alert_records_severity ON alert_records(severity_level);

-- 添加注释
ALTER TABLE alert_records MODIFY COLUMN rule_category VARCHAR(50) COMMENT '规则类别：AGENT, DATABASE, CACHE, SYSTEM';
ALTER TABLE alert_records MODIFY COLUMN severity_level INT DEFAULT 1 COMMENT '严重程度等级：1-5，5最严重';
ALTER TABLE alert_records MODIFY COLUMN resolved_by VARCHAR(50) COMMENT '解决人用户ID';
ALTER TABLE alert_records MODIFY COLUMN resolution_notes TEXT COMMENT '解决说明和处理措施';
