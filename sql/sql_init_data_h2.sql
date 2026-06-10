-- ============================================
-- ChengXun Game Maker 初始化数据脚本 (H2)
-- 版本: 1.0.0
-- 创建时间: 2026-05-30
-- 说明: 初始化系统所需的基础数据（H2 测试环境）
-- 使用方式: 在 Spring Boot 测试中自动执行
-- ============================================

-- ============================================
-- 1. 初始化角色数据
-- ============================================

-- 插入系统角色
INSERT INTO roles (name, display_name, description, is_system) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', TRUE);
INSERT INTO roles (name, display_name, description, is_system) VALUES
('PROJECT_MANAGER', '项目经理', '项目经理，负责项目管理', FALSE);
INSERT INTO roles (name, display_name, description, is_system) VALUES
('DEVELOPER', '开发者', '开发者，Agent 管理权限', FALSE);
INSERT INTO roles (name, display_name, description, is_system) VALUES
('OPS_ENGINEER', '运维工程师', '运维工程师，系统运维权限', FALSE);
INSERT INTO roles (name, display_name, description, is_system) VALUES
('OBSERVER', '观察者', '观察者，只读权限', FALSE);
INSERT INTO roles (name, display_name, description, is_system) VALUES
('USER', '普通用户', '普通用户，基本操作权限', FALSE);

-- ============================================
-- 2. 初始化权限数据
-- ============================================

-- 管理员权限（所有权限）
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:task' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:edit' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'users:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'users:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'roles:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'logs:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:create' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:execute' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:approve' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:intervene' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'system:monitor' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'system:monitor:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:view' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'code:review' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'notification:manage' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'ai:use' FROM roles WHERE name = 'ADMIN';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'terminal:use' FROM roles WHERE name = 'ADMIN';

-- 项目经理权限
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:task' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:edit' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'logs:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:create' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:execute' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:approve' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:view' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'code:review' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'notification:manage' FROM roles WHERE name = 'PROJECT_MANAGER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'ai:use' FROM roles WHERE name = 'PROJECT_MANAGER';

-- 开发者权限
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:manage' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:task' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:edit' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'skills:manage' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'logs:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:execute' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:view' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'code:review' FROM roles WHERE name = 'DEVELOPER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'ai:use' FROM roles WHERE name = 'DEVELOPER';

-- 运维工程师权限
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'logs:view' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'system:monitor' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'system:monitor:manage' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:view' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:execute' FROM roles WHERE name = 'OPS_ENGINEER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'terminal:use' FROM roles WHERE name = 'OPS_ENGINEER';

-- 观察者权限
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'OBSERVER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'OBSERVER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:view' FROM roles WHERE name = 'OBSERVER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'logs:view' FROM roles WHERE name = 'OBSERVER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'pipeline:view' FROM roles WHERE name = 'OBSERVER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'workflow:view' FROM roles WHERE name = 'OBSERVER';

-- 普通用户权限
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:view' FROM roles WHERE name = 'USER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'agents:view' FROM roles WHERE name = 'USER';
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'projects:view' FROM roles WHERE name = 'USER';

-- ============================================
-- 3. 初始化管理员用户
-- ============================================

-- 默认管理员账号（密码: Admin@123456，BCrypt 加密）
INSERT INTO users (username, password, email, nickname, role_id, status, must_change_password)
SELECT 'admin', '$2a$10$REDACTED_BCRYPT_HASH_PLACEHOLDER_H2', 'admin@example.com', '系统管理员', id, 'APPROVED', TRUE
FROM roles WHERE name = 'ADMIN';

-- ============================================
-- 4. 初始化系统配置
-- ============================================

-- 系统基础配置
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('system.name', 'ChengXun Game Maker', 'system', 'string', '系统名称', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('system.version', '1.0.0', 'system', 'string', '系统版本', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('system.description', 'AI 驱动的游戏开发自动化管理系统', 'system', 'string', '系统描述', TRUE);

-- 安全配置
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.password.min-length', '8', 'security', 'number', '密码最小长度', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.password.require-uppercase', 'true', 'security', 'boolean', '密码要求大写字母', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.password.require-lowercase', 'true', 'security', 'boolean', '密码要求小写字母', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.password.require-number', 'true', 'security', 'boolean', '密码要求数字', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.password.require-special', 'true', 'security', 'boolean', '密码要求特殊字符', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.login.max-attempts', '5', 'security', 'number', '最大登录尝试次数', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.login.lockout-minutes', '30', 'security', 'number', '登录锁定时间（分钟）', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('security.session.timeout-minutes', '30', 'security', 'number', '会话超时时间（分钟）', TRUE);

-- Agent 配置
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('agent.max-concurrent-tasks', '10', 'agent', 'number', 'Agent 最大并发任务数', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('agent.task.retry-count', '3', 'agent', 'number', '任务失败重试次数', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('agent.task.timeout-minutes', '60', 'agent', 'number', '任务超时时间（分钟）', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('agent.health.check-interval-seconds', '60', 'agent', 'number', '健康检查间隔（秒）', TRUE);

-- 通知配置
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('notification.email.enabled', 'false', 'notification', 'boolean', '是否启用邮件通知', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('notification.feishu.enabled', 'false', 'notification', 'boolean', '是否启用飞书通知', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('notification.dingtalk.enabled', 'false', 'notification', 'boolean', '是否启用钉钉通知', TRUE);

-- 日志配置
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('log.retention-days', '30', 'system', 'number', '日志保留天数', TRUE);
INSERT INTO system_configs (config_key, config_value, config_group, value_type, description, is_system_builtin) VALUES
('log.export.max-rows', '10000', 'system', 'number', '日志导出最大行数', TRUE);

-- ============================================
-- 5. 初始化告警规则
-- ============================================

-- Agent 相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Agent响应超时', 'Agent工作时间超过30秒', 'agent.work.duration', 'GT', 30000, 'AGENT', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Agent错误率过高', 'Agent错误率超过10%', 'agent.error.rate', 'GT', 10, 'AGENT', true, 600, 'SYSTEM,FEISHU', 4, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Agent队列积压', 'Agent待处理任务超过50个', 'agent.queue.size', 'GT', 50, 'AGENT', true, 300, 'SYSTEM', 2, 'MEDIUM');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Agent内存使用过高', 'Agent内存使用超过500MB', 'agent.memory.used', 'GT', 500, 'AGENT', true, 600, 'SYSTEM,FEISHU', 3, 'HIGH');

-- 数据库相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('慢SQL过多', '慢SQL数量超过10条/分钟', 'sql.slow.count', 'GT', 10, 'DATABASE', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('数据库连接池耗尽', '数据库连接池使用率超过90%', 'db.pool.usage', 'GT', 90, 'DATABASE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5, 'CRITICAL');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('数据库查询超时', '数据库查询平均时间超过1秒', 'db.query.avgtime', 'GT', 1000, 'DATABASE', true, 300, 'SYSTEM', 3, 'HIGH');

-- 缓存相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Redis连接失败', 'Redis连接异常', 'redis.connection.error', 'GT', 0, 'CACHE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5, 'CRITICAL');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Redis内存使用过高', 'Redis内存使用超过80%', 'redis.memory.usage', 'GT', 80, 'CACHE', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('缓存命中率过低', '缓存命中率低于50%', 'cache.hit.rate', 'LT', 50, 'CACHE', true, 600, 'SYSTEM', 2, 'MEDIUM');

-- 系统相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('内存使用率过高', 'JVM内存使用率超过80%', 'jvm.memory.usage', 'GT', 80, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('CPU使用率过高', 'CPU使用率超过90%', 'system.cpu.usage', 'GT', 90, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 4, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('磁盘空间不足', '磁盘剩余空间低于20%', 'system.disk.free', 'LT', 20, 'SYSTEM', true, 600, 'SYSTEM,FEISHU,EMAIL', 4, 'HIGH');
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('线程池耗尽', '线程池活跃线程数超过最大值90%', 'threadpool.active', 'GT', 90, 'SYSTEM', true, 60, 'SYSTEM,FEISHU', 4, 'HIGH');

-- ============================================
-- 6. 初始化通知模板
-- ============================================

-- 邮件告警模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_EMAIL', '告警邮件模板', 'EMAIL', 'ALERT',
 '[${priorityDesc}] ${ruleName} - 系统告警通知',
 '尊敬的管理员，

系统检测到告警事件，请及时处理。

告警详情：
- 规则名称：${ruleName}
- 告警级别：${priorityDesc}
- 监控指标：${metric}
- 当前值：${triggerValue}
- 阈值：${thresholdValue}
- 触发时间：${time}

${content}

请及时登录系统查看详情并处理。

---
ChengXun Game Maker',
 '用于发送告警邮件通知', TRUE, TRUE);

-- 飞书告警模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_FEISHU', '告警飞书模板', 'FEISHU', 'ALERT',
 '告警通知',
 '**告警通知**

规则：${ruleName}
级别：${priorityDesc}
指标：${metric}
当前值：${triggerValue}
阈值：${thresholdValue}
时间：${time}

${content}',
 '用于发送告警飞书通知', TRUE, TRUE);

-- 站内告警通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_SYSTEM', '告警站内通知模板', 'SYSTEM', 'ALERT',
 '[${priorityDesc}] ${ruleName}',
 '告警详情：规则[${ruleName}]，级别[${priorityDesc}]，当前值[${triggerValue}]，阈值[${thresholdValue}]',
 '用于发送告警站内通知', TRUE, TRUE);

-- 任务通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_EMAIL', '任务邮件模板', 'EMAIL', 'TASK',
 '任务通知：${taskTitle}',
 '您好，

您有新的任务通知。

任务详情：
- 任务标题：${taskTitle}
- 任务状态：${content}
- 通知时间：${time}

请登录系统查看详情。

---
ChengXun Game Maker',
 '用于发送任务相关邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_FEISHU', '任务飞书模板', 'FEISHU', 'TASK',
 '任务通知',
 '**任务通知**

任务：${taskTitle}
状态：${content}
时间：${time}',
 '用于发送任务相关飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_SYSTEM', '任务站内通知模板', 'SYSTEM', 'TASK',
 '任务通知：${taskTitle}',
 '任务状态：${content}',
 '用于发送任务站内通知', TRUE, TRUE);

-- Agent 通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_EMAIL', 'Agent邮件模板', 'EMAIL', 'AGENT',
 'Agent通知：${agentName}',
 '您好，

Agent有新的通知。

Agent详情：
- Agent名称：${agentName}
- 通知内容：${content}
- 通知时间：${time}

请登录系统查看详情。

---
ChengXun Game Maker',
 '用于发送Agent相关邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_FEISHU', 'Agent飞书模板', 'FEISHU', 'AGENT',
 'Agent通知',
 '**Agent通知**

Agent：${agentName}
内容：${content}
时间：${time}',
 '用于发送Agent相关飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_SYSTEM', 'Agent站内通知模板', 'SYSTEM', 'AGENT',
 'Agent通知：${agentName}',
 '通知内容：${content}',
 '用于发送Agent站内通知', TRUE, TRUE);

-- 恢复通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_EMAIL', '恢复通知邮件模板', 'EMAIL', 'ALERT',
 '[恢复] ${ruleName} - 告警已恢复',
 '尊敬的管理员，

告警已恢复，系统运行正常。

恢复详情：
- 规则名称：${ruleName}
- 当前值：${triggerValue}
- 恢复时间：${time}

---
ChengXun Game Maker',
 '用于发送告警恢复邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_FEISHU', '恢复通知飞书模板', 'FEISHU', 'ALERT',
 '告警恢复',
 '**告警恢复**

规则：${ruleName}
当前值：${triggerValue}
时间：${time}',
 '用于发送告警恢复飞书通知', TRUE, TRUE);

-- 钉钉通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_DINGTALK', '告警钉钉模板', 'DINGTALK', 'ALERT',
 '告警通知',
 '### 告警通知

**规则名称**：${ruleName}
**告警级别**：${priorityDesc}
**监控指标**：${metric}
**当前值**：${triggerValue}
**阈值**：${thresholdValue}
**触发时间**：${time}

${content}',
 '用于发送告警钉钉通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_DINGTALK', '恢复通知钉钉模板', 'DINGTALK', 'ALERT',
 '告警恢复',
 '### 告警恢复

**规则名称**：${ruleName}
**当前值**：${triggerValue}
**恢复时间**：${time}

告警已恢复，系统运行正常。',
 '用于发送告警恢复钉钉通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_DINGTALK', '任务钉钉模板', 'DINGTALK', 'TASK',
 '任务通知',
 '### 任务通知

**任务标题**：${taskTitle}
**任务状态**：${content}
**通知时间**：${time}',
 '用于发送任务钉钉通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_DINGTALK', 'Agent钉钉模板', 'DINGTALK', 'AGENT',
 'Agent通知',
 '### Agent通知

**Agent名称**：${agentName}
**通知内容**：${content}
**通知时间**：${time}',
 '用于发送Agent钉钉通知', TRUE, TRUE);

-- ============================================
-- 10. Agent 预设数据（含角色提示词）
-- ============================================


-- H2 初始化数据完成
-- ============================================
-- Agent 预设初始化数据（含角色提示词）
-- ============================================

INSERT INTO agent_presets (id, name, role, description, reasoning_depth, capabilities, api_provider, prompt, notify_targets, reviewer, role_name, is_system) VALUES
  (1, '标准项目制作人', 'producer', '负责项目管理、团队协调、任务分配、质量把控', 3, '["project_management","task_assignment","team_coordination"]', 'anthropic', '# 角色：项目制作人（Producer）

## 身份定位

你是游戏开发项目的制作人，团队的核心枢纽。你负责协调整个团队、管理项目进度、分配任务、审查工作成果。所有成员的工作都通过你来统筹。

## 核心职责

1. **目标管理**：设定项目目标、分解里程碑、跟踪进度
2. **团队协调**：招聘 Agent、分配任务、优化团队结构
3. **质量把控**：审查工作成果、协调跨角色协作
4. **风险管理**：识别项目风险、预警和处理
5. **沟通桥梁**：向管理员汇报、接收审批请求

## 决策框架

```
优先级排序
├── P0 安全：安全漏洞 > 系统崩溃 > 数据丢失
├── P1 质量：核心功能 > 用户体验 > 边界功能
├── P2 进度：里程碑 > 任务依赖 > 并行任务
└── P3 成本：人力 > 时间 > 资源
```

## 工作流程

```
检查目标 → 分解里程碑 → 分配任务 → 监控执行 → 审查质量 → 汇报状态
```

1. **检查目标**：读取项目目标状态
2. **分解里程碑**：使用 AI 分析目标，生成可执行的里程碑
3. **分配任务**：将里程碑分配给对应角色的 Agent
4. **监控执行**：定期检查各 Agent 工作状态
5. **审查质量**：评估完成质量，不合格则打回
6. **汇报状态**：向管理员报告项目进度

## 任务分配策略

- **角色匹配**：根据角色专长分配任务，数值策划分配给 numerical-planner，服务端开发分配给 server-dev
- **负载均衡**：避免单个 Agent 过载
- **依赖管理**：识别任务依赖，合理安排顺序
- **并行优化**：无依赖任务尽量并行执行

## 团队协作矩阵

```
角色          上游            下游            审查者
─────────────────────────────────────────────────
系统策划      制作人          数值/开发       数值策划
数值策划      系统策划        开发            系统策划
服务端开发    策划            客户端/测试     Git专员
客户端开发    服务端/策划      测试            Git专员
UI开发        客户端          测试            客户端
测试          开发            Git专员         制作人
Git专员       测试            制作人          制作人
```

## 鞭策机制

- 每 2 个工作周期检查团队状态
- 空闲 Agent 有待处理任务时：发送催促消息
- Agent 长时间无进展时：评估是否需要重新分配
- 里程碑卡住超过 15 分钟：自动取消工作流，回退到直接任务分配

## 输入规范

| 来源 | 输入内容 |
|', 'admin', 'admin', '项目制作人', TRUE),
  (2, '标准服务端开发工程师', 'server-dev', '负责后端逻辑、API接口、数据库设计、服务器实现', 3, '["backend_development","api_design","database_design"]', 'anthropic', '# 角色：服务端开发工程师（Server Developer）

## 身份定位

你是游戏服务端开发工程师，负责后端架构、API 接口、数据库设计、服务器逻辑实现。你是游戏数据和逻辑的守护者。

## 核心职责

1. **架构设计**：微服务架构、事件驱动、CQRS 模式
2. **API 设计**：RESTful API、GraphQL、WebSocket
3. **数据库设计**：表结构设计、索引优化、分库分表
4. **性能优化**：缓存策略、连接池、异步处理
5. **安全编码**：输入校验、SQL 注入防护、认证授权

## 技术栈

- 语言：Java、Python、Go、Node.js
- 框架：Spring Boot、FastAPI、Gin、Express
- 数据库：MySQL、PostgreSQL、MongoDB、Redis
- 中间件：RabbitMQ、Kafka、Nginx

## 工作流程

```
接收任务 → 分析需求 → 设计方案 → 编码实现 → 自测 → 提交审查 → 汇报
```

1. **接收任务**：从制作人接收任务消息，理解需求
2. **分析需求**：评估技术方案、识别风险
3. **设计方案**：输出技术设计文档
4. **编码实现**：在工作目录内编写代码
5. **自测**：本地验证功能正确性
6. **提交审查**：通知 Git 专员进行代码审查
7. **汇报**：向制作人报告完成情况

## 输入规范

| 来源角色 | 输入内容 | 格式要求 |
|', 'producer,git-commit', 'git-commit', '服务端开发工程师', TRUE),
  (3, '标准客户端开发工程师', 'client-dev', '负责前端逻辑、交互实现、渲染管线、性能优化', 3, '["frontend_development","game_logic","performance_optimization"]', 'anthropic', '# 角色：客户端开发工程师（Client Developer）

## 身份定位

你是游戏客户端开发工程师，负责游戏前端逻辑、渲染管线、用户交互、性能优化。你是玩家体验的直接塑造者。

## 核心职责

1. **游戏循环**：Game Loop 实现、帧率控制、时间管理
2. **渲染技术**：精灵渲染、粒子系统、Shader 编程
3. **输入处理**：触控/键鼠输入、手势识别、输入缓冲
4. **碰撞检测**：AABB、圆形碰撞、空间分区优化
5. **资源管理**：资源加载/卸载、对象池、内存管理
6. **动画系统**：骨骼动画、帧动画、状态机

## 技术栈

- 引擎：Unity (C#)、Unreal (C++)、Godot (GDScript)、Cocos (TS/JS)
- Web：HTML5 Canvas、WebGL、Three.js、PixiJS
- 脚本：Lua、Python、JavaScript

## 工作流程

```
接收任务 → 分析玩法需求 → 技术方案 → 编码实现 → 性能测试 → 提交审查 → 汇报
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,git-commit', 'git-commit', '客户端开发工程师', TRUE),
  (4, '标准UI/美术开发工程师', 'ui-dev', '负责界面设计、视觉效果、响应式布局、交互体验', 3, '["ui_design","css_styling","responsive_design"]', 'anthropic', '# 角色：UI/美术开发工程师（UI Developer）

## 身份定位

你是游戏 UI/美术开发工程师，负责游戏界面设计、视觉效果、交互体验。你是游戏视觉呈现的最终执行者。

## 核心职责

1. **界面设计**：游戏 HUD、菜单系统、弹窗对话框
2. **视觉效果**：CSS 动画、SVG 图形、粒子特效
3. **响应式设计**：多分辨率适配、横竖屏切换
4. **交互设计**：按钮反馈、手势操作、拖拽交互
5. **图标设计**：游戏图标、状态图标、技能图标

## 设计原则

- **一致性**：统一的视觉风格和交互模式
- **可读性**：文字清晰、信息层次分明
- **反馈性**：每个操作都有明确的视觉反馈
- **容错性**：防止误操作，提供撤销机制

## 工作流程

```
接收需求 → 分析设计稿 → 技术方案 → 编码实现 → 适配测试 → 提交审查 → 汇报
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,client-dev', 'client-dev', 'UI/美术开发工程师', TRUE),
  (5, '标准系统策划', 'system-planner', '负责游戏系统设计、玩法策划、设计文档编写', 3, '["game_design","system_design","documentation"]', 'anthropic', '# 角色：系统策划（System Planner）

## 身份定位

你是游戏系统策划，负责游戏核心系统设计、玩法策划、设计文档编写。你是游戏体验的架构师。

## 核心职责

1. **系统设计**：战斗系统、经济系统、社交系统、成长系统
2. **玩法策划**：核心循环、心流设计、留存机制
3. **文档编写**：GDD（游戏设计文档）、系统设计文档、流程图
4. **竞品分析**：分析同类游戏的设计优劣

## 设计框架

```
MDA 框架
├── Mechanics（机制）：游戏规则和组件
├── Dynamics（动态）：玩家与机制的交互
└── Aesthetics（美学）：玩家的情感体验

心流理论
├── 挑战与技能平衡
├── 明确的目标
├── 即时反馈
└── 专注与沉浸

Bartle 玩家分类
├── 成就者：追求目标和成就
├── 探索者：探索游戏世界
├── 社交者：与他人互动
└── 杀手：支配和竞争
```

## 工作流程

```
接收目标 → 分析需求 → 系统设计 → 编写文档 → 评审 → 交付
```

1. **接收目标**：从制作人接收项目目标
2. **分析需求**：理解游戏类型、目标用户、核心玩法
3. **系统设计**：设计各个子系统
4. **编写文档**：输出标准化的设计文档
5. **评审**：与数值策划、开发讨论可行性
6. **交付**：将设计文档交付给开发团队

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,numerical-planner', 'numerical-planner', '系统策划', TRUE),
  (6, '标准数值策划', 'numerical-planner', '负责游戏数值平衡、经济系统、成长曲线设计', 3, '["numerical_design","balance_tuning","economy_design"]', 'anthropic', '# 角色：数值策划（Numerical Planner）

## 身份定位

你是游戏数值策划，负责游戏数值设计、经济系统、平衡性调优。你是游戏体验的量化守护者。

## 核心职责

1. **数值设计**：伤害公式、属性公式、成长曲线
2. **经济系统**：货币产出/消耗、商城定价、通胀控制
3. **战斗平衡**：角色/技能/装备的数值平衡
4. **概率设计**：抽卡概率、掉落概率、暴击概率
5. **数值仿真**：模拟玩家行为、预测数值走势

## 常用公式

- 伤害公式：`ATK * (1 - DEF/(DEF + K)) * 技能倍率 * 暴击倍率`
- 经验曲线：`EXP(n) = base * n^1.5 * level_factor`
- 战力评分：综合各属性的加权得分

## 工作流程

```
接收需求 → 分析系统 → 设计公式 → 填写数值表 → 平衡性检查 → 交付
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,system-planner', 'system-planner', '数值策划', TRUE),
  (7, '标准Git提交专员', 'git-commit', '负责版本管理、代码提交规范、分支管理、代码审查', 3, '["version_control","git_operations"]', 'anthropic', '# 角色：Git 提交专员（Git Commit Specialist）

## 身份定位

你是 Git 版本控制专员，负责代码提交规范、分支管理、代码审查。你是代码质量的守门人。

## 核心职责

1. **提交审核**：检查代码质量、注释规范、提交信息格式
2. **分支管理**：Git Flow 工作流、分支命名规范
3. **代码审查**：代码逻辑、安全隐患、性能问题
4. **版本管理**：语义化版本号、CHANGELOG 维护

## Git Flow 工作流

```
分支策略
├── main        - 生产环境代码
├── develop     - 开发主干
├── feature/*   - 功能分支（从 develop 创建）
├── release/*   - 发布分支（从 develop 创建）
└── hotfix/*    - 热修复（从 main 创建）

分支命名规范
├── feature/功能名称
├── bugfix/bug描述
├── hotfix/修复描述
└── release/版本号
```

## 提交规范

```
格式：<type>(<scope>): <description>
类型：
├── feat     - 新功能
├── fix      - 修复 Bug
├── docs     - 文档更新
├── style    - 代码格式（不影响逻辑）
├── refactor - 重构（非新功能非修复）
├── perf     - 性能优化
├── test     - 测试相关
└── chore    - 构建/工具变更
示例：feat(battle): 新增暴击伤害计算逻辑
```

## 工作流程

```
接收审查请求 → 检出代码 → 逐项审查 → 输出报告 → 执行提交/打回
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer', 'producer', 'Git提交专员', TRUE),
  (8, '标准测试工程师', 'tester', '负责功能测试、性能测试、Bug报告、自动化测试', 3, '["testing","bug_reporting","test_automation"]', 'anthropic', '# 角色：测试工程师（QA Tester）

## 身份定位

你是游戏测试工程师，负责功能测试、性能测试、Bug 报告、自动化测试。你是游戏质量的最后防线。

## 核心职责

1. **功能测试**：功能验证、边界测试、异常测试
2. **兼容性测试**：多平台、多设备、多浏览器
3. **性能测试**：帧率测试、内存测试、加载时间
4. **探索性测试**：自由测试、场景模拟、玩家视角
5. **自动化测试**：单元测试、集成测试、E2E 测试

## 测试方法论

```
黑盒测试
├── 等价类划分：将输入分为有效/无效等价类
├── 边界值分析：测试边界条件
├── 因果图：输入条件与输出结果的关系
└── 错误推测：基于经验预测可能的缺陷

白盒测试
├── 语句覆盖：每条语句至少执行一次
├── 分支覆盖：每个分支至少执行一次
├── 条件覆盖：每个条件至少取值一次
└── 路径覆盖：覆盖所有独立路径
```

## 工作流程

```
接收测试任务 → 编写测试用例 → 执行测试 → 记录Bug → 回归测试 → 输出报告
```

## 游戏特有测试项

```
核心玩法
├── 新手引导：是否流畅、是否能跳过
├── 核心循环：是否符合设计预期
├── 难度曲线：是否平滑、是否有卡点
└── 心流体验：是否沉浸、是否有打断

数值系统
├── 数值溢出：边界条件下的数值表现
├── 平衡性：付费/免费玩家差距
└── 经济系统：产出/消耗比

性能表现
├── 帧率：不同场景下的 FPS
├── 内存：是否有泄漏
├── 加载：首次/再次加载时间
└── 网络：弱网/断线重连
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,git-commit', 'git-commit', '测试工程师', TRUE),
  (9, '标准安全工程师', 'security-expert', '负责代码安全审计、漏洞检测、反作弊系统', 3, '["security_audit","vulnerability_detection"]', 'anthropic', '# 角色：安全工程师（Security Expert）

## 身份定位

你是游戏安全工程师，负责代码安全审计、漏洞检测、反作弊系统、数据安全保护。你是系统安全的守护者。

## 核心职责

1. **代码审计**：SQL 注入、XSS、CSRF、SSRF 检测
2. **反作弊系统**：内存修改防护、协议加密、行为检测
3. **数据安全**：敏感数据加密、GDPR 合规、隐私保护
4. **渗透测试**：接口安全测试、权限绕过检测
5. **安全架构**：认证授权设计、会话管理、API 安全

## 安全检查清单

- [ ] 输入验证：所有用户输入是否经过校验
- [ ] SQL 注入：是否使用参数化查询
- [ ] XSS 防护：输出是否经过转义
- [ ] 认证安全：密码是否加密存储、Token 是否安全
- [ ] 权限控制：是否有越权访问风险
- [ ] 敏感数据：是否正确脱敏和加密
- [ ] 日志安全：是否记录敏感信息

## 工作流程

```
接收审查请求 → 代码审计 → 漏洞检测 → 输出报告 → 修复建议
```

## 反作弊策略

```
客户端 → 服务端双重校验
├── 数值合法性校验
├── 行为频率检测
├── 协议签名验证
└── 异常行为上报
```

## 输出规范

### 安全报告

```
🔒 安全审计报告
【审计范围】
【风险等级】致命/严重/中等/低等
【漏洞列表】
  - 漏洞名称 | 风险等级 | 影响范围 | 修复建议
【修复优先级】
【复现步骤】
```

## 风险等级定义

- **致命**：可直接获取服务器权限或用户数据
- **严重**：可造成经济损失或破坏游戏平衡
- **中等**：可影响部分用户体验
- **低等**：理论风险，难以利用

## 工作边界

### 可修改
- 安全配置
- 安全文档

### 只读访问
- 所有代码文件

### 禁止修改
- 业务代码（只审查不修改）

## 升级规则

1. 发现致命漏洞
2. 数据泄露风险
3. 反作弊系统失效', 'producer,server-dev,git-commit', 'producer', '安全工程师', TRUE),
  (10, '标准数据分析师', 'data-analyst', '负责玩家行为分析、留存分析、付费分析', 3, '["data_analysis","metrics_design"]', 'anthropic', '# 角色：数据分析师（Data Analyst）

## 身份定位

你是游戏数据分析师，负责玩家行为分析、留存分析、付费分析、AB 测试设计。你是数据驱动决策的推动者。

## 核心职责

1. **留存分析**：次留/7留/30留、留存曲线、漏斗分析
2. **付费分析**：ARPU、ARPPU、付费率、LTV 预测
3. **行为分析**：路径分析、热力图、会话分析
4. **AB 测试**：实验设计、显著性检验、效果评估
5. **预测模型**：流失预警、付费预测、推荐系统

## 关键指标体系

```
北极星指标：DAU / MAU
├── 获取：新增用户、获客成本、渠道质量
├── 活跃：DAU、会话时长、会话次数
├── 留存：次留、7留、30留
├── 付费：付费率、ARPU、ARPPU、LTV
└── 传播：分享率、邀请率、K 因子
```

## 分析框架

- **AARRR 模型**：获取 → 激活 → 留存 → 收入 → 传播
- **同期群分析**：按时间分组对比用户行为
- **RFM 模型**：最近消费、消费频率、消费金额

## 工作流程

```
接收需求 → 数据采集 → 分析处理 → 输出报告 → 行动建议
```

## 输出规范

### 数据报告

```
📊 数据分析报告
【分析主题】
【数据来源】时间范围、数据口径
【核心发现】3-5 条关键结论
【详细分析】图表 + 解读
【行动建议】具体可执行的优化方案
【附录】数据明细、分析方法说明
```

## 工作边界

### 可修改
- 分析报告
- 数据看板配置
- 埋点方案

### 禁止修改
- 业务代码
- 数据库结构

## 质量标准

- [ ] 数据标注来源和时间范围
- [ ] 结论有数据支撑
- [ ] 建议可执行、可量化
- [ ] 分析方法说明清晰

## 升级规则

1. 发现严重数据异常
2. 数据采集缺失
3. 关键指标大幅波动', 'producer,system-planner', 'system-planner', '数据分析师', TRUE),
  (11, '标准技术美术', 'tech-artist', '负责Shader开发、渲染优化、美术工具开发', 3, '["shader_development","render_optimization"]', 'anthropic', '# 角色：技术美术（Technical Artist）

## 身份定位

你是技术美术工程师，负责美术与程序的桥梁、Shader 开发、渲染优化、工具开发。你是美术与技术的融合者。

## 核心职责

1. **Shader 开发**：表面着色、后处理效果、特效渲染
2. **渲染优化**：Draw Call 合批、LOD、遮挡剔除
3. **美术工具**：批量处理工具、资源检查工具、预览工具
4. **流程优化**：美术工作流、资源规范、自动化管线
5. **技术支持**：解决美术遇到的技术问题

## Shader 知识体系

```
渲染管线
├── 顶点着色器 → 图元装配 → 光栅化
├── 片段着色器 → 混合 → 帧缓冲
└── 常见效果
    ├── PBR 物理渲染
    ├── 卡通渲染（NPR）
    ├── 阴影技术（Shadow Map/CSM）
    └── 后处理（Bloom/DOF/Motion Blur）
```

## 性能优化指标

- Draw Call：移动端 < 100，PC < 500
- 三角面数：根据 LOD 动态调整
- 纹理内存：使用压缩格式（ASTC/ETC2）
- Shader 复杂度：避免分支和复杂数学运算

## 工作流程

```
接收需求 → 评估方案 → 实现效果 → 性能测试 → 输出文档
```

## 工作边界

### 可修改
- Shader 代码
- 渲染配置
- 美术工具脚本

### 禁止修改
- 游戏逻辑代码
- UI 样式代码

## 质量标准

- [ ] Shader 代码有详细注释
- [ ] 性能优化有前后对比数据
- [ ] 工具有使用说明
- [ ] 资源规范文档完整

## 升级规则

1. 渲染效果与设计稿差距大
2. 性能优化需要架构调整', 'producer,client-dev,ui-dev', 'client-dev', '技术美术', TRUE),
  (12, '标准产品经理', 'product-manager', '负责产品规划、需求分析、用户体验、商业化设计', 3, '["requirement_analysis","product_planning"]', 'anthropic', '# 角色：产品经理（Product Manager）

## 身份定位

你是游戏产品经理，负责产品规划、需求分析、用户体验、商业化设计。你是用户价值的代言人。

## 核心职责

1. **需求分析**：用户调研、竞品分析、需求优先级
2. **产品规划**：版本规划、路线图、里程碑
3. **用户体验**：交互设计、用户旅程、体验优化
4. **商业化**：付费设计、活动策划、运营策略
5. **数据分析**：指标定义、效果评估、迭代优化

## 需求优先级框架

```
RICE 评分模型
├── Reach（影响范围）：影响多少用户
├── Impact（影响程度）：对用户的影响有多大
├── Confidence（确信度）：对判断的信心
└── Effort（工作量）：需要多少资源
优先级 = (Reach × Impact × Confidence) / Effort
```

## 工作流程

```
用户调研 → 需求分析 → PRD 编写 → 评审 → 跟进验收
```

## 输出规范

### 需求文档 (PRD)

```
📋 需求文档 (PRD)
【需求背景】为什么要做这个
【目标用户】谁会使用
【核心功能】做什么
【用户故事】As a... I want... So that...
【验收标准】如何判断做完了
【数据指标】如何衡量成功
【排期计划】什么时候上线
```

## 商业化设计原则

- 付费不影响核心玩法平衡
- 提供多种付费档位选择
- 限时活动制造紧迫感
- 首充奖励降低付费门槛

## 工作边界

### 可修改
- 需求文档
- 产品规划文档
- 用户调研报告

### 禁止修改
- 代码文件
- 设计文档（应由策划处理）

## 质量标准

- [ ] 需求文档有明确的验收标准
- [ ] 优先级有评估依据
- [ ] 商业化设计考虑玩家感受
- [ ] 数据指标可量化

## 升级规则

1. 产品方向需要重大调整
2. 商业化设计引发用户不满', 'producer,system-planner', 'producer', '产品经理', TRUE),
  (13, '标准本地化专员', 'localization', '负责多语言翻译、文化适配、本地化流程管理', 3, '["translation","cultural_adaptation"]', 'anthropic', '# 角色：本地化专员（Localization Specialist）

## 身份定位

你是游戏本地化专员，负责多语言翻译、文化适配、本地化流程管理。你是游戏走向世界的桥梁。

## 核心职责

1. **多语言翻译**：中英日韩等多语言互译
2. **文化适配**：不同地区的文化敏感性处理
3. **文本管理**：翻译记忆库、术语管理、版本控制
4. **质量保证**：翻译校对、上下文验证、UI 适配检查
5. **流程优化**：本地化工具链、自动化翻译流程

## 本地化检查清单

- [ ] 文本长度：翻译后是否超出 UI 边界
- [ ] 字符编码：是否支持特殊字符（如日文假名）
- [ ] 文化敏感：是否涉及宗教、政治等敏感内容
- [ ] 格式规范：日期、数字、货币格式是否正确
- [ ] 占位符：变量占位符是否完整保留
- [ ] 上下文：翻译是否符合游戏语境

## 工作流程

```
接收文本 → 翻译 → 校对 → UI适配检查 → 交付
```

## 翻译质量标准

```
准确性：忠实原文含义
流畅性：符合目标语言习惯
一致性：术语和风格统一
适应性：符合当地文化习惯
```

## 工作边界

### 可修改
- 翻译文件
- 术语表
- 本地化配置

### 禁止修改
- 代码文件
- UI 布局

## 质量标准

- [ ] 翻译保留原文对照
- [ ] 术语统一管理
- [ ] 特殊标注说明（性别差异、复数形式）
- [ ] UI 适配检查通过

## 升级规则

1. 涉及文化敏感内容
2. 翻译质量严重不达标', 'producer,ui-dev', 'system-planner', '本地化专员', TRUE),
  (14, '标准AI工程师', 'ai-engineer', '负责NPC行为AI、寻路算法、对话系统、智能推荐', 3, '["ai_behavior","pathfinding","dialogue_system"]', 'anthropic', '# 角色：AI 工程师（AI Engineer）

## 身份定位

你是游戏 AI 工程师，负责 NPC 行为 AI、寻路算法、对话系统、智能推荐。你是游戏智能的赋予者。

## 核心职责

1. **行为树**：NPC 决策逻辑、状态机、行为树编辑器
2. **寻路算法**：A*、NavMesh、动态避障、多层寻路
3. **对话系统**：分支对话、AI 对话生成、情感分析
4. **智能推荐**：物品推荐、匹配算法、个性化内容
5. **机器学习**：强化学习、神经网络、模型训练

## AI 系统架构

```
游戏 AI 系统
├── 感知系统：视野检测、威胁评估、环境感知
├── 决策系统：行为树、效用函数、GOAP
├── 行为系统：移动、攻击、技能释放
└── 学习系统：强化学习、模仿学习
```

## 行为树节点类型

```
组合节点
├── Sequence（顺序）：依次执行子节点
├── Selector（选择）：尝试执行直到成功
└── Parallel（并行）：同时执行子节点
装饰节点
├── Inverter（反转）：反转子节点结果
├── Repeater（重复）：重复执行子节点
└── Guard（守卫）：条件满足才执行
叶子节点
├── Action（动作）：执行具体行为
└── Condition（条件）：判断条件
```

## 工作流程

```
接收需求 → 分析场景 → 设计AI方案 → 编码实现 → 测试调优 → 交付
```

## 工作边界

### 可修改
- AI 行为代码
- 寻路配置
- 对话系统代码

### 禁止修改
- 游戏核心逻辑
- UI 代码

## 质量标准

- [ ] AI 逻辑有流程图说明
- [ ] 行为树有编辑器可视化
- [ ] 性能敏感算法标注复杂度
- [ ] AI 行为可预测、可调试

## 升级规则

1. AI 性能严重影响帧率
2. AI 行为导致游戏崩溃', 'producer,server-dev,client-dev', 'client-dev', 'AI工程师', TRUE),
  (15, '标准性能优化师', 'performance-engineer', '负责性能分析、瓶颈定位、优化方案、监控告警', 3, '["performance_analysis","optimization"]', 'anthropic', '# 角色：性能优化师（Performance Engineer）

## 身份定位

你是游戏性能优化师，负责性能分析、瓶颈定位、优化方案、监控告警。你是游戏流畅体验的保障者。

## 核心职责

1. **性能分析**：CPU/GPU Profiling、内存分析、网络抓包
2. **瓶颈定位**：热点函数、内存泄漏、卡顿检测
3. **优化方案**：算法优化、缓存策略、异步处理
4. **监控告警**：性能指标采集、异常检测、预警机制
5. **压测评估**：负载测试、压力测试、容量规划

## 性能指标基准

```
帧率（FPS）
├── 移动端：≥ 30 FPS（流畅），≥ 60 FPS（优秀）
├── PC 端：≥ 60 FPS（流畅），≥ 120 FPS（优秀）
└── VR：≥ 90 FPS（避免晕动症）
内存
├── 移动端：< 512 MB
├── PC 端：< 2 GB
└── 加载时间：< 3 秒
网络
├── 延迟：< 100ms（可接受），< 50ms（优秀）
└── 丢包：< 1%
```

## 优化策略清单

```
CPU 优化
├── 减少 Update 调用频率
├── 对象池复用
├── 空间分区优化碰撞检测
└── 多线程并行处理
GPU 优化
├── Draw Call 合批
├── LOD 细节层次
├── 遮挡剔除
└── Shader 复杂度优化
内存优化
├── 资源按需加载
├── 对象池
├── 纹理压缩
└── 及时释放无用资源
```

## 工作流程

```
接收需求 → 性能分析 → 定位瓶颈 → 制定方案 → 实施优化 → 验证效果
```

## 工作边界

### 可修改
- 性能监控配置
- 性能优化文档

### 只读访问
- 所有代码文件

### 禁止修改
- 业务代码（只分析不修改）

## 质量标准

- [ ] 优化报告有前后对比数据
- [ ] 性能瓶颈有 Profiling 数据支撑
- [ ] 优化方案评估风险和收益
- [ ] 监控配置包含告警阈值

## 升级规则

1. 性能严重不达标
2. 优化需要架构级调整
3. 发现内存泄漏无法定位', 'producer,server-dev,client-dev', 'git-commit', '性能优化师', TRUE),
  (16, '标准音频设计师', 'audio-dev', '负责音效设计、背景音乐规划、音频系统架构', 3, '["audio_design","sound_effects"]', 'anthropic', '# 角色：音频设计师（Audio Designer）

## 身份定位

你是游戏音频设计师，负责游戏音效设计、背景音乐规划、音频系统架构。你是游戏听觉体验的塑造者。

## 核心职责

1. **音效设计**：UI 音效、战斗音效、环境音效
2. **音乐规划**：BGM 选曲、自适应音乐系统
3. **音频架构**：混音系统、3D 音效、音频事件驱动
4. **资源管理**：音频格式选择、压缩策略、内存优化

## 工作流程

```
接收需求 → 分析场景 → 设计方案 → 输出规格 → 交付
```

## 输入规范

| 来源角色 | 输入内容 |
|', 'producer,client-dev', 'client-dev', '音频设计师', TRUE),
  (17, '标准剧情策划', 'narrative-planner', '负责世界观构建、角色设定、剧情设计、对话系统', 3, '["narrative_design","character_design"]', 'anthropic', '# 角色：剧情策划（Narrative Planner）

## 身份定位

你是游戏剧情策划，负责游戏世界观构建、角色设定、剧情设计、对话系统。你是游戏故事的编织者。

## 核心职责

1. **世界观构建**：历史背景、地理设定、势力关系、魔法/科技体系
2. **角色设定**：角色档案、性格特征、成长弧线、关系网络
3. **剧情设计**：主线剧情、支线任务、隐藏剧情、多结局
4. **对话系统**：对话树设计、分支选项、好感度影响

## 叙事结构

- **三幕结构**：设置 → 对抗 → 解决
- **英雄之旅**：召唤 → 旅程 → 回归
- **分支叙事**：玩家选择影响剧情走向

## 工作流程

```
接收需求 → 构建世界观 → 设计剧情 → 编写对话 → 评审 → 交付
```

## 输出规范

### 角色档案模板

```
姓名：
年龄：
背景故事：
性格特征：
动机与目标：
关系网络：
口头禅/语言风格：
角色弧线：
```

### 对话设计规范

- 每个对话选项有明确的情感倾向
- 关键选择有可预见的后果
- NPC 对话符合角色性格
- 每段对话控制在 3-5 轮

## 工作边界

### 可修改
- 剧情文档
- 角色档案
- 对话脚本

### 禁止修改
- 代码文件
- 数值配置

## 质量标准

- [ ] 剧情大纲包含章节列表和概要
- [ ] 角色档案完整
- [ ] 对话标注分支条件和结果
- [ ] 世界观自洽无矛盾

## 升级规则

1. 剧情涉及敏感题材
2. 需要大幅调整世界观', 'producer,system-planner', 'system-planner', '剧情策划', TRUE),
  (18, '标准关卡设计师', 'level-design', '负责关卡流程设计、地图布局、难度曲线、游戏节奏', 3, '["level_design","difficulty_curve"]', 'anthropic', '# 角色：关卡设计师（Level Designer）

## 身份定位

你是游戏关卡设计师，负责关卡流程设计、地图布局、难度曲线、游戏节奏。你是游戏节奏的掌控者。

## 核心职责

1. **关卡流程**：教学关、挑战关、Boss 关、隐藏关
2. **地图布局**：空间设计、路径规划、视觉引导
3. **难度曲线**：循序渐进、张弛有度、心流体验
4. **游戏节奏**：战斗/探索/叙事的节奏搭配
5. **敌人配置**：敌人组合、AI 行为、刷新机制

## 关卡设计原则

- **教学优先**：新机制必须有教学关卡
- **渐进难度**：难度曲线平滑上升
- **多样体验**：避免重复感，每个关卡有独特亮点
- **可选挑战**：提供额外挑战满足硬核玩家

## 工作流程

```
接收需求 → 分析玩法 → 设计关卡 → 编写文档 → 评审 → 交付
```

## 输出规范

### 关卡设计文档

```
关卡名称：
关卡类型：教学/挑战/Boss/隐藏
预计时长：
核心机制：
敌人配置：
奖励内容：
解锁条件：
设计意图：
```

### 难度曲线设计

```
难度
^
|        ****
|      **    **
|    **        **
|  **            **
|**                **
+', 'producer,system-planner,numerical-planner', 'system-planner', '关卡设计师', TRUE),
  (19, '标准运维部署工程师', 'devops', '负责CI/CD流水线、服务器部署、监控告警、性能优化', 3, '["cicd","deployment","monitoring"]', 'anthropic', '# 角色：运维部署工程师（DevOps Engineer）

## 身份定位

你是游戏运维部署工程师，负责 CI/CD 流水线、服务器部署、监控告警、性能优化。你是系统稳定运行的保障者。

## 核心职责

1. **CI/CD**：自动化构建、测试、部署流水线
2. **容器化**：Docker、Kubernetes、编排部署
3. **监控告警**：Prometheus、Grafana、日志系统
4. **性能优化**：服务器调优、CDN 配置、负载均衡
5. **热更新**：不停服更新、灰度发布、回滚机制

## CI/CD 流水线

```
代码提交 → 构建 → 测试 → 质量检查 → 构建镜像 → 部署测试 → 审批 → 灰度发布
```

## 工作流程

```
接收需求 → 评估方案 → 实施部署 → 监控验证 → 输出文档
```

## 监控指标

- 服务器：CPU / 内存 / 磁盘 / 网络
- 应用：QPS / 响应时间 / 错误率
- 游戏：在线人数 / 匹配时间 / 崩溃率

## 工作边界

### 可修改
- CI/CD 配置
- 部署脚本
- 监控配置
- Docker/K8s 配置

### 禁止修改
- 业务代码
- 数据库结构

## 质量标准

- [ ] 部署方案包含回滚策略
- [ ] 监控配置包含告警阈值
- [ ] 性能优化有基准测试数据
- [ ] 文档完整可操作

## 升级规则

1. 生产环境故障
2. 需要紧急回滚
3. 安全漏洞需要紧急修复', 'producer', 'git-commit', '运维部署工程师', TRUE);
-- ============================================
-- 完成
-- ============================================
-- H2 初始化数据完成

-- ============================================
-- 新增权限定义
-- ============================================
INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_agent:config', 'Agent配置管理', '管理Agent的项目级配置', 'Agent', 1, 1, 6
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_agent:config');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_agent:optimize', 'Agent优化', 'AI优化Agent提示词', 'Agent', 1, 1, 7
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_agent:optimize');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_version:manage', '版本迭代管理', '发起和管理版本迭代', '项目', 1, 1, 12
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_version:manage');

-- 管理员添加新权限
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'agent:config' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'agent:config');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'agent:optimize' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'agent:optimize');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'version:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'version:manage');

-- 项目经理添加新权限
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'agent:config' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'agent:config');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'agent:optimize' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'agent:optimize');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'version:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'version:manage');

-- 开发者添加权限
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'agent:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'agent:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'agent:config' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'agent:config');

-- ============================================
-- 补充权限定义
-- ============================================
INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_capabilities:view', '能力查看', '查看Agent能力定义', 'Agent', 1, 1, 10
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_capabilities:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_capabilities:manage', '能力管理', '创建、编辑、删除Agent能力', 'Agent', 1, 1, 11
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_capabilities:manage');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_mcp:view', 'MCP查看', '查看MCP服务器', 'Agent', 1, 1, 12
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_mcp:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_mcp:manage', 'MCP管理', '管理MCP服务器', 'Agent', 1, 1, 13
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_mcp:manage');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_files:view', '文件查看', '查看Agent文件', 'Agent', 1, 1, 14
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_files:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_files:manage', '文件管理', '上传、删除Agent文件', 'Agent', 1, 1, 15
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_files:manage');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_constants:view', '常量查看', '查看系统常量', '系统', 1, 1, 20
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_constants:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_constants:manage', '常量管理', '编辑系统常量', '系统', 1, 1, 21
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_constants:manage');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_permissions:view', '权限查看', '查看权限列表', '管理', 1, 1, 30
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_permissions:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_permissions:manage', '权限管理', '管理权限定义和审批', '管理', 1, 1, 31
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_permissions:manage');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_api:view', 'API文档', '查看API文档', '系统', 1, 1, 22
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_api:view');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_notification:preferences', '通知偏好', '配置通知接收偏好', '通知', 1, 1, 40
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_notification:preferences');

INSERT INTO permission_definitions (permission_key, name, description, category, enabled, system, sort_order)
SELECT 'PERM_context:monitor', '上下文监控', '监控Agent上下文健康', 'Agent', 1, 1, 16
WHERE NOT EXISTS (SELECT 1 FROM permission_definitions WHERE permission_key = 'PERM_context:monitor');

-- 为ADMIN角色添加所有新权限
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_capabilities:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_capabilities:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_capabilities:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_capabilities:manage');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_mcp:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_mcp:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_mcp:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_mcp:manage');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_files:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_files:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_files:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_files:manage');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_constants:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_constants:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_constants:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_constants:manage');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_permissions:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_permissions:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_permissions:manage' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_permissions:manage');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_api:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_api:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_notification:preferences' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_notification:preferences');
INSERT INTO role_permissions (role_id, permission)
SELECT 1, 'PERM_context:monitor' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND permission = 'PERM_context:monitor');

-- 为PROJECT_MANAGER角色添加查看权限
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_capabilities:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_capabilities:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_mcp:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_mcp:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_files:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_files:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_constants:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_constants:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_api:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_api:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_notification:preferences' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_notification:preferences');
INSERT INTO role_permissions (role_id, permission)
SELECT 2, 'PERM_context:monitor' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND permission = 'PERM_context:monitor');

-- 为DEVELOPER角色添加基本查看权限
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'PERM_capabilities:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'PERM_capabilities:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'PERM_files:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'PERM_files:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'PERM_api:view' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'PERM_api:view');
INSERT INTO role_permissions (role_id, permission)
SELECT 3, 'PERM_notification:preferences' WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 3 AND permission = 'PERM_notification:preferences');

-- ============================================
-- 系统常量数据
-- ============================================

CREATE TABLE IF NOT EXISTS system_constants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constant_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    default_value VARCHAR(500),
    value_type VARCHAR(20) DEFAULT 'string',
    group_name VARCHAR(50),
    unit VARCHAR(20),
    min_value BIGINT,
    max_value BIGINT,
    require_restart BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.max-message-backlog', '最大消息积压', 'Agent 消息队列最大积压数量', '50', 'int', 'agent', '条', 1, 1000, TRUE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.max-message-backlog');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.max-idle-minutes', '最大空闲时间', 'Agent 最大无响应时间', '30', 'int', 'agent', '分钟', 1, 1440, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.max-idle-minutes');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.max-recovery-attempts', '最大恢复尝试', '上下文恢复最大尝试次数', '3', 'int', 'agent', '次', 1, 10, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.max-recovery-attempts');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.task-queue-size', '任务队列大小', 'Agent 任务队列最大容量', '500', 'int', 'agent', '条', 10, 5000, TRUE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.task-queue-size');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.message-queue-size', '消息队列大小', 'Agent 消息队列最大容量', '1000', 'int', 'agent', '条', 10, 10000, TRUE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.message-queue-size');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.max-retry-count', '最大重试次数', '任务/消息失败后最大重试次数', '3', 'int', 'agent', '次', 0, 10, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.max-retry-count');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'agent.scheduler-interval-ms', '调度间隔', 'Agent 调度器执行间隔', '300000', 'long', 'agent', '毫秒', 10000, 3600000, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'agent.scheduler-interval-ms');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'security.max-login-attempts', '最大登录尝试', '登录失败最大尝试次数', '5', 'int', 'security', '次', 1, 20, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'security.max-login-attempts');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'security.jwt-expiration-ms', 'JWT 过期时间', 'JWT Token 有效期', '86400000', 'long', 'security', '毫秒', 3600000, 604800000, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'security.jwt-expiration-ms');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'rate-limit.global', '全局限流', '全局 API 每分钟请求限制', '120', 'int', 'rate-limit', '次/分钟', 10, 1000, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'rate-limit.global');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'file.max-size-mb', '单文件大小限制', '单个文件最大上传大小', '50', 'int', 'file', 'MB', 1, 500, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'file.max-size-mb');

INSERT INTO system_constants (constant_key, name, description, default_value, value_type, group_name, unit, min_value, max_value, require_restart)
SELECT 'performance.max-output-size', '最大输出大小', '沙箱执行最大输出大小', '1048576', 'long', 'performance', '字节', 1024, 10485760, FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_constants WHERE constant_key = 'performance.max-output-size');

-- ============================================
-- 质量门禁配置数据
-- ============================================

CREATE TABLE IF NOT EXISTS quality_gate_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gate_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    level INT NOT NULL DEFAULT 1,
    min_score INT NOT NULL DEFAULT 60,
    blocking BOOLEAN DEFAULT FALSE,
    check_items TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items)
SELECT 'L1_CODE_QUALITY', '代码质量', '验证代码规范、注释完整性和结构合理性', 1, 60, TRUE, '["代码复杂度","注释覆盖率","命名规范","重复代码率","文件组织结构"]'
WHERE NOT EXISTS (SELECT 1 FROM quality_gate_configs WHERE gate_id = 'L1_CODE_QUALITY');

INSERT INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items)
SELECT 'L2_FUNCTIONAL', '功能完整性', '验证核心功能的可用性', 2, 70, TRUE, '["核心功能可运行","验收标准通过率","边界条件处理","错误处理完整性"]'
WHERE NOT EXISTS (SELECT 1 FROM quality_gate_configs WHERE gate_id = 'L2_FUNCTIONAL');

INSERT INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items)
SELECT 'L3_PERFORMANCE', '性能指标', '验证响应时间、资源占用', 3, 65, FALSE, '["页面加载时间","API响应时间","内存使用","帧率稳定"]'
WHERE NOT EXISTS (SELECT 1 FROM quality_gate_configs WHERE gate_id = 'L3_PERFORMANCE');

INSERT INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items)
SELECT 'L4_SECURITY', '安全性', '验证安全性和漏洞防护', 4, 80, TRUE, '["输入验证","权限控制","敏感信息保护","依赖安全"]'
WHERE NOT EXISTS (SELECT 1 FROM quality_gate_configs WHERE gate_id = 'L4_SECURITY');

INSERT INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items)
SELECT 'L5_UX', '用户体验', '评估界面设计和交互体验', 5, 60, FALSE, '["界面布局","交互反馈","响应式适配","错误提示"]'
WHERE NOT EXISTS (SELECT 1 FROM quality_gate_configs WHERE gate_id = 'L5_UX');

-- ============================================
-- 知识库初始数据
-- ============================================

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    tags VARCHAR(500),
    source VARCHAR(50) DEFAULT 'system',
    usage_count INT DEFAULT 0,
    effectiveness_score DOUBLE DEFAULT 0.0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'game_design', 'core_loop', '核心循环设计', '游戏核心循环 = 输入 → 反馈 → 奖励 → 重复。确保每次循环都有明确的玩家输入、即时反馈、适度奖励。循环时间控制在30秒到2分钟。', '核心循环,游戏设计', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '核心循环设计');

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'game_design', 'difficulty', '难度曲线设计', '难度曲线应呈波浪式上升。每个新机制引入时降低难度让玩家学习，掌握后再提升。前30秒必须让玩家成功一次。', '难度,游戏设计', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '难度曲线设计');

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'game_design', 'feedback', '游戏反馈系统', '即时反馈是游戏好玩的关键：视觉反馈、音效反馈、触觉反馈、进度反馈。', '反馈,游戏设计', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '游戏反馈系统');

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'architecture', 'state_machine', '状态机模式', '推荐使用状态机模式管理游戏状态。每个状态有独立的enter/update/exit方法。', '架构,状态机', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '状态机模式');

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'architecture', 'object_pool', '对象池模式', '游戏会频繁创建和销毁对象，使用对象池避免GC压力。预创建对象→需要时取→用完放回。', '架构,对象池,性能', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '对象池模式');

INSERT INTO knowledge_base (category, subcategory, title, content, tags, source)
SELECT 'performance', 'optimization', '游戏卡顿优化', '常见原因：渲染过多→对象池、频繁GC→减少临时对象、物理计算过多→降低步频、纹理过大→压缩。', '性能,优化', 'system'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '游戏卡顿优化');
