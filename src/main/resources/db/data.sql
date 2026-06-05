-- ============================================
-- ChengXun Game Maker 默认数据
-- 版本: 2.0.0
-- 更新时间: 2026-06-03
-- ============================================

-- 插入默认角色
INSERT IGNORE INTO roles (id, name, display_name, description, is_system) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有所有权限', TRUE),
(2, 'PROJECT_MANAGER', '项目经理', '负责项目管理和 Agent 调度', TRUE),
(3, 'DEVELOPER', '开发者', '使用 Agent 进行开发工作', TRUE),
(4, 'OPS_ENGINEER', '运维工程师', '负责系统运维和部署', TRUE),
(5, 'OBSERVER', '观察者', '只读权限，查看系统状态', TRUE),
(6, 'USER', '普通用户', '普通用户，基础权限', TRUE);

-- 插入管理员角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(1, 'system:monitor'),
(1, 'admin:manage'),
(1, 'agent:view'),
(1, 'approval:manage'),
(1, 'ai:admin'),
(1, 'skills:manage'),
(1, 'workflow:view'),
(1, 'agent:manage'),
(1, 'system:monitor:manage'),
(1, 'notification:manage'),
(1, 'pipeline:manage'),
(1, 'notification:view'),
(1, 'agents:manage'),
(1, 'code:review'),
(1, 'skills:view'),
(1, 'logs:view'),
(1, 'roles:manage'),
(1, 'pipeline:view'),
(1, 'ai:use'),
(1, 'agents:task'),
(1, 'system:config:manage'),
(1, 'knowledge:manage'),
(1, 'pipeline:approve'),
(1, 'log:view'),
(1, 'dashboard:view'),
(1, 'terminal:use'),
(1, 'system:view'),
(1, 'workflow:manage'),
(1, 'projects:view'),
(1, 'pipeline:execute'),
(1, 'users:manage'),
(1, 'system:config'),
(1, 'pipeline:intervene'),
(1, 'tokens:view'),
(1, 'projects:manage'),
(1, 'system:manage'),
(1, 'approval:view'),
(1, 'tokens:manage'),
(1, 'agents:view');

-- 插入项目经理角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(2, 'system:monitor'),
(2, 'pipeline:approve'),
(2, 'dashboard:view'),
(2, 'projects:view'),
(2, 'workflow:manage'),
(2, 'workflow:view'),
(2, 'pipeline:execute'),
(2, 'projects:edit'),
(2, 'notification:manage'),
(2, 'pipeline:manage'),
(2, 'pipeline:create'),
(2, 'agents:manage'),
(2, 'pipeline:intervene'),
(2, 'code:review'),
(2, 'skills:view'),
(2, 'projects:manage'),
(2, 'agents:task'),
(2, 'ai:use'),
(2, 'pipeline:view'),
(2, 'agents:view');

-- 插入开发者角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(3, 'code:review'),
(3, 'dashboard:view'),
(3, 'projects:view'),
(3, 'skills:view'),
(3, 'pipeline:execute'),
(3, 'agents:task'),
(3, 'ai:use'),
(3, 'pipeline:view'),
(3, 'agents:view');

-- 插入运维工程师角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(4, 'system:monitor'),
(4, 'pipeline:approve'),
(4, 'dashboard:view'),
(4, 'projects:view'),
(4, 'workflow:manage'),
(4, 'workflow:view'),
(4, 'pipeline:execute'),
(4, 'system:monitor:manage'),
(4, 'pipeline:manage'),
(4, 'pipeline:create'),
(4, 'pipeline:intervene'),
(4, 'pipeline:view'),
(4, 'agents:view');

-- 插入观察者角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(5, 'system:monitor'),
(5, 'dashboard:view'),
(5, 'projects:view'),
(5, 'skills:view'),
(5, 'ai:use'),
(5, 'pipeline:view'),
(5, 'agents:view');

-- 插入普通用户角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(6, 'dashboard:view'),
(6, 'projects:view');

-- 插入默认管理员用户（密码：admin123）
INSERT IGNORE INTO users (id, username, password, email, nickname, role_id, status) VALUES
(1, 'admin', '$2a$12$REDACTED_BCRYPT_HASH_PLACEHOLDER_DATA', 'chengxun@88.com', '管理员', 1, 'APPROVED');

-- 插入默认通知模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_EMAIL_001', '告警邮件通知', 'EMAIL', 'ALERT',
'[${priorityDesc}] ${title}',
'<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background-color:#f8f9fa;padding:15px;border-radius:5px;margin-bottom:20px}.content{padding:15px;background-color:#fff;border:1px solid #ddd;border-radius:5px}.footer{margin-top:20px;padding-top:15px;border-top:1px solid #ddd;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2 style="margin:0">${title}</h2></div><div class="content"><p>${content}</p><p><strong>告警规则：</strong>${ruleName}</p><p><strong>触发值：</strong>${triggerValue}</p><p><strong>阈值：</strong>${thresholdValue}</p><p><strong>时间：</strong>${time}</p></div><div class="footer"><p>此邮件由 ${systemName} 自动发送</p></div></div></body></html>',
'系统告警邮件通知模板', TRUE, TRUE),

('ALERT_FEISHU_001', '告警飞书通知', 'FEISHU', 'ALERT',
'${title}',
'**${title}**

告警级别：${priorityDesc}
告警规则：${ruleName}
触发值：${triggerValue}
阈值：${thresholdValue}
时间：${time}

${content}',
'系统告警飞书通知模板', TRUE, TRUE),

('ALERT_SYSTEM_001', '告警站内信通知', 'SYSTEM', 'ALERT',
'${title}',
'${content}

告警规则：${ruleName}
触发值：${triggerValue} / ${thresholdValue}
时间：${time}',
'系统告警站内信通知模板', TRUE, TRUE),

('TASK_EMAIL_001', '任务完成邮件通知', 'EMAIL', 'TASK',
'[任务完成] ${taskTitle}',
'<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background-color:#27ae60;color:white;padding:15px;border-radius:5px;margin-bottom:20px}.content{padding:15px;background-color:#fff;border:1px solid #ddd;border-radius:5px}.footer{margin-top:20px;padding-top:15px;border-top:1px solid #ddd;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2 style="margin:0">任务完成通知</h2></div><div class="content"><p><strong>任务标题：</strong>${taskTitle}</p><p><strong>执行Agent：</strong>${agentName}</p><p><strong>完成时间：</strong>${time}</p><p><strong>任务结果：</strong>${taskResult}</p><hr><p>${content}</p></div><div class="footer"><p>此邮件由 ${systemName} 自动发送</p></div></div></body></html>',
'任务完成邮件通知模板', TRUE, TRUE),

('TASK_FEISHU_001', '任务完成飞书通知', 'FEISHU', 'TASK',
'任务完成：${taskTitle}',
'**任务完成通知**

任务标题：${taskTitle}
执行Agent：${agentName}
完成时间：${time}
任务结果：${taskResult}

${content}',
'任务完成飞书通知模板', TRUE, TRUE),

('AGENT_SYSTEM_001', 'Agent状态变更通知', 'SYSTEM', 'AGENT',
'Agent状态变更：${agentName}',
'Agent ${agentName} (${agentId}) 状态已变更。

${content}

时间：${time}',
'Agent状态变更站内信通知模板', TRUE, TRUE),

('SYSTEM_EMAIL_001', '系统通知邮件', 'EMAIL', 'SYSTEM',
'[系统通知] ${title}',
'<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background-color:#3498db;color:white;padding:15px;border-radius:5px;margin-bottom:20px}.content{padding:15px;background-color:#fff;border:1px solid #ddd;border-radius:5px}.footer{margin-top:20px;padding-top:15px;border-top:1px solid #ddd;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2 style="margin:0">${title}</h2></div><div class="content"><p>${content}</p><p style="color:#666;font-size:12px">时间：${time}</p></div><div class="footer"><p>此邮件由 ${systemName} 自动发送</p></div></div></body></html>',
'系统通用邮件通知模板', TRUE, TRUE),

('SYSTEM_SYSTEM_001', '系统站内信通知', 'SYSTEM', 'SYSTEM',
'${title}',
'${content}

时间：${time}',
'系统通用站内信通知模板', TRUE, TRUE);

-- 插入默认系统配置
INSERT IGNORE INTO system_configs (config_key, config_value, config_group, value_type, description) VALUES
('system.name', 'ChengXun Game Maker', 'system', 'string', '系统名称'),
('security.password.min-length', '8', 'security', 'number', '密码最小长度'),
('security.password.require-uppercase', 'true', 'security', 'boolean', '密码是否需要大写字母'),
('security.password.require-lowercase', 'true', 'security', 'boolean', '密码是否需要小写字母'),
('security.password.require-digit', 'true', 'security', 'boolean', '密码是否需要数字'),
('security.password.require-special', 'true', 'security', 'boolean', '密码是否需要特殊字符'),
('security.session.timeout-minutes', '30', 'security', 'number', '会话超时时间（分钟）'),
('security.session.max-concurrent', '1', 'security', 'number', '最大并发会话数'),
('security.login.max-attempts', '5', 'security', 'number', '最大登录尝试次数'),
('security.login.lockout-minutes', '15', 'security', 'number', '登录锁定时间（分钟）'),
('agent.task.max-retry', '3', 'agent', 'number', '任务最大重试次数'),
('agent.task.retry-delay-ms', '5000', 'agent', 'number', '任务重试延迟（毫秒）'),
('agent.task.max-queue-size', '100', 'agent', 'number', '任务队列最大长度'),
('agent.message.max-size', '10000', 'agent', 'number', '消息最大长度'),
('email.verification.expire-minutes', '10', 'email', 'number', '验证码过期时间（分钟）'),
('email.verification.code-length', '6', 'email', 'number', '验证码长度'),
('notification.enabled.channels', 'feishu,email', 'notification', 'string', '启用的通知渠道（逗号分隔）'),
('system.pagination.default-size', '20', 'system', 'number', '默认分页大小'),
('system.pagination.max-size', '100', 'system', 'number', '最大分页大小');
