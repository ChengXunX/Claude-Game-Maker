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

-- 插入系统角色（7个内置角色，仅 ADMIN 为系统保护角色）
INSERT IGNORE INTO roles (id, name, display_name, description, is_system) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有所有权限', TRUE),
(2, 'PROJECT_MANAGER', '项目经理', '负责项目管理和 Agent 调度', FALSE),
(3, 'DEVELOPER', '开发者', '使用 Agent 进行开发工作', FALSE),
(4, 'OPS_ENGINEER', '运维工程师', '负责系统运维和部署', FALSE),
(5, 'OBSERVER', '观察者', '只读权限，查看系统状态', FALSE),
(6, 'USER', '普通用户', '普通用户，基础权限', FALSE),
(7, 'READONLY', '只读访客', '只读权限，可查看所有模块但不能修改，供外部人员了解系统特性', FALSE);

-- 兼容已有系统：将非 ADMIN 角色的 is_system 改为 FALSE（仅 ADMIN 受保护）
UPDATE roles SET is_system = FALSE WHERE name != 'ADMIN' AND is_system = TRUE;

-- ============================================
-- 2. 初始化角色权限数据（完整60个权限）
-- ============================================

-- ADMIN 管理员：通配符权限 * + 所有具体权限（JwtAuthenticationFilter.addAllPermissions 会在检测到 * 时自动注入全部权限）
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(1, '*'),
(1, 'dashboard:view'), (1, 'agents:view'), (1, 'agents:manage'), (1, 'agents:task'),
(1, 'agent:view'), (1, 'agent:manage'), (1, 'agent:config'), (1, 'agent:optimize'),
(1, 'ai:use'), (1, 'ai:admin'),
(1, 'projects:view'), (1, 'projects:manage'), (1, 'projects:edit'),
(1, 'skills:view'), (1, 'skills:manage'),
(1, 'tokens:view'), (1, 'tokens:manage'),
(1, 'notification:view'), (1, 'notification:manage'), (1, 'notifications:manage'),
(1, 'code:review'), (1, 'knowledge:manage'),
(1, 'pipeline:view'), (1, 'pipeline:manage'), (1, 'pipeline:create'),
(1, 'pipeline:execute'), (1, 'pipeline:approve'), (1, 'pipeline:intervene'),
(1, 'workflow:view'), (1, 'workflow:manage'),
(1, 'approval:view'), (1, 'approval:manage'),
(1, 'users:view'), (1, 'users:manage'),
(1, 'roles:manage'), (1, 'logs:view'), (1, 'log:view'),
(1, 'system:view'), (1, 'system:monitor'), (1, 'system:monitor:manage'),
(1, 'system:config'), (1, 'system:config:manage'), (1, 'system:manage'),
(1, 'admin:manage'), (1, 'terminal:use'),
(1, 'version:manage'), (1, 'data:view'),
(1, 'capabilities:view'), (1, 'capabilities:manage'),
(1, 'mcp:view'), (1, 'mcp:manage'),
(1, 'files:view'), (1, 'files:manage'),
(1, 'constants:view'), (1, 'constants:manage'),
(1, 'permissions:view'), (1, 'permissions:manage'),
(1, 'api:view'), (1, 'notification:preferences'), (1, 'context:monitor'),
(1, 'iteration:view'), (1, 'iteration:manage'), (1, 'supervision:view'),
(1, 'checkpoint:view'), (1, 'checkpoint:manage'),
(1, 'fork:view'), (1, 'fork:create'), (1, 'fork:manage'),
(1, 'subagent:view'), (1, 'subagent:create'), (1, 'subagent:manage'),
(1, 'tool:permission:manage'),
(1, 'dream:execute'), (1, 'skill:discover'), (1, 'distill:execute'),
(1, 'goal:view'), (1, 'goal:manage'),
(1, 'snapshot:view'), (1, 'snapshot:manage');

-- PROJECT_MANAGER 项目经理：35个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(2, 'dashboard:view'), (2, 'agents:view'), (2, 'agents:manage'), (2, 'agents:task'),
(2, 'agent:config'), (2, 'agent:optimize'),
(2, 'ai:use'),
(2, 'projects:view'), (2, 'projects:manage'), (2, 'projects:edit'),
(2, 'skills:view'), (2, 'skills:manage'),
(2, 'tokens:view'),
(2, 'notification:view'), (2, 'notification:manage'),
(2, 'code:review'),
(2, 'pipeline:view'), (2, 'pipeline:manage'), (2, 'pipeline:create'),
(2, 'pipeline:execute'), (2, 'pipeline:approve'), (2, 'pipeline:intervene'),
(2, 'workflow:view'), (2, 'workflow:manage'),
(2, 'approval:view'),
(2, 'logs:view'), (2, 'system:view'), (2, 'system:monitor'),
(2, 'version:manage'),
(2, 'PERM_capabilities:view'), (2, 'PERM_mcp:view'), (2, 'PERM_files:view'),
(2, 'PERM_constants:view'), (2, 'PERM_api:view'),
(2, 'PERM_notification:preferences'), (2, 'PERM_context:monitor'),
(2, 'iteration:view'), (2, 'iteration:manage'), (2, 'supervision:view'),
(2, 'reasoning:view'), (2, 'reasoning:manage'), (2, 'quality:view'), (2, 'quality:predict'),
(2, 'iteration:adapt'), (2, 'knowledge:graph');

-- DEVELOPER 开发者：18个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(3, 'dashboard:view'), (3, 'agents:view'), (3, 'agents:task'),
(3, 'agent:view'), (3, 'agent:config'),
(3, 'ai:use'),
(3, 'projects:view'),
(3, 'skills:view'),
(3, 'tokens:view'),
(3, 'notification:view'), (3, 'code:review'),
(3, 'pipeline:view'), (3, 'pipeline:execute'),
(3, 'approval:view'),
(3, 'PERM_capabilities:view'), (3, 'PERM_files:view'),
(3, 'PERM_api:view'), (3, 'PERM_notification:preferences'),
(3, 'iteration:view'), (3, 'supervision:view');

-- OPS_ENGINEER 运维工程师：18个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(4, 'dashboard:view'), (4, 'agents:view'),
(4, 'projects:view'),
(4, 'tokens:view'),
(4, 'notification:view'),
(4, 'pipeline:view'), (4, 'pipeline:manage'), (4, 'pipeline:create'),
(4, 'pipeline:execute'), (4, 'pipeline:approve'), (4, 'pipeline:intervene'),
(4, 'workflow:view'), (4, 'workflow:manage'),
(4, 'system:view'), (4, 'system:monitor'), (4, 'system:monitor:manage'),
(4, 'system:config'), (4, 'logs:view'), (4, 'terminal:use');

-- OBSERVER 观察者：14个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(5, 'dashboard:view'), (5, 'agents:view'),
(5, 'ai:use'),
(5, 'projects:view'), (5, 'skills:view'),
(5, 'tokens:view'), (5, 'notification:view'),
(5, 'code:review'),
(5, 'pipeline:view'), (5, 'workflow:view'), (5, 'approval:view'),
(5, 'system:view'), (5, 'system:monitor'), (5, 'logs:view');

-- USER 普通用户：4个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(6, 'dashboard:view'), (6, 'projects:view'),
(6, 'notification:view'), (6, 'skills:view');

-- READONLY 只读访客：15个权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(7, 'dashboard:view'), (7, 'agents:view'), (7, 'ai:use'),
(7, 'projects:view'), (7, 'skills:view'), (7, 'tokens:view'),
(7, 'notification:view'), (7, 'code:review'), (7, 'pipeline:view'),
(7, 'workflow:view'), (7, 'approval:view'), (7, 'users:view'),
(7, 'logs:view'), (7, 'system:view'), (7, 'system:monitor');

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

-- 用户配置
('user.default.role', 'USER', 'system', 'string', '新注册用户默认角色（USER/READONLY/ADMIN等）', TRUE),

-- 安全配置
('security.password.min-length', '8', 'security', 'number', '密码最小长度', TRUE),
('security.password.require-uppercase', 'true', 'security', 'boolean', '密码要求大写字母', TRUE),
('security.password.require-lowercase', 'true', 'security', 'boolean', '密码要求小写字母', TRUE),
('security.password.require-number', 'true', 'security', 'boolean', '密码要求数字', TRUE),
('security.password.require-special', 'true', 'security', 'boolean', '密码要求特殊字符', TRUE),
('security.login.max-attempts', '5', 'security', 'number', '最大登录尝试次数', TRUE),
('security.login.lockout-minutes', '30', 'security', 'number', '登录锁定时间（分钟）', TRUE),
('security.session.timeout-minutes', '30', 'security', 'number', '会话超时时间（分钟）', TRUE),

-- Token 分配策略
('token.allocation.strategy', 'system', 'agent', 'string', 'Token 分配策略（system=系统自动分配, producer=制作人分配）', TRUE),

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
-- ============================================
-- 6. 初始化通知模板（美化版 v2）
-- 新增变量：${domain}、${projectLink}、${approvalLink}、${agentLink}
-- 美化样式：更好的排版、颜色、按钮
-- ============================================

-- ============================================
-- 6.1 飞书通知模板
-- ============================================

-- 审批相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_NEW_FEISHU', '飞书新审批请求', 'FEISHU', 'SYSTEM', '🔔 新审批请求',
'**📋 审批请求**

---

**审批类型**：${requestTypeDesc}
**发起者**：${requesterName}
**项目**：${projectName}

**描述**：${description}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

💡 请点击卡片底部按钮或前往管理后台处理审批',
'新审批请求飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_APPROVED_FEISHU', '飞书审批通过', 'FEISHU', 'SYSTEM', '✅ 审批通过',
'**✅ 审批通过通知**

---

**类型**：${requestTypeDesc}
**描述**：${description}
**审批人**：${approverName}
**审批意见**：${approvalComment}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批通过飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_REJECTED_FEISHU', '飞书审批拒绝', 'FEISHU', 'SYSTEM', '❌ 审批拒绝',
'**❌ 审批拒绝通知**

---

**类型**：${requestTypeDesc}
**描述**：${description}
**审批人**：${approverName}
**拒绝原因**：${approvalComment}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批拒绝飞书通知', TRUE, TRUE);

-- Agent 相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_FEISHU', '飞书Agent通知', 'FEISHU', 'AGENT', '🤖 Agent通知',
'**🤖 Agent通知**

---

**Agent**：${agentName}
**状态**：${status}

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent相关飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_STATUS_FEISHU', '飞书Agent状态变更', 'FEISHU', 'AGENT', '🔄 Agent状态变更',
'**🔄 Agent状态变更**

---

**Agent名称**：${agentName}
**当前状态**：${status}
**备注**：${remark}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent状态变更飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AGENT_CREATED_FEISHU', '飞书Agent已创建', 'FEISHU', 'AGENT', '🤖 Agent已创建',
'**🤖 Agent已创建**

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent已创建飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AGENT_EVALUATED_FEISHU', '飞书Agent评估完成', 'FEISHU', 'AGENT', '📊 Agent评估完成',
'**📊 Agent评估完成**

---

${content}

---

⏰ ${time}

🔗 [查看评估](${domain}/performance)',
'Agent评估完成飞书通知', TRUE, TRUE);

-- 告警相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_FEISHU', '飞书告警通知', 'FEISHU', 'ALERT', '🚨 告警通知',
'**🚨 告警通知**

---

**告警规则**：${ruleName}
**告警级别**：${priorityDesc}
**指标**：${metric}
**当前值**：${triggerValue}
**阈值**：${thresholdValue}

---

${content}

---

⏰ ${time}

🔗 [查看告警](${domain}/alerts)

⚠️ 请及时处理告警，避免影响系统稳定性',
'告警飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_FEISHU', '飞书告警恢复', 'FEISHU', 'ALERT', '✅ 告警恢复',
'**✅ 告警恢复**

---

**告警规则**：${ruleName}
**当前值**：${triggerValue}

---

⏰ ${time}

🔗 [查看告警](${domain}/alerts)

✅ 系统已恢复正常',
'告警恢复飞书通知', TRUE, TRUE);

-- 任务相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_FEISHU', '飞书任务通知', 'FEISHU', 'TASK', '📋 任务通知',
'**📋 任务通知**

---

**任务**：${taskTitle}
**状态**：${status}

---

${content}

---

⏰ ${time}

🔗 [查看任务](${domain}/scheduler)',
'任务飞书通知', TRUE, TRUE);

-- 审批流程相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_HUMAN_APPROVAL_NEEDED_FEISHU', '飞书需要人工审批', 'FEISHU', 'SYSTEM', '🔔 需要人工审批',
'**🔔 需要人工审批**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'需要人工审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_APPROVAL_REQUIRED_FEISHU', '飞书招聘审批', 'FEISHU', 'SYSTEM', '👥 招聘审批请求',
'**👥 招聘审批请求**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批是否允许招聘该 Agent。',
'招聘审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_CREATE_AGENT_APPROVAL_REQUIRED_FEISHU', '飞书创建Agent审批', 'FEISHU', 'SYSTEM', '🤖 创建Agent审批',
'**🤖 创建Agent审批请求**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批。',
'创建Agent审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DELIVERY_APPROVAL_FEISHU', '飞书交付审批', 'FEISHU', 'SYSTEM', '📦 交付审批',
'**📦 交付审批请求**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批是否可以交付。',
'交付审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_APPROVAL_REQUIRED_FEISHU', '飞书需要审批', 'FEISHU', 'SYSTEM', '📋 需要审批',
'**📋 需要审批**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'需要审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_APPROVAL_REJECTED_FEISHU', '飞书审批被拒绝', 'FEISHU', 'SYSTEM', '❌ 审批被拒绝',
'**❌ 审批被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_REJECTED_FEISHU', '飞书招聘被拒绝', 'FEISHU', 'SYSTEM', '❌ 招聘被拒绝',
'**❌ 招聘被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'招聘被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_CREATE_AGENT_REJECTED_FEISHU', '飞书创建Agent被拒绝', 'FEISHU', 'SYSTEM', '❌ 创建Agent被拒绝',
'**❌ 创建Agent被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'创建Agent被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DISMISS_REJECTED_FEISHU', '飞书解雇被拒绝', 'FEISHU', 'SYSTEM', '❌ 解雇被拒绝',
'**❌ 解雇被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'解雇被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DISMISS_REQUEST_SENT_FEISHU', '飞书解雇请求已发送', 'FEISHU', 'SYSTEM', '📤 解雇请求已发送',
'**📤 解雇请求已发送**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)

等待管理员审批。',
'解雇请求已发送飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_COMPLETED_FEISHU', '飞书招聘已完成', 'FEISHU', 'AGENT', '✅ 招聘已完成',
'**✅ 招聘已完成**

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'招聘已完成飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AUTO_RECRUIT_REQUEST_FEISHU', '飞书自动招聘请求', 'FEISHU', 'AGENT', '🔄 自动招聘请求',
'**🔄 自动招聘请求**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/agents)',
'自动招聘请求飞书通知', TRUE, TRUE);

-- 项目状态相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_MILESTONE_STUCK_FEISHU', '飞书里程碑卡住', 'FEISHU', 'SYSTEM', '⚠️ 里程碑卡住',
'**⚠️ 里程碑卡住**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

请关注项目进展。',
'里程碑卡住飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PROJECT_STUCK_FEISHU', '飞书项目卡住', 'FEISHU', 'SYSTEM', '🚨 项目卡住',
'**🚨 项目卡住**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

项目进展受阻，请及时干预。',
'项目卡住飞书通知', TRUE, TRUE);

-- 里程碑完成通知
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_MILESTONE_COMPLETED_FEISHU', '飞书里程碑完成', 'FEISHU', 'SYSTEM', '✅ 里程碑完成',
'**✅ 里程碑完成**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

项目进展顺利！',
'里程碑完成飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_ALL_MILESTONES_COMPLETED_FEISHU', '飞书所有里程碑完成', 'FEISHU', 'SYSTEM', '🎉 所有里程碑完成',
'**🎉 所有里程碑完成**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

项目进入审查阶段！',
'所有里程碑完成飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PROJECT_RULES_GENERATED_FEISHU', '飞书项目规则已生成', 'FEISHU', 'SYSTEM', '📜 项目规则已生成',
'**📜 项目规则已生成**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'项目规则已生成飞书通知', TRUE, TRUE);

-- 版本迭代相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_ITERATION_STARTED_FEISHU', '飞书版本迭代已启动', 'FEISHU', 'SYSTEM', '🔄 版本迭代已启动',
'**🔄 版本迭代已启动**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'版本迭代已启动飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_STAFFING_ISSUE_FEISHU', '飞书人员配置问题', 'FEISHU', 'SYSTEM', '👥 人员配置问题',
'**👥 版本人员配置问题**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'人员配置问题飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_LOW_PERFORMANCE_FEISHU', '飞书版本低绩效', 'FEISHU', 'SYSTEM', '📉 版本低绩效',
'**📉 版本低绩效预警**

---

${content}

---

⏰ ${time}

🔗 [查看绩效](${domain}/performance)',
'版本低绩效飞书通知', TRUE, TRUE);

-- 质量改进迭代通知
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_QUALITY_ITERATION_FEISHU', '飞书质量改进迭代', 'FEISHU', 'SYSTEM', '⚠️ 质量改进迭代',
'**⚠️ 质量改进迭代**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

系统将继续迭代直到质量达标。',
'质量改进迭代飞书通知', TRUE, TRUE);

-- 工作流相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_STARTED_FEISHU', '飞书工作流已启动', 'FEISHU', 'SYSTEM', '🚀 工作流已启动',
'**🚀 工作流已启动**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'工作流已启动飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_CREATED_FEISHU', '飞书新工作流已创建', 'FEISHU', 'SYSTEM', '📋 新工作流已创建',
'**📋 新工作流已创建**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'新工作流已创建飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_PRODUCER_APPROVED_FEISHU', '飞书工作流已自动审批', 'FEISHU', 'SYSTEM', '✅ 工作流已自动审批',
'**✅ 工作流已自动审批**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'工作流已自动审批飞书通知', TRUE, TRUE);

-- 团队相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_TEAM_OPTIMIZATION_FEISHU', '飞书团队优化', 'FEISHU', 'AGENT', '🔧 团队优化建议',
'**🔧 团队优化建议**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看团队](${domain}/agents)',
'团队优化飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_TEAM_WARNING_FEISHU', '飞书团队警告', 'FEISHU', 'SYSTEM', '⚠️ 团队警告',
'**⚠️ 团队警告**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看团队](${domain}/agents)',
'团队警告飞书通知', TRUE, TRUE);

-- 其他
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PERIODIC_VERIFY_FAILED_FEISHU', '飞书定期验证失败', 'FEISHU', 'SYSTEM', '❌ 定期验证失败',
'**❌ 定期验证失败**

---

**项目**: ${projectName}

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'定期验证失败飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('DISMISSAL_REQUEST_FEISHU', '飞书解雇审批请求', 'FEISHU', 'SYSTEM', '🔔 解雇审批请求',
'**🔔 解雇审批请求**

---

**Agent名称**：${agentName}
**申请原因**：${reason}
**申请人**：${requesterName}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'解雇审批请求飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PERFORMANCE_FEISHU', '飞书绩效通知', 'FEISHU', 'SYSTEM', '📊 绩效通知',
'**📊 绩效通知**

---

${content}

---

⏰ ${time}

🔗 [查看绩效](${domain}/performance)',
'绩效飞书通知', TRUE, TRUE);

-- ============================================
-- 6.2 邮件通知模板（美化版）
-- ============================================

-- 审批相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_NEW_EMAIL', '邮件新审批请求', 'EMAIL', 'SYSTEM', '🔔 [审批] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📋 新审批请求</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>审批类型：</strong>${requestTypeDesc}</p>
    <p><strong>发起者：</strong>${requesterName}</p>
    <p><strong>项目：</strong>${projectName}</p>
    <p><strong>描述：</strong>${description}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #f5576c; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">前往审批</a>
    </div>
  </div>
</div>',
'新审批请求邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_APPROVED_EMAIL', '邮件审批通过', 'EMAIL', 'SYSTEM', '✅ [审批通过] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">✅ 审批通过</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>类型：</strong>${requestTypeDesc}</p>
    <p><strong>描述：</strong>${description}</p>
    <p><strong>审批人：</strong>${approverName}</p>
    <p><strong>审批意见：</strong>${approvalComment}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #43e97b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看详情</a>
    </div>
  </div>
</div>',
'审批通过邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_REJECTED_EMAIL', '邮件审批拒绝', 'EMAIL', 'SYSTEM', '❌ [审批拒绝] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">❌ 审批拒绝</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>类型：</strong>${requestTypeDesc}</p>
    <p><strong>描述：</strong>${description}</p>
    <p><strong>审批人：</strong>${approverName}</p>
    <p><strong>拒绝原因：</strong>${approvalComment}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #ff6b6b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看详情</a>
    </div>
  </div>
</div>',
'审批拒绝邮件通知', TRUE, TRUE);

-- Agent 相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_EMAIL', '邮件Agent通知', 'EMAIL', 'AGENT', '🤖 Agent通知: ${agentName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🤖 Agent通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>Agent：</strong>${agentName}</p>
    <p><strong>状态：</strong>${status}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/agents" style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看Agent</a>
    </div>
  </div>
</div>',
'Agent邮件通知', TRUE, TRUE);

-- 告警相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_EMAIL', '邮件告警通知', 'EMAIL', 'ALERT', '🚨 [${priorityDesc}] ${ruleName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🚨 告警通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>告警规则：</strong>${ruleName}</p>
    <p><strong>告警级别：</strong>${priorityDesc}</p>
    <p><strong>指标：</strong>${metric}</p>
    <p><strong>当前值：</strong>${triggerValue}</p>
    <p><strong>阈值：</strong>${thresholdValue}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/alerts" style="display: inline-block; background: #ff6b6b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看告警</a>
    </div>
    <p style="color: #dc3545; font-weight: bold; margin-top: 15px;">⚠️ 请及时处理告警，避免影响系统稳定性</p>
  </div>
</div>',
'告警邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_EMAIL', '邮件告警恢复', 'EMAIL', 'ALERT', '✅ [恢复] ${ruleName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">✅ 告警恢复</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>告警规则：</strong>${ruleName}</p>
    <p><strong>当前值：</strong>${triggerValue}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/alerts" style="display: inline-block; background: #43e97b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看告警</a>
    </div>
    <p style="color: #28a745; font-weight: bold; margin-top: 15px;">✅ 系统已恢复正常</p>
  </div>
</div>',
'告警恢复邮件通知', TRUE, TRUE);

-- 任务相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_EMAIL', '邮件任务通知', 'EMAIL', 'TASK', '📋 任务通知: ${taskTitle}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📋 任务通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>任务：</strong>${taskTitle}</p>
    <p><strong>状态：</strong>${status}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/scheduler" style="display: inline-block; background: #a18cd1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看任务</a>
    </div>
  </div>
</div>',
'任务邮件通知', TRUE, TRUE);

-- 审批流程相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_HUMAN_APPROVAL_NEEDED_EMAIL', '邮件需要人工审批', 'EMAIL', 'SYSTEM', '🔔 需要人工审批: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff922b 0%, #fd7e14 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔔 需要人工审批</h1>
  </div>
  <div style="background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <div style="background:#e8f4fd;padding:12px;border-radius:6px;margin-bottom:15px;border-left:4px solid #2196F3;">
      <p style="margin:4px 0;font-size:14px;"><b>项目：</b>${projectName}</p>
      <p style="margin:4px 0;font-size:14px;"><b>创建人：</b>${createdBy}</p>
      <p style="margin:4px 0;font-size:14px;"><b>描述：</b>${projectDescription}</p>
    </div>
    <p>${content}</p>
    <p style="color: #666; margin-top: 20px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #fd7e14; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">前往审批</a>
    </div>
  </div>
</div>',
'需要人工审批邮件通知', TRUE, TRUE);

-- 版本迭代相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_ITERATION_STARTED_EMAIL', '邮件版本迭代已启动', 'EMAIL', 'SYSTEM', '🔄 版本迭代已启动: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #339af0 0%, #228be6 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔄 版本迭代已启动</h1>
  </div>
  <div style="background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <div style="background:#e8f4fd;padding:12px;border-radius:6px;margin-bottom:15px;border-left:4px solid #2196F3;">
      <p style="margin:4px 0;font-size:14px;"><b>项目：</b>${projectName}</p>
      <p style="margin:4px 0;font-size:14px;"><b>创建人：</b>${createdBy}</p>
      <p style="margin:4px 0;font-size:14px;"><b>描述：</b>${projectDescription}</p>
    </div>
    <p>${content}</p>
    <p style="color: #666; margin-top: 20px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/projects" style="display: inline-block; background: #228be6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看项目</a>
    </div>
  </div>
</div>',
'版本迭代已启动邮件通知', TRUE, TRUE);

-- 绩效相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PERFORMANCE_EMAIL', '邮件绩效通知', 'EMAIL', 'SYSTEM', '📊 绩效通知: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📊 绩效通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/performance" style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看绩效</a>
    </div>
  </div>
</div>',
'绩效邮件通知', TRUE, TRUE);

-- 验证码邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('VERIFICATION_EMAIL', '邮件验证码', 'EMAIL', 'SYSTEM', '${systemName} - 邮箱验证码',
'<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📧 邮箱验证码</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>您好，</p>
    <p>您的邮箱验证码是：</p>
    <div style="text-align: center; margin: 30px 0;">
      <span style="font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px;">${code}</span>
    </div>
    <p>验证码有效期为 ${expireMinutes} 分钟，请尽快使用。</p>
    <p style="color: #dc3545; font-size: 14px;">⚠️ 如果这不是您的操作，请忽略此邮件。</p>
  </div>
</div>',
'邮箱验证码通知', TRUE, TRUE);
-- 10. Agent 预设数据（含角色提示词）
-- ============================================


-- ============================================
-- 11. Agent 能力初始化数据
-- ============================================

-- 制作人能力（producer）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
-- 团队管理类
('producer', 'createAgent', '招聘团队成员', '根据项目需求招聘新的 Agent 团队成员', 'team_management', TRUE, 'CREATE_AGENT', 1, '{"name":"string|required","role":"string|required","workDir":"string","description":"string"}', 'java', TRUE),
('producer', 'deleteAgent', '解雇团队成员', '解雇不适合的 Agent 团队成员', 'team_management', TRUE, 'DELETE_AGENT', 2, '{"agentId":"string|required","reason":"string|required"}', 'java', TRUE),
('producer', 'assignWorkDir', '分配工作职责', '为 Agent 分配或调整工作职责和工作目录', 'team_management', FALSE, NULL, 3, '{"agentId":"string|required","workDir":"string|required"}', 'java', TRUE),
('producer', 'assignApiConfig', '配置工作环境', '为 Agent 配置 API 密钥、模型等开发环境', 'team_management', FALSE, NULL, 4, '{"agentId":"string|required","apiUrl":"string","model":"string"}', 'java', TRUE),
('producer', 'changeAgentConfig', '调整成员配置', '调整 Agent 的配置参数以优化工作表现', 'team_management', FALSE, NULL, 5, '{"agentId":"string|required","configKey":"string|required","configValue":"string|required"}', 'java', TRUE),
('producer', 'optimizeAgentRole', '优化成员能力', '根据项目进展优化 Agent 的能力组合和工作方式', 'team_management', FALSE, NULL, 6, '{"agentId":"string|required","optimizationType":"enum:capabilities,workflow,focus|required","reason":"string"}', 'java', TRUE),
('producer', 'addAgentCapability', '追加Agent能力', '为项目下的 Agent 追加已有的系统能力，扩展其工作范围', 'team_management', FALSE, NULL, 7, '{"agentId":"string|required","capabilityName":"string|required","reason":"string"}', 'java', TRUE),
('producer', 'createAgentCapability', '新建Agent能力', '为项目下的 Agent 创建全新的自定义能力（Prompt类型）', 'team_management', FALSE, NULL, 8, '{"agentId":"string|required","capabilityName":"string|required","displayName":"string|required","description":"string|required","promptTemplate":"string|required","paramSchema":"string"}', 'java', TRUE),
('producer', 'evaluateAgentPerformance', '评估成员绩效', '定期评估 Agent 的工作表现，提供反馈和改进建议', 'team_management', FALSE, NULL, 9, '{"agentId":"string|required","evaluationCriteria":"string","period":"string"}', 'java', TRUE),
-- 项目管理类
('producer', 'setProjectGoal', '设定项目目标', '设定或调整项目的整体目标和方向', 'project_management', FALSE, NULL, 10, '{"goal":"string|required","goalType":"enum:GAME_DEVELOPMENT,BUG_FIX,FEATURE,REFACTOR,CUSTOM|required","deadline":"string"}', 'java', TRUE),
('producer', 'decomposeGoal', '分解项目目标', '将项目目标分解为可执行的里程碑和任务', 'project_management', FALSE, NULL, 11, '{"milestones":"string|required","assignRoles":"string"}', 'java', TRUE),
('producer', 'sendTaskToAgent', '分配任务', '向指定 Agent 分配具体的工作任务', 'task_management', FALSE, NULL, 12, '{"targetAgent":"string|required","taskContent":"string|required","priority":"enum:low,medium,high,urgent","deadline":"string"}', 'java', TRUE),
('producer', 'broadcastMessage', '广播通知', '向项目内所有 Agent 发送重要通知或指令', 'task_management', FALSE, NULL, 13, '{"content":"string|required","messageType":"enum:info,warning,urgent"}', 'java', TRUE),
-- 监控与报告类
('producer', 'queryAgentStatus', '查询成员状态', '查询指定 Agent 的当前工作状态和任务进展', 'monitoring', FALSE, NULL, 20, '{"agentId":"string|required"}', 'java', TRUE),
('producer', 'getProjectStatus', '获取项目状态', '获取项目的整体进展、里程碑完成情况和团队状态', 'monitoring', FALSE, NULL, 21, '{"detailLevel":"enum:summary,normal,detailed|default=normal"}', 'java', TRUE),
('producer', 'generateProgressReport', '生成进度报告', '生成项目进度报告，用于汇报和存档', 'monitoring', FALSE, NULL, 22, '{"reportType":"enum:daily,weekly,milestone,custom|required","recipients":"string"}', 'java', TRUE),
('producer', 'alertRisk', '风险预警', '向管理员发送风险预警通知', 'monitoring', FALSE, NULL, 23, '{"riskType":"string|required","severity":"enum:low,medium,high,critical|required","description":"string|required","suggestedAction":"string"}', 'java', TRUE),
-- 游戏验证类
('producer', 'verifyGameProject', '验证游戏项目', '验证游戏项目的结构完整性和代码质量', 'verification', FALSE, NULL, 30, '{"projectDir":"string","includeQualityAnalysis":"boolean|default=true"}', 'java', TRUE),
('producer', 'verifyGameQuality', '深度质量分析', '使用 AI 深度分析游戏的可玩性、玩法完整性、UI/UX 质量', 'verification', FALSE, NULL, 31, '{"projectDir":"string","projectName":"string","projectGoal":"string"}', 'java', TRUE),
-- 版本迭代类
('producer', 'evaluateVersion', '评估当前版本', '评估当前版本的质量和完成度，给出1-10分的评分', 'version_management', FALSE, NULL, 32, '{"projectId":"string"}', 'java', TRUE),
('producer', 'planNextVersion', '规划下一版本', '根据当前版本评估结果，规划下一版本的目标和里程碑', 'version_management', FALSE, NULL, 33, '{"projectId":"string","currentScore":"number"}', 'java', TRUE),
('producer', 'upgradeVersion', '执行版本升级', '创建新版本历史，重置里程碑状态，开始下一版本迭代', 'version_management', FALSE, NULL, 34, '{"projectId":"string","newVersion":"string|required","newGoal":"string","reason":"string"}', 'java', TRUE),
('producer', 'checkVersionIteration', '检查版本迭代', '检查当前版本是否完成，是否需要进入下一版本迭代', 'version_management', FALSE, NULL, 35, '{"projectId":"string"}', 'java', TRUE),
('producer', 'stopAllProjectAgents', '停止项目Agent', '目标完成时停止项目内所有Agent', 'version_management', FALSE, NULL, 36, '{"projectId":"string","reason":"string"}', 'java', TRUE),
-- 通用能力
('producer', 'sendMessage', '发送消息', '向其他 Agent 发送消息', 'communication', FALSE, NULL, 40, '{"targetAgent":"string|required","content":"string|required","type":"string"}', 'java', TRUE),
('producer', 'saveKnowledge', '保存知识', '将知识保存到记忆系统', 'knowledge', FALSE, NULL, 41, '{"key":"string|required","value":"string|required"}', 'java', TRUE),
('producer', 'reportStatus', '汇报状态', '向管理员汇报当前工作状态', 'communication', FALSE, NULL, 42, '{"status":"string|required","details":"string"}', 'java', TRUE);

-- ===== 任务完成能力（所有 Agent 通用） =====
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
('producer', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('system-planner', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('numerical-planner', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('server-dev', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('client-dev', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('ui-dev', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('tester', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('git-commit', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE),
('multi-agent', 'completeTask', '完成任务', '标记任务完成并通知制作人', 'task_management', FALSE, NULL, 50, '{"taskId":"string|required","milestoneId":"string","result":"string|required","summary":"string"}', 'java', TRUE);

-- ===== system-planner 系统策划能力 =====
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
-- 项目管理类
('system-planner', 'getProjectStatus', '获取项目状态', '获取项目的整体进展、里程碑完成情况和团队状态', 'project_management', FALSE, NULL, 1, '{"detailLevel":"enum:summary,normal,detailed|default=normal"}', 'java', TRUE),
('system-planner', 'updateMilestone', '更新里程碑', '更新里程碑的状态或进度', 'project_management', FALSE, NULL, 2, '{"milestoneId":"string|required","status":"enum:PENDING,IN_PROGRESS,COMPLETED,BLOCKED","progress":"number"}', 'java', TRUE),
('system-planner', 'updateTask', '更新任务', '更新任务的状态或信息', 'project_management', FALSE, NULL, 3, '{"milestoneId":"string|required","taskId":"string|required","title":"string","description":"string","assignedRole":"string","priority":"enum:LOW,MEDIUM,HIGH,CRITICAL"}', 'java', TRUE),
('system-planner', 'addTaskToMilestone', '添加任务', '为里程碑添加新任务', 'project_management', FALSE, NULL, 4, '{"milestoneId":"string|required","title":"string|required","description":"string","assignedRole":"string","priority":"enum:LOW,MEDIUM,HIGH,CRITICAL|default=MEDIUM"}', 'java', TRUE),
-- 知识管理类
('system-planner', 'queryKnowledge', '查询知识', '从知识库中查询相关知识', 'knowledge', FALSE, NULL, 10, '{"query":"string|required","category":"string","limit":"number|default=10"}', 'java', TRUE),
('system-planner', 'saveKnowledge', '保存知识', '将知识保存到记忆系统', 'knowledge', FALSE, NULL, 11, '{"key":"string|required","value":"string|required"}', 'java', TRUE),
-- 通信类
('system-planner', 'sendMessage', '发送消息', '向其他 Agent 发送消息', 'communication', FALSE, NULL, 20, '{"targetAgent":"string|required","content":"string|required","type":"string"}', 'java', TRUE),
('system-planner', 'reportStatus', '汇报状态', '向管理员汇报当前工作状态', 'communication', FALSE, NULL, 21, '{"status":"string|required","details":"string"}', 'java', TRUE),
-- 监控类
('system-planner', 'queryAgentStatus', '查询成员状态', '查询指定 Agent 的当前工作状态和任务进展', 'monitoring', FALSE, NULL, 30, '{"agentId":"string|required"}', 'java', TRUE);

-- ===== numerical-planner 数值策划能力 =====
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
-- 项目管理类
('numerical-planner', 'getProjectStatus', '获取项目状态', '获取项目的整体进展、里程碑完成情况和团队状态', 'project_management', FALSE, NULL, 1, '{"detailLevel":"enum:summary,normal,detailed|default=normal"}', 'java', TRUE),
('numerical-planner', 'updateMilestone', '更新里程碑', '更新里程碑的状态或进度', 'project_management', FALSE, NULL, 2, '{"milestoneId":"string|required","status":"enum:PENDING,IN_PROGRESS,COMPLETED,BLOCKED","progress":"number"}', 'java', TRUE),
('numerical-planner', 'updateTask', '更新任务', '更新任务的状态或信息', 'project_management', FALSE, NULL, 3, '{"milestoneId":"string|required","taskId":"string|required","title":"string","description":"string","assignedRole":"string","priority":"enum:LOW,MEDIUM,HIGH,CRITICAL"}', 'java', TRUE),
-- 知识管理类
('numerical-planner', 'queryKnowledge', '查询知识', '从知识库中查询相关知识', 'knowledge', FALSE, NULL, 10, '{"query":"string|required","category":"string","limit":"number|default=10"}', 'java', TRUE),
('numerical-planner', 'saveKnowledge', '保存知识', '将知识保存到记忆系统', 'knowledge', FALSE, NULL, 11, '{"key":"string|required","value":"string|required"}', 'java', TRUE),
-- 通信类
('numerical-planner', 'sendMessage', '发送消息', '向其他 Agent 发送消息', 'communication', FALSE, NULL, 20, '{"targetAgent":"string|required","content":"string|required","type":"string"}', 'java', TRUE),
('numerical-planner', 'reportStatus', '汇报状态', '向管理员汇报当前工作状态', 'communication', FALSE, NULL, 21, '{"status":"string|required","details":"string"}', 'java', TRUE);

-- ===== Agent 工具能力：快照、会话分叉、子代理、工具权限 =====
-- 快照管理（所有开发类角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
('producer', 'createSnapshot', '创建快照', '对指定文件创建快照，保存当前状态以便后续恢复', 'snapshot', FALSE, NULL, 90, '{"projectId":"string|required","agentId":"string|required","filePaths":"array|required","description":"string"}', 'java', TRUE),
('producer', 'listSnapshots', '查看快照', '查看指定项目和 Agent 的所有快照列表', 'snapshot', FALSE, NULL, 91, '{"projectId":"string|required","agentId":"string|required"}', 'java', TRUE),
('producer', 'restoreSnapshot', '恢复快照', '恢复指定快照，将文件还原到快照时的状态', 'snapshot', FALSE, NULL, 92, '{"projectId":"string|required","agentId":"string|required","snapshotId":"string|required"}', 'java', TRUE),
('producer', 'undoSnapshot', '撤销快照恢复', '撤销最近一次快照恢复操作', 'snapshot', FALSE, NULL, 93, '{"projectId":"string|required","agentId":"string|required"}', 'java', TRUE),
('server-dev', 'createSnapshot', '创建快照', '对指定文件创建快照', 'snapshot', FALSE, NULL, 90, '{"projectId":"string|required","agentId":"string|required","filePaths":"array|required","description":"string"}', 'java', TRUE),
('server-dev', 'listSnapshots', '查看快照', '查看快照列表', 'snapshot', FALSE, NULL, 91, '{"projectId":"string|required","agentId":"string|required"}', 'java', TRUE),
('server-dev', 'restoreSnapshot', '恢复快照', '恢复指定快照', 'snapshot', FALSE, NULL, 92, '{"projectId":"string|required","agentId":"string|required","snapshotId":"string|required"}', 'java', TRUE),
('server-dev', 'undoSnapshot', '撤销快照恢复', '撤销最近一次快照恢复', 'snapshot', FALSE, NULL, 93, '{"projectId":"string|required","agentId":"string|required"}', 'java', TRUE);

-- 会话分叉（制作人和高级角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
('producer', 'createSessionFork', '创建会话分叉', '从当前会话分叉出一个探索性分支', 'session', FALSE, NULL, 95, '{"projectId":"string|required","agentId":"string|required","description":"string"}', 'java', TRUE),
('producer', 'listSessionForks', '查看会话分叉', '查看所有会话分叉', 'session', FALSE, NULL, 96, '{"parentAgentId":"string|required"}', 'java', TRUE),
('producer', 'mergeSessionFork', '合并会话分叉', '将分叉的上下文合并回主会话', 'session', FALSE, NULL, 97, '{"forkId":"string|required","strategy":"string"}', 'java', TRUE),
('producer', 'discardSessionFork', '丢弃会话分叉', '丢弃不需要的会话分叉', 'session', FALSE, NULL, 98, '{"forkId":"string|required"}', 'java', TRUE),
('server-dev', 'createSessionFork', '创建会话分叉', '从当前会话分叉出探索分支', 'session', FALSE, NULL, 95, '{"projectId":"string|required","agentId":"string|required","description":"string"}', 'java', TRUE),
('server-dev', 'listSessionForks', '查看会话分叉', '查看所有会话分叉', 'session', FALSE, NULL, 96, '{"parentAgentId":"string|required"}', 'java', TRUE),
('server-dev', 'mergeSessionFork', '合并会话分叉', '合并分叉回主会话', 'session', FALSE, NULL, 97, '{"forkId":"string|required","strategy":"string"}', 'java', TRUE),
('server-dev', 'discardSessionFork', '丢弃会话分叉', '丢弃不需要的分叉', 'session', FALSE, NULL, 98, '{"forkId":"string|required"}', 'java', TRUE);

-- 子代理（制作人和高级角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
('producer', 'spawnSubAgent', '创建子代理', '创建子代理来并行处理子任务', 'subagent', FALSE, NULL, 100, '{"parentAgentId":"string|required","projectId":"string|required","task":"string|required","role":"string"}', 'java', TRUE),
('producer', 'listSubAgents', '查看子代理', '查看所有子代理', 'subagent', FALSE, NULL, 101, '{"parentAgentId":"string|required"}', 'java', TRUE),
('producer', 'terminateSubAgent', '终止子代理', '终止运行中的子代理', 'subagent', FALSE, NULL, 102, '{"subAgentId":"string|required"}', 'java', TRUE),
('server-dev', 'spawnSubAgent', '创建子代理', '创建子代理并行处理子任务', 'subagent', FALSE, NULL, 100, '{"parentAgentId":"string|required","projectId":"string|required","task":"string|required","role":"string"}', 'java', TRUE),
('server-dev', 'listSubAgents', '查看子代理', '查看所有子代理', 'subagent', FALSE, NULL, 101, '{"parentAgentId":"string|required"}', 'java', TRUE),
('server-dev', 'terminateSubAgent', '终止子代理', '终止运行中的子代理', 'subagent', FALSE, NULL, 102, '{"subAgentId":"string|required"}', 'java', TRUE);

-- 工具权限（仅制作人）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, approval_type, priority, param_schema, execution_type, enabled) VALUES
('producer', 'setToolPermissions', '设置工具权限', '设置 Agent 的工具调用权限规则', 'security', FALSE, NULL, 105, '{"agentId":"string|required","permissions":"array|required"}', 'java', TRUE);

-- ============================================
-- MySQL 初始化数据完成
-- ============================================

SELECT 'MySQL 初始化数据完成！' AS message;
SELECT COUNT(*) AS '角色数量' FROM roles;
SELECT COUNT(*) AS '权限数量' FROM role_permissions;
SELECT COUNT(*) AS '用户数量' FROM users;
SELECT COUNT(*) AS '系统配置数量' FROM system_configs;
SELECT COUNT(*) AS '告警规则数量' FROM alert_rules;
SELECT COUNT(*) AS '通知模板数量' FROM notification_templates;
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

-- ============================================
-- 权限定义表
-- ============================================
CREATE TABLE IF NOT EXISTS permission_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    `system` TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_key (permission_key),
    INDEX idx_pd_category (category),
    INDEX idx_pd_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 权限定义（全量60个，使用 PERM_ 前缀格式）
-- ============================================
INSERT IGNORE INTO permission_definitions (permission_key, name, description, category, enabled, `system`, sort_order) VALUES
('PERM_dashboard:view', '仪表盘查看', '查看仪表盘和系统概览', '工作台', 1, 1, 1),
('PERM_agents:view', 'Agent查看', '查看Agent列表、状态、详情', 'Agent', 1, 1, 1),
('PERM_agents:manage', 'Agent管理', '启动、停止、重启Agent，修改Agent配置', 'Agent', 1, 1, 2),
('PERM_agents:task', 'Agent任务', '向Agent发送任务和指令', 'Agent', 1, 1, 3),
('PERM_agent:view', 'Agent查看(兼容)', '查看Agent列表（兼容别名）', 'Agent', 1, 1, 4),
('PERM_agent:manage', 'Agent管理(兼容)', '管理Agent（兼容别名）', 'Agent', 1, 1, 5),
('PERM_ai:use', 'AI助手使用', '使用AI助手进行对话', 'AI', 1, 1, 1),
('PERM_ai:admin', 'AI助手管理', '管理AI配置、知识库、技能生成', 'AI', 1, 1, 2),
('PERM_projects:view', '项目查看', '查看项目列表和详情', '项目', 1, 1, 1),
('PERM_projects:manage', '项目管理', '创建、编辑、删除项目，管理项目配置', '项目', 1, 1, 2),
('PERM_projects:edit', '项目编辑', '编辑项目配置和设置', '项目', 1, 1, 3),
('PERM_skills:view', '技能查看', '查看技能列表和详情', '技能', 1, 1, 1),
('PERM_skills:manage', '技能管理', '创建、编辑、删除技能，AI生成技能', '技能', 1, 1, 2),
('PERM_tokens:view', 'Token查看', '查看Token列表和用量统计', 'Token', 1, 1, 1),
('PERM_tokens:manage', 'Token管理', '创建、编辑、删除Token，分配Token给Agent', 'Token', 1, 1, 2),
('PERM_notification:view', '通知查看', '查看系统通知和消息', '通知', 1, 1, 1),
('PERM_notification:manage', '通知管理', '管理系统通知、模板、清理无效通知', '通知', 1, 1, 2),
('PERM_code:review', '代码审查', '查看和执行代码审查', '代码', 1, 1, 1),
('PERM_knowledge:manage', '知识库管理', '管理知识库、知识进化、文档索引', '知识库', 1, 1, 1),
('PERM_pipeline:view', '流水线查看', '查看CI/CD流水线列表和状态', '流水线', 1, 1, 1),
('PERM_pipeline:manage', '流水线管理', '创建、编辑、删除流水线', '流水线', 1, 1, 2),
('PERM_pipeline:create', '流水线创建', '创建新的流水线', '流水线', 1, 1, 3),
('PERM_pipeline:execute', '流水线执行', '触发流水线执行', '流水线', 1, 1, 4),
('PERM_pipeline:approve', '流水线审批', '审批流水线执行请求', '流水线', 1, 1, 5),
('PERM_pipeline:intervene', '流水线干预', '干预正在执行的流水线', '流水线', 1, 1, 6),
('PERM_workflow:view', '工作流查看', '查看工作流模板和实例', '工作流', 1, 1, 1),
('PERM_workflow:manage', '工作流管理', '创建、编辑、管理工作流', '工作流', 1, 1, 2),
('PERM_approval:view', '审批查看', '查看审批记录和流程', '审批', 1, 1, 1),
('PERM_approval:manage', '审批管理', '处理审批请求，批准或驳回', '审批', 1, 1, 2),
('PERM_users:view', '用户查看', '查看用户列表和详情', '用户', 1, 1, 1),
('PERM_users:manage', '用户管理', '创建、编辑、删除用户，审批注册', '用户', 1, 1, 2),
('PERM_roles:manage', '角色管理', '创建、编辑、删除角色，分配权限', '角色', 1, 1, 1),
('PERM_logs:view', '日志查看', '查看操作日志和审计记录', '日志', 1, 1, 1),
('PERM_system:view', '系统查看', '查看系统信息和状态', '系统', 1, 1, 1),
('PERM_system:monitor', '系统监控', '查看系统监控、资源用量、Agent健康', '系统', 1, 1, 2),
('PERM_system:monitor:manage', '监控管理', '管理告警规则、处理告警', '系统', 1, 1, 3),
('PERM_system:config', '配置查看', '查看系统配置和常量', '系统', 1, 1, 4),
('PERM_system:config:manage', '配置管理', '修改系统配置和常量', '系统', 1, 1, 5),
('PERM_system:manage', '系统管理', '系统级管理操作', '系统', 1, 1, 6),
('PERM_admin:manage', '管理后台', '访问管理后台功能', '系统', 1, 1, 7),
('PERM_terminal:use', '终端使用', '使用系统终端执行命令', '系统', 1, 1, 8),
('PERM_version:manage', '版本迭代管理', '发起和管理版本迭代', '项目', 1, 1, 9),
('PERM_agent:config', 'Agent配置管理', '管理Agent的项目级配置', 'Agent', 1, 1, 10),
('PERM_agent:optimize', 'Agent优化', 'AI优化Agent提示词', 'Agent', 1, 1, 11),
('PERM_data:view', '数据查看', '查看数据和报表', '数据', 1, 1, 1),
('PERM_notifications:manage', '通知管理(兼容)', '管理通知（兼容别名）', '通知', 1, 1, 3),
('PERM_log:view', '日志查看(兼容)', '查看日志（兼容别名）', '日志', 1, 1, 2),
('PERM_capabilities:view', '能力查看', '查看Agent能力定义', 'Agent', 1, 1, 20),
('PERM_capabilities:manage', '能力管理', '创建、编辑、删除Agent能力', 'Agent', 1, 1, 21),
('PERM_mcp:view', 'MCP查看', '查看MCP服务器', 'Agent', 1, 1, 22),
('PERM_mcp:manage', 'MCP管理', '管理MCP服务器', 'Agent', 1, 1, 23),
('PERM_files:view', '文件查看', '查看Agent文件', 'Agent', 1, 1, 24),
('PERM_files:manage', '文件管理', '上传、删除Agent文件', 'Agent', 1, 1, 25),
('PERM_constants:view', '常量查看', '查看系统常量', '系统', 1, 1, 30),
('PERM_constants:manage', '常量管理', '编辑系统常量', '系统', 1, 1, 31),
('PERM_permissions:view', '权限查看', '查看权限列表', '管理', 1, 1, 40),
('PERM_permissions:manage', '权限管理', '管理权限定义和审批', '管理', 1, 1, 41),
('PERM_api:view', 'API文档', '查看API文档', '系统', 1, 1, 32),
('PERM_notification:preferences', '通知偏好', '配置通知接收偏好', '通知', 1, 1, 50),
('PERM_context:monitor', '上下文监控', '监控Agent上下文健康', 'Agent', 1, 1, 26),
('PERM_iteration:view', '迭代查看', '查看版本迭代记录', '项目', 1, 1, 51),
('PERM_iteration:manage', '迭代管理', '管理版本迭代', '项目', 1, 1, 52),
('PERM_supervision:view', '督查查看', '查看督查报告和协作效率', '项目', 1, 1, 53),
('PERM_checkpoint:view', '检查点查看', '查看Agent检查点列表', 'Agent', 1, 1, 60),
('PERM_checkpoint:manage', '检查点管理', '创建、恢复、删除检查点', 'Agent', 1, 1, 61),
('PERM_fork:view', '会话分叉查看', '查看会话分叉列表', 'Agent', 1, 1, 62),
('PERM_fork:create', '会话分叉创建', '创建会话分叉', 'Agent', 1, 1, 63),
('PERM_fork:manage', '会话分叉管理', '合并、丢弃会话分叉', 'Agent', 1, 1, 64),
('PERM_subagent:view', '子代理查看', '查看子代理列表和状态', 'Agent', 1, 1, 65),
('PERM_subagent:create', '子代理创建', '创建子代理', 'Agent', 1, 1, 66),
('PERM_subagent:manage', '子代理管理', '终止子代理', 'Agent', 1, 1, 67),
('PERM_tool:permission:manage', '工具权限管理', '管理Agent工具权限配置', 'Agent', 1, 1, 68),
('PERM_dream:execute', '知识提取', '执行Dream知识提取', '知识库', 1, 1, 70),
('PERM_skill:discover', 'Skill发现', '发现文件系统Skill', 'Agent', 1, 1, 71),
('PERM_distill:execute', '工作流发现', '执行Distill工作流发现', 'Agent', 1, 1, 72),
('PERM_goal:view', '目标查看', '查看项目目标', '项目', 1, 1, 73),
('PERM_goal:manage', '目标管理', '管理项目目标', '项目', 1, 1, 74),
('PERM_snapshot:view', '快照查看', '查看Agent快照', 'Agent', 1, 1, 75),
('PERM_snapshot:manage', '快照管理', '管理Agent快照', 'Agent', 1, 1, 76),
('PERM_reasoning:view', '推理查看', '查看多轮推理记录', 'Agent', 1, 1, 77),
('PERM_reasoning:manage', '推理管理', '触发和管理多轮推理', 'Agent', 1, 1, 78),
('PERM_quality:view', '质量预测查看', '查看质量预测结果', '项目', 1, 1, 79),
('PERM_quality:predict', '质量预测执行', '执行质量预测', '项目', 1, 1, 80),
('PERM_iteration:adapt', '迭代适应', '应用迭代策略建议', '项目', 1, 1, 81),
('PERM_knowledge:graph', '知识图谱', '查看和构建知识图谱', '项目', 1, 1, 82);

-- 权限映射已在上方完整定义，无需补充

-- ============================================
-- 系统常量数据（来自 SystemConstants.java）
-- ============================================

-- 创建系统常量表（如果不存在）
CREATE TABLE IF NOT EXISTS system_constants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constant_key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    value VARCHAR(500) NOT NULL,
    default_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) DEFAULT 'string',
    group_name VARCHAR(50) NOT NULL,
    unit VARCHAR(20),
    min_value BIGINT,
    max_value BIGINT,
    require_restart BOOLEAN DEFAULT FALSE,
    system_builtin BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sc_group (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统常量表';

-- Agent 相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('agent.max-message-backlog', '最大消息积压', 'Agent 消息队列最大积压数量，超过此数视为上下文异常', '50', '50', 'int', 'agent', '条', 1, 1000, TRUE),
('agent.max-idle-minutes', '最大空闲时间', 'Agent 最大无响应时间，超过视为上下文失效', '30', '30', 'int', 'agent', '分钟', 1, 1440, FALSE),
('agent.max-recovery-attempts', '最大恢复尝试', '上下文恢复最大尝试次数，超过则停止 Agent', '3', '3', 'int', 'agent', '次', 1, 10, FALSE),
('agent.task-queue-size', '任务队列大小', 'Agent 任务队列最大容量', '500', '500', 'int', 'agent', '条', 10, 5000, TRUE),
('agent.message-queue-size', '消息队列大小', 'Agent 消息队列最大容量', '1000', '1000', 'int', 'agent', '条', 10, 10000, TRUE),
('agent.max-retry-count', '最大重试次数', '任务/消息失败后最大重试次数', '3', '3', 'int', 'agent', '次', 0, 10, FALSE),
('agent.history-size', '历史记录大小', '任务/消息历史保留数量', '1000', '1000', 'int', 'agent', '条', 100, 10000, FALSE),
('agent.scheduler-interval-ms', '调度间隔', 'Agent 调度器执行间隔', '300000', '300000', 'long', 'agent', '毫秒', 10000, 3600000, FALSE),
('agent.max-warnings', '最大警告次数', '绩效管理最大警告次数，超过触发解雇流程', '3', '3', 'int', 'agent', '次', 1, 10, FALSE),
('agent.max-learned-knowledge', '最大知识条数', 'Agent 最大学习知识数量', '100', '100', 'int', 'agent', '条', 10, 1000, FALSE),
('agent.capability-prompt-cache-ttl', '能力Prompt缓存时间', '能力列表Prompt缓存有效期，避免每次调用都重建', '300', '300', 'int', 'agent', '秒', 10, 3600, FALSE),
('agent.collaboration-context-cache-ttl', '协作上下文缓存时间', '协作上下文缓存有效期，避免每次调用都重建', '60', '60', 'int', 'agent', '秒', 10, 300, FALSE),
('agent.message-dedup-window-seconds', '消息去重窗口', '相同来源和类型的连续消息在此时间窗口内合并', '30', '30', 'int', 'agent', '秒', 5, 300, FALSE),
('agent.collaboration-context-inject', '协作上下文注入开关', '是否在Agent发送消息时自动注入团队协作上下文', 'true', 'true', 'boolean', 'agent', '', NULL, NULL, FALSE),
('agent.context-window-size', '上下文窗口大小', 'Agent对话历史保留的消息数', '50', '50', 'int', 'agent', '条', 10, 200, FALSE),
('agent.context-compact-token-threshold', '上下文压缩Token阈值', '会话累计Token超过此值触发自动压缩', '80000', '80000', 'long', 'agent', 'token', 10000, 500000, FALSE),
('context.collaboration-max-length', '协作上下文最大长度', '协作上下文注入的最大字符数', '2000', '2000', 'int', 'context', '字符', 500, 10000, FALSE);

-- 文件相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('file.max-size-mb', '单文件大小限制', '单个文件最大上传大小', '50', '50', 'int', 'file', 'MB', 1, 500, FALSE),
('file.quota-mb', 'Agent 存储配额', '每个 Agent 的文件存储配额', '500', '500', 'int', 'file', 'MB', 10, 5000, FALSE),
('file.storage-dir', '存储目录', '文件存储根目录', 'data/files', 'data/files', 'string', 'file', '', 0, 0, TRUE),
('file.version-limit', '版本保留数', '同名文件保留的最大版本数', '10', '10', 'int', 'file', '个', 1, 100, FALSE);

-- 安全相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('security.max-login-attempts', '最大登录尝试', '登录失败最大尝试次数，超过锁定账户', '5', '5', 'int', 'security', '次', 1, 20, FALSE),
('security.jwt-expiration-ms', 'JWT 过期时间', 'JWT Token 有效期', '86400000', '86400000', 'long', 'security', '毫秒', 3600000, 604800000, FALSE),
('security.session-timeout-minutes', '会话超时', 'Web 会话超时时间', '30', '30', 'int', 'security', '分钟', 5, 480, FALSE);

-- 限流相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('rate-limit.global', '全局限流', '全局 API 每分钟请求限制', '120', '120', 'int', 'rate-limit', '次/分钟', 10, 1000, FALSE),
('rate-limit.auth', '认证限流', '登录接口每分钟请求限制', '10', '10', 'int', 'rate-limit', '次/分钟', 1, 100, FALSE),
('rate-limit.write', '写操作限流', '写操作每分钟请求限制', '60', '60', 'int', 'rate-limit', '次/分钟', 5, 500, FALSE);

-- 性能相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('performance.max-backups', '最大备份数', '文件备份最大保留数量', '10', '10', 'int', 'performance', '份', 1, 100, FALSE),
('performance.max-output-size', '最大输出大小', '沙箱执行最大输出大小', '1048576', '1048576', 'long', 'performance', '字节', 1024, 10485760, FALSE),
('performance.ws-session-timeout', 'WebSocket 超时', '终端 WebSocket 会话超时时间', '1800000', '1800000', 'long', 'performance', '毫秒', 60000, 86400000, FALSE),
('performance.context-compact-threshold', '上下文压缩阈值', '触发自动压缩的消息数', '50', '50', 'int', 'performance', '条', 10, 500, FALSE);

-- 通知和MCP相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('notification.batch-size', '批量通知大小', '批量发送通知的批次大小', '100', '100', 'int', 'notification', '条', 10, 1000, FALSE),
('mcp.discovery-timeout-ms', 'MCP 发现超时', 'MCP 工具发现超时时间', '10000', '10000', 'long', 'mcp', '毫秒', 1000, 60000, FALSE);

-- 版本迭代相关常量
INSERT IGNORE INTO system_constants (constant_key, display_name, description, value, default_value, value_type, group_name, unit, min_value, max_value, require_restart) VALUES
('version.pass-score', '版本验收通过分数', '版本质量评估通过的最低分数（1-10分）', '7', '7', 'int', 'version', '分', 1, 10, FALSE),
('version.max-iterations', '最大迭代次数', '项目最大版本迭代次数，防止无限循环', '10', '10', 'int', 'version', '次', 1, 100, FALSE),
('version.min-iterations', '最小迭代次数', '项目最少版本迭代次数，不可低于此数', '1', '1', 'int', 'version', '次', 1, 50, FALSE),
('version.iteration-strategy', '迭代策略', '版本迭代策略：incremental（增量迭代）、full（全量迭代）、adaptive（自适应迭代）', 'adaptive', 'adaptive', 'string', 'version', '', NULL, NULL, FALSE);

-- ============================================
-- 版本评估维度配置
-- ============================================

CREATE TABLE IF NOT EXISTS version_evaluation_dimensions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dimension_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    weight INT NOT NULL DEFAULT 10,
    criteria TEXT,
    evaluation_prompt TEXT,
    min_score INT NOT NULL DEFAULT 5,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    system_builtin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ved_enabled (enabled),
    INDEX idx_ved_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版本评估维度配置表';

INSERT IGNORE INTO version_evaluation_dimensions (dimension_key, display_name, description, weight, criteria, min_score, enabled, display_order, system_builtin) VALUES
('functionality', '功能完整性', '核心功能是否完整实现，游戏是否可玩', 30, '核心玩法循环完整、主要功能可用、无阻断性Bug', 6, TRUE, 1, TRUE),
('code_quality', '代码质量', '代码结构、可维护性、规范性', 20, '代码结构清晰、注释完整、无明显坏味道、通过静态检查', 5, TRUE, 2, TRUE),
('ux', '游戏体验', '可玩性、流畅度、用户界面', 25, '操作流畅、界面美观、交互反馈及时、新手引导完善', 5, TRUE, 3, TRUE),
('goal_alignment', '目标达成度', '是否达到项目目标和验收标准', 25, '项目目标基本达成、验收标准通过率>80%', 6, TRUE, 4, TRUE);

-- ============================================
-- 版本迭代记录表
-- ============================================

CREATE TABLE IF NOT EXISTS version_iteration_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    evaluation_score INT NOT NULL,
    passed BOOLEAN NOT NULL,
    evaluation_details TEXT,
    strengths TEXT,
    improvements TEXT,
    recommendation TEXT,
    need_next_version BOOLEAN NOT NULL,
    next_version VARCHAR(50),
    next_goal TEXT,
    next_milestones TEXT,
    plan_reason TEXT,
    completed_milestones INT NOT NULL DEFAULT 0,
    total_milestones INT NOT NULL DEFAULT 0,
    result VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vir_project (project_id),
    INDEX idx_vir_version (project_id, version),
    INDEX idx_vir_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版本迭代记录表';

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_qgc_level (level),
    INDEX idx_qgc_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量门禁配置表';

INSERT IGNORE INTO quality_gate_configs (gate_id, name, description, level, min_score, blocking, check_items) VALUES
('L1_CODE_QUALITY', '代码质量', '验证代码规范、注释完整性和结构合理性', 1, 60, TRUE, '["代码复杂度（圈复杂度 < 15）","注释覆盖率（> 30%）","命名规范（驼峰/帕斯卡命名）","重复代码率（< 10%）","文件组织结构合理性"]'),
('L2_FUNCTIONAL', '功能完整性', '验证核心功能的可用性和验收标准通过率', 2, 70, TRUE, '["核心功能可运行","验收标准通过率（> 80%）","边界条件处理","错误处理完整性","用户交互响应正确"]'),
('L3_PERFORMANCE', '性能指标', '验证响应时间、资源占用等性能指标', 3, 65, FALSE, '["页面加载时间（< 3秒）","API 响应时间（< 500ms）","内存使用合理（< 512MB）","帧率稳定（> 30fps）","无内存泄漏"]'),
('L4_SECURITY', '安全性', '验证安全性和漏洞防护', 4, 80, TRUE, '["输入验证（防 XSS/SQL 注入）","权限控制（接口鉴权）","敏感信息不泄露（无硬编码密码）","依赖无已知高危漏洞","CSRF 防护"]'),
('L5_UX', '用户体验', '评估界面设计和交互体验', 5, 60, FALSE, '["界面布局合理","交互反馈及时","响应式适配","错误提示友好","操作流程顺畅"]');

-- ============================================
-- 工作流模板数据
-- ============================================

INSERT IGNORE INTO workflow_templates (id, name, description, steps_json, builtin) VALUES
('workflow-producer', '制作人工作流', '制作人的标准工作流程', '{"steps":[{"id":"plan","name":"规划","agentRole":"producer","description":"制定迭代计划"},{"id":"assign","name":"分配任务","agentRole":"producer","description":"将任务分配给对应Agent"},{"id":"monitor","name":"监控执行","agentRole":"producer","description":"跟踪任务进度"},{"id":"review","name":"审查质量","agentRole":"producer","description":"评估完成质量"},{"id":"report","name":"汇报状态","agentRole":"producer","description":"向管理员报告"}]}', TRUE),
('workflow-system-planner', '系统策划工作流', '系统策划的标准工作流程', '{"steps":[{"id":"analyze","name":"需求分析","agentRole":"system-planner","description":"分析项目需求"},{"id":"design","name":"系统设计","agentRole":"system-planner","description":"设计游戏系统"},{"id":"document","name":"文档编写","agentRole":"system-planner","description":"编写设计文档"},{"id":"review","name":"设计评审","agentRole":"system-planner","description":"评审设计方案"}]}', TRUE),
('workflow-numerical-planner', '数值策划工作流', '数值策划的标准工作流程', '{"steps":[{"id":"analyze","name":"数值需求分析","agentRole":"numerical-planner","description":"分析数值需求"},{"id":"design","name":"数值设计","agentRole":"numerical-planner","description":"设计游戏数值"},{"id":"balance","name":"平衡性测试","agentRole":"numerical-planner","description":"测试数值平衡"},{"id":"adjust","name":"数值调整","agentRole":"numerical-planner","description":"调整数值参数"}]}', TRUE),
('workflow-server-dev', '服务端开发工作流', '服务端开发的标准工作流程', '{"steps":[{"id":"design","name":"技术设计","agentRole":"server-dev","description":"设计技术方案"},{"id":"implement","name":"代码实现","agentRole":"server-dev","description":"编写后端代码"},{"id":"test","name":"单元测试","agentRole":"server-dev","description":"编写和运行测试"},{"id":"review","name":"代码审查","agentRole":"server-dev","description":"审查代码质量"}]}', TRUE),
('workflow-tester', '测试工程师工作流', '测试工程师的标准工作流程', '{"steps":[{"id":"plan","name":"测试计划","agentRole":"tester","description":"制定测试计划"},{"id":"cases","name":"测试用例","agentRole":"tester","description":"编写测试用例"},{"id":"execute","name":"执行测试","agentRole":"tester","description":"执行测试用例"},{"id":"report","name":"测试报告","agentRole":"tester","description":"生成测试报告"}]}', TRUE),
('test-fix-loop', '测试-修复循环流程', '测试驱动的质量保证流程：测试→发现问题→分配修复→重新测试，循环直到所有测试通过', '{"steps":[{"id":"analyze","name":"需求分析","agentRole":"system-planner","description":"分析功能需求，明确测试范围和验收标准"},{"id":"write-test","name":"编写测试用例","agentRole":"tester","description":"根据验收标准编写测试用例，覆盖正常路径、边界条件、异常场景"},{"id":"execute-test","name":"执行测试","agentRole":"tester","description":"执行测试用例，记录测试结果，生成测试报告","loopUntilSuccess":true,"maxLoopIterations":5,"loopCondition":"所有测试用例通过","feedbackOnFailure":"测试失败，请根据测试报告修复以下问题","loopBodyStepIds":["fix-bug","execute-test"]},{"id":"fix-bug","name":"修复Bug","agentRole":"server-dev","description":"根据测试报告修复发现的Bug，确保修复不引入新问题"},{"id":"regression","name":"回归测试","agentRole":"tester","description":"执行回归测试，验证所有修复的Bug已通过且未引入新Bug","requiresApproval":true,"importance":"HIGH"},{"id":"deploy","name":"部署上线","agentRole":"git-commit","description":"合并代码并部署到生产环境，进行线上冒烟测试","requiresApproval":true,"importance":"CRITICAL"}]}', TRUE);

-- ============================================
-- 知识库初始数据
-- ============================================

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    knowledge_key VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    project_id VARCHAR(100),
    created_by VARCHAR(100),
    access_level VARCHAR(20) NOT NULL DEFAULT 'public',
    required_permissions VARCHAR(500),
    tags VARCHAR(500),
    priority INT NOT NULL DEFAULT 10,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_category (category),
    INDEX idx_kb_key (knowledge_key),
    INDEX idx_kb_project (project_id),
    INDEX idx_kb_access (access_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 游戏设计知识
INSERT IGNORE INTO knowledge_base (category, knowledge_key, title, content, tags, access_level) VALUES
('game_design', 'game_design.core_loop', '核心循环设计', '游戏核心循环 = 输入 → 反馈 → 奖励 → 重复。确保每次循环都有明确的玩家输入、即时反馈（视觉/音效）、适度奖励（分数/道具/解锁）。循环时间控制在 30 秒到 2 分钟之间。', '核心循环,游戏设计,反馈', 'public'),
('game_design', 'game_design.difficulty', '难度曲线设计', '难度曲线应呈波浪式上升：Easy → Medium → Hard → Easy（奖励关）→ Harder。每个新机制引入时降低难度让玩家学习，掌握后再提升。前 30 秒必须让玩家成功一次。', '难度,游戏设计,平衡', 'public'),
('game_design', 'game_design.feedback', '游戏反馈系统', '即时反馈是游戏好玩的关键：1.视觉反馈：消除时的粒子效果、得分时的数字弹出 2.音效反馈：成功音、失败音、背景音乐变化 3.触觉反馈：移动端震动 4.进度反馈：分数、等级、进度条、成就解锁', '反馈,游戏设计,体验', 'public'),
('game_design', 'game_design.level_design', '关卡设计原则', '关卡设计三要素：目标、挑战、奖励。目标：每个关卡有明确的胜利条件。挑战：逐步引入新机制，不要一次性堆砌。奖励：通关后给予有意义的奖励。前 3 关作为教程关。', '关卡,游戏设计,教程', 'public'),
('game_design', 'game_design.ui_design', '游戏 UI/UX 设计', '游戏 UI 原则：1.信息层次：最重要的信息最大最显眼 2.操作反馈：按钮点击有动画和音效 3.状态可见：始终显示当前分数、生命值、目标 4.容错设计：重要操作有确认弹窗 5.响应式：适配不同屏幕尺寸', 'UI,UX,游戏设计,界面', 'public');

-- 性能优化知识
INSERT IGNORE INTO knowledge_base (category, knowledge_key, title, content, tags, access_level) VALUES
('performance', 'performance.optimization', '游戏卡顿优化', '常见原因和解决方案：1.渲染过多对象→使用对象池、视锥剔除 2.频繁GC→减少临时对象创建 3.物理计算过多→降低物理步频 4.纹理过大→压缩纹理、使用图集 5.过多定时器→合并定时器', '性能,优化,卡顿', 'public'),
('performance', 'performance.memory', '内存优化', '内存优化策略：1.对象池复用 2.资源卸载 3.内存监控 4.垃圾回收优化 5.避免内存泄漏', '性能,内存,优化', 'public'),
('performance', 'performance.network', '网络优化', '网络优化策略：1.数据压缩 2.请求合并 3.缓存策略 4.CDN加速 5.预测补偿', '性能,网络,优化', 'public');

-- 架构设计知识
INSERT IGNORE INTO knowledge_base (category, knowledge_key, title, content, tags, access_level) VALUES
('architecture', 'architecture.state_machine', '状态机模式', '推荐使用状态机模式管理游戏状态：MENU → PLAYING → PAUSED → GAME_OVER → MENU。每个状态有独立的 enter/update/exit 方法。状态切换时清理前一个状态的资源。', '架构,状态机,设计模式', 'public'),
('architecture', 'architecture.object_pool', '对象池模式', '游戏会频繁创建和销毁对象（如子弹、特效），使用对象池避免GC压力。预创建对象→需要时取→用完放回→不产生垃圾。', '架构,对象池,性能,设计模式', 'public'),
('architecture', 'architecture.event_system', '事件系统', '使用事件总线解耦游戏系统：发布者发送事件→事件总线路由→订阅者处理。优点：系统间低耦合、易于扩展、支持异步处理。', '架构,事件系统,解耦,设计模式', 'public');

-- ============================================
-- 完成统计
-- ============================================
SELECT 'MySQL 初始化数据完成！' AS message;
SELECT COUNT(*) AS '角色数量' FROM roles;
SELECT COUNT(*) AS '权限数量' FROM role_permissions;
SELECT COUNT(*) AS '用户数量' FROM users;
SELECT COUNT(*) AS '系统配置数量' FROM system_configs;
SELECT COUNT(*) AS '告警规则数量' FROM alert_rules;
SELECT COUNT(*) AS '通知模板数量' FROM notification_templates;
SELECT COUNT(*) AS 'Agent预设数量' FROM agent_presets;
SELECT COUNT(*) AS '权限定义数量' FROM permission_definitions;
SELECT COUNT(*) AS '系统常量数量' FROM system_constants;
SELECT COUNT(*) AS '质量门禁数量' FROM quality_gate_configs;
SELECT COUNT(*) AS '工作流模板数量' FROM workflow_templates;
SELECT COUNT(*) AS '知识库条目数量' FROM knowledge_base;
