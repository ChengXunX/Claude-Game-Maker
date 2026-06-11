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
(6, 'USER', '普通用户', '普通用户，基础权限', TRUE),
(7, 'READONLY', '只读访客', '只读权限，可查看所有模块但不能修改，供外部人员了解系统特性', TRUE);

-- 插入管理员角色权限（完整60个权限）
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
(1, 'agents:view'),
(1, 'pipeline:create'),
(1, 'projects:edit'),
(1, 'data:view'),
(1, 'version:manage'),
(1, 'agent:config'),
(1, 'agent:optimize'),
(1, 'users:view'),
(1, 'notifications:manage'),
(1, 'PERM_capabilities:view'),
(1, 'PERM_capabilities:manage'),
(1, 'PERM_mcp:view'),
(1, 'PERM_mcp:manage'),
(1, 'PERM_files:view'),
(1, 'PERM_files:manage'),
(1, 'PERM_constants:view'),
(1, 'PERM_constants:manage'),
(1, 'PERM_permissions:view'),
(1, 'PERM_permissions:manage'),
(1, 'PERM_api:view'),
(1, 'PERM_notification:preferences'),
(1, 'PERM_context:monitor');

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
(2, 'agents:view'),
(2, 'agent:config'),
(2, 'agent:optimize'),
(2, 'version:manage'),
(2, 'approval:view'),
(2, 'notification:view'),
(2, 'tokens:view'),
(2, 'system:view'),
(2, 'logs:view'),
(2, 'PERM_capabilities:view'),
(2, 'PERM_mcp:view'),
(2, 'PERM_files:view'),
(2, 'PERM_constants:view'),
(2, 'PERM_api:view'),
(2, 'PERM_notification:preferences'),
(2, 'PERM_context:monitor'),
(2, 'skills:manage');

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
(3, 'agents:view'),
(3, 'agent:view'),
(3, 'agent:config'),
(3, 'notification:view'),
(3, 'tokens:view'),
(3, 'approval:view'),
(3, 'PERM_capabilities:view'),
(3, 'PERM_files:view'),
(3, 'PERM_api:view'),
(3, 'PERM_notification:preferences');

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
(4, 'agents:view'),
(4, 'system:view'),
(4, 'system:config'),
(4, 'logs:view'),
(4, 'notification:view'),
(4, 'tokens:view'),
(4, 'terminal:use');

-- 插入观察者角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(5, 'system:monitor'),
(5, 'dashboard:view'),
(5, 'projects:view'),
(5, 'skills:view'),
(5, 'ai:use'),
(5, 'pipeline:view'),
(5, 'agents:view'),
(5, 'system:view'),
(5, 'logs:view'),
(5, 'notification:view'),
(5, 'tokens:view'),
(5, 'approval:view'),
(5, 'workflow:view'),
(5, 'code:review');

-- 插入普通用户角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(6, 'dashboard:view'),
(6, 'projects:view'),
(6, 'notification:view'),
(6, 'skills:view');

-- 插入只读访客角色权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(7, 'dashboard:view'),
(7, 'agents:view'),
(7, 'ai:use'),
(7, 'projects:view'),
(7, 'skills:view'),
(7, 'tokens:view'),
(7, 'notification:view'),
(7, 'code:review'),
(7, 'pipeline:view'),
(7, 'workflow:view'),
(7, 'approval:view'),
(7, 'users:view'),
(7, 'logs:view'),
(7, 'system:view'),
(7, 'system:monitor');

-- 管理员用户由 Flyway V1 迁移脚本创建，此处不再重复插入（避免覆盖用户自定义密码）

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
('security.device.trust.enabled', 'false', 'security', 'boolean', '是否启用设备信任（陌生设备二次验证）'),
('security.device.trust.days', '7', 'security', 'number', '设备信任有效期（天）'),
('agent.task.max-retry', '3', 'agent', 'number', '任务最大重试次数'),
('agent.task.retry-delay-ms', '5000', 'agent', 'number', '任务重试延迟（毫秒）'),
('agent.task.max-queue-size', '100', 'agent', 'number', '任务队列最大长度'),
('agent.message.max-size', '10000', 'agent', 'number', '消息最大长度'),
('email.verification.expire-minutes', '10', 'email', 'number', '验证码过期时间（分钟）'),
('email.verification.code-length', '6', 'email', 'number', '验证码长度'),
('notification.enabled.channels', 'feishu,email', 'notification', 'string', '启用的通知渠道（逗号分隔）'),
('system.pagination.default-size', '20', 'system', 'number', '默认分页大小'),
('system.pagination.max-size', '100', 'system', 'number', '最大分页大小');

-- 插入权限定义（使用 PERM_ 前缀格式，与数据库一致）
INSERT IGNORE INTO permission_definitions (permission_key, name, description, category, enabled, `system`, sort_order) VALUES
('PERM_dashboard:view', '仪表盘查看', '查看仪表盘和系统概览', '工作台', 1, 1, 1),
('PERM_agents:view', 'Agent查看', '查看Agent列表、状态、详情', 'Agent', 1, 1, 1),
('PERM_agents:manage', 'Agent管理', '启动、停止、重启Agent，修改Agent配置', 'Agent', 1, 1, 2),
('PERM_agents:task', 'Agent任务', '向Agent发送任务和指令', 'Agent', 1, 1, 3),
('PERM_ai:use', 'AI助手使用', '使用AI助手进行对话', 'AI', 1, 1, 1),
('PERM_ai:admin', 'AI助手管理', '管理AI配置、知识库、技能生成', 'AI', 1, 1, 2),
('PERM_projects:view', '项目查看', '查看项目列表和详情', '项目', 1, 1, 1),
('PERM_projects:manage', '项目管理', '创建、编辑、删除项目，管理项目配置', '项目', 1, 1, 2),
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
('PERM_pipeline:execute', '流水线执行', '触发流水线执行', '流水线', 1, 1, 3),
('PERM_pipeline:approve', '流水线审批', '审批流水线执行请求', '流水线', 1, 1, 4),
('PERM_pipeline:intervene', '流水线干预', '干预正在执行的流水线', '流水线', 1, 1, 5),
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
('PERM_agent:view', 'Agent查看(兼容)', '查看Agent列表（兼容别名）', 'Agent', 1, 1, 4),
('PERM_agent:manage', 'Agent管理(兼容)', '管理Agent（兼容别名）', 'Agent', 1, 1, 5),
('PERM_pipeline:create', '流水线创建', '创建新的流水线', '流水线', 1, 1, 3),
('PERM_projects:edit', '项目编辑', '编辑项目配置和设置', '项目', 1, 1, 3),
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
('PERM_context:monitor', '上下文监控', '监控Agent上下文健康', 'Agent', 1, 1, 26);
