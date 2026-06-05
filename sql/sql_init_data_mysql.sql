-- ============================================
-- ChengXun Game Maker 初始化数据脚本 (MySQL)
-- 版本: 1.0.0
-- 创建时间: 2026-05-30
-- 说明: 初始化系统所需的基础数据
-- 使用方式: mysql -u root -p game_maker < sql_init_data_mysql.sql
-- ============================================

USE game_maker;

-- ============================================
-- 1. 初始化角色数据
-- ============================================

-- 插入系统角色
INSERT INTO roles (name, display_name, description, is_system) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', TRUE),
('PROJECT_MANAGER', '项目经理', '项目经理，负责项目管理', TRUE),
('DEVELOPER', '开发者', '开发者，Agent 管理权限', TRUE),
('OPS_ENGINEER', '运维工程师', '运维工程师，系统运维权限', TRUE),
('OBSERVER', '观察者', '观察者，只读权限', TRUE),
('USER', '普通用户', '普通用户，基本操作权限', TRUE);

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
('system.name', 'ChengXun Game Maker', 'system', 'string', '系统名称', TRUE),
('system.version', '1.0.0', 'system', 'string', '系统版本', TRUE),
('system.description', 'AI 驱动的游戏开发自动化管理系统', 'system', 'string', '系统描述', TRUE),

-- 安全配置
('security.password.min-length', '8', 'security', 'number', '密码最小长度', TRUE),
('security.password.require-uppercase', 'true', 'security', 'boolean', '密码要求大写字母', TRUE),
('security.password.require-lowercase', 'true', 'security', 'boolean', '密码要求小写字母', TRUE),
('security.password.require-number', 'true', 'security', 'boolean', '密码要求数字', TRUE),
('security.password.require-special', 'true', 'security', 'boolean', '密码要求特殊字符', TRUE),
('security.login.max-attempts', '5', 'security', 'number', '最大登录尝试次数', TRUE),
('security.login.lockout-minutes', '30', 'security', 'number', '登录锁定时间（分钟）', TRUE),
('security.session.timeout-minutes', '30', 'security', 'number', '会话超时时间（分钟）', TRUE),

-- Agent 配置
('agent.max-concurrent-tasks', '10', 'agent', 'number', 'Agent 最大并发任务数', TRUE),
('agent.task.retry-count', '3', 'agent', 'number', '任务失败重试次数', TRUE),
('agent.task.timeout-minutes', '60', 'agent', 'number', '任务超时时间（分钟）', TRUE),
('agent.health.check-interval-seconds', '60', 'agent', 'number', '健康检查间隔（秒）', TRUE),

-- 通知配置
('notification.email.enabled', 'false', 'notification', 'boolean', '是否启用邮件通知', TRUE),
('notification.feishu.enabled', 'false', 'notification', 'boolean', '是否启用飞书通知', TRUE),
('notification.dingtalk.enabled', 'false', 'notification', 'boolean', '是否启用钉钉通知', TRUE),

-- 日志配置
('log.retention-days', '30', 'system', 'number', '日志保留天数', TRUE),
('log.export.max-rows', '10000', 'system', 'number', '日志导出最大行数', TRUE);

-- ============================================
-- 5. 初始化告警规则
-- ============================================

-- Agent 相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Agent响应超时', 'Agent工作时间超过30秒', 'agent.work.duration', 'GT', 30000, 'AGENT', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH'),
('Agent错误率过高', 'Agent错误率超过10%', 'agent.error.rate', 'GT', 10, 'AGENT', true, 600, 'SYSTEM,FEISHU', 4, 'HIGH'),
('Agent队列积压', 'Agent待处理任务超过50个', 'agent.queue.size', 'GT', 50, 'AGENT', true, 300, 'SYSTEM', 2, 'MEDIUM'),
('Agent内存使用过高', 'Agent内存使用超过500MB', 'agent.memory.used', 'GT', 500, 'AGENT', true, 600, 'SYSTEM,FEISHU', 3, 'HIGH');

-- 数据库相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('慢SQL过多', '慢SQL数量超过10条/分钟', 'sql.slow.count', 'GT', 10, 'DATABASE', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH'),
('数据库连接池耗尽', '数据库连接池使用率超过90%', 'db.pool.usage', 'GT', 90, 'DATABASE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5, 'CRITICAL'),
('数据库查询超时', '数据库查询平均时间超过1秒', 'db.query.avgtime', 'GT', 1000, 'DATABASE', true, 300, 'SYSTEM', 3, 'HIGH');

-- 缓存相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('Redis连接失败', 'Redis连接异常', 'redis.connection.error', 'GT', 0, 'CACHE', true, 60, 'SYSTEM,FEISHU,EMAIL', 5, 'CRITICAL'),
('Redis内存使用过高', 'Redis内存使用超过80%', 'redis.memory.usage', 'GT', 80, 'CACHE', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH'),
('缓存命中率过低', '缓存命中率低于50%', 'cache.hit.rate', 'LT', 50, 'CACHE', true, 600, 'SYSTEM', 2, 'MEDIUM');

-- 系统相关告警规则
INSERT INTO alert_rules (name, description, metric_name, condition_type, threshold, rule_category, enabled, cooldown_seconds, notification_channels, severity_level, priority) VALUES
('内存使用率过高', 'JVM内存使用率超过80%', 'jvm.memory.usage', 'GT', 80, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 3, 'HIGH'),
('CPU使用率过高', 'CPU使用率超过90%', 'system.cpu.usage', 'GT', 90, 'SYSTEM', true, 300, 'SYSTEM,FEISHU', 4, 'HIGH'),
('磁盘空间不足', '磁盘剩余空间低于20%', 'system.disk.free', 'LT', 20, 'SYSTEM', true, 600, 'SYSTEM,FEISHU,EMAIL', 4, 'HIGH'),
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
-- 完成
-- ============================================
SELECT 'MySQL 初始化数据完成！' AS message;
SELECT COUNT(*) AS '角色数量' FROM roles;
SELECT COUNT(*) AS '权限数量' FROM role_permissions;
SELECT COUNT(*) AS '用户数量' FROM users;
SELECT COUNT(*) AS '系统配置数量' FROM system_configs;
SELECT COUNT(*) AS '告警规则数量' FROM alert_rules;
SELECT COUNT(*) AS '通知模板数量' FROM notification_templates;
