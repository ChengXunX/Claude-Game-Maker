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
(1, 'PERM_context:monitor'),
(1, 'checkpoint:view'),
(1, 'checkpoint:manage'),
(1, 'goal:view'),
(1, 'goal:manage'),
(1, 'dream:execute'),
(1, 'subagent:view'),
(1, 'subagent:create'),
(1, 'subagent:manage'),
(1, 'skill:discover'),
(1, 'iteration:view'),
(1, 'iteration:manage'),
(1, 'supervision:view');

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
(2, 'skills:manage'),
(2, 'checkpoint:view'),
(2, 'goal:view'),
(2, 'goal:manage'),
(2, 'dream:execute'),
(2, 'subagent:view'),
(2, 'subagent:create');

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
(3, 'PERM_notification:preferences'),
(3, 'checkpoint:view'),
(3, 'goal:view'),
(3, 'subagent:view'),
(3, 'subagent:create');

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
('PERM_context:monitor', '上下文监控', '监控Agent上下文健康', 'Agent', 1, 1, 26),
('PERM_checkpoint:view', '检查点查看', '查看会话检查点', '检查点', 1, 1, 1),
('PERM_checkpoint:manage', '检查点管理', '创建、恢复、删除检查点', '检查点', 1, 1, 2),
('PERM_goal:view', '目标查看', '查看项目目标', '目标', 1, 1, 1),
('PERM_goal:manage', '目标管理', '设置、评估、完成目标', '目标', 1, 1, 2),
('PERM_dream:execute', '知识提取', '执行Dream知识提取', '知识', 1, 1, 1),
('PERM_subagent:view', '子代理查看', '查看子代理列表', '子代理', 1, 1, 1),
('PERM_subagent:create', '子代理创建', '创建子代理', '子代理', 1, 1, 2),
('PERM_subagent:manage', '子代理管理', '终止、清理子代理', '子代理', 1, 1, 3),
('PERM_skill:discover', 'Skill发现', '发现文件系统中的Skill', '技能', 1, 1, 3),
('PERM_iteration:view', '迭代查看', '查看版本迭代记录', '项目', 1, 1, 12),
('PERM_iteration:manage', '迭代管理', '管理版本迭代、回滚', '项目', 1, 1, 13),
('PERM_supervision:view', '督查查看', '查看督查报告', '项目', 1, 1, 14);

-- ===== MiMo V2: 6个新模块权限 =====

-- 新增权限定义
INSERT IGNORE INTO permission_definitions (permission_key, name, description, category, enabled, `system`, sort_order) VALUES
('PERM_distill:execute', '工作流发现', '执行Distill工作流发现', '知识', 1, 1, 2),
('PERM_snapshot:view', '快照查看', '查看文件快照', '快照', 1, 1, 1),
('PERM_snapshot:manage', '快照管理', '创建、恢复、删除快照', '快照', 1, 1, 2),
('PERM_fork:view', '分叉查看', '查看会话分叉', '会话', 1, 1, 1),
('PERM_fork:create', '分叉创建', '创建会话分叉', '会话', 1, 1, 2),
('PERM_fork:manage', '分叉管理', '合并、丢弃会话分叉', '会话', 1, 1, 3),
('PERM_tool:permission:manage', '工具权限管理', '管理Agent工具权限', 'Agent', 1, 1, 30);

-- ADMIN 角色新增权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(1, 'distill:execute'),
(1, 'snapshot:view'), (1, 'snapshot:manage'),
(1, 'fork:view'), (1, 'fork:create'), (1, 'fork:manage'),
(1, 'tool:permission:manage');

-- PROJECT_MANAGER 角色新增权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(2, 'distill:execute'),
(2, 'snapshot:view'), (2, 'snapshot:manage'),
(2, 'fork:view'), (2, 'fork:create');

-- DEVELOPER 角色新增权限
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
(3, 'snapshot:view'), (3, 'fork:view');

-- ===== LSP 代码理解能力 =====
-- 为代码密集型角色注册 LSP 代码理解能力（prompt 类型，AI 驱动）
-- 适用角色：server-dev, client-dev, ui-dev, tester, verifier, security-expert, performance-engineer, ai-engineer, tech-artist, devops

INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, priority, param_schema, execution_type, prompt_template, enabled) VALUES
-- server-dev
('server-dev', 'lspGoToDefinition', '跳转定义', '查找符号（类、方法、变量、接口）的定义位置，返回定义所在的文件路径、行号和上下文代码', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。\n\n分析步骤：\n1. 在项目目录中搜索该符号的定义（class、function、method、interface、type、const 等）\n2. 返回定义所在的文件路径、行号\n3. 展示定义的完整代码上下文（包含前后 5 行）\n4. 说明符号的类型、参数、返回值等信息\n\n如果找到多个同名定义，全部列出并说明区别。', 1),
('server-dev', 'lspFindReferences', '查找引用', '查找符号在项目中的所有引用位置，包括调用、赋值、导入等', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。\n\n分析步骤：\n1. 搜索项目中所有引用该符号的文件\n2. 对每个引用，说明引用类型（调用、赋值、导入、继承、实现等）\n3. 展示引用的代码上下文\n4. 统计引用总数和分布情况\n\n如果 includeDeclaration 为 true，同时包含定义位置。', 1),
('server-dev', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误、警告和改进建议，类似 IDE 的实时诊断', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。\n\n诊断维度：\n1. 语法错误：语法不正确、缺少分号/括号等\n2. 类型错误：类型不匹配、未定义的变量/方法\n3. 逻辑警告：可能的空指针、未处理的异常、死代码\n4. 代码规范：命名规范、代码风格、最佳实践\n5. 性能建议：潜在的性能问题、优化建议\n6. 安全风险：注入风险、敏感信息泄露等\n\n输出格式：\n- 严重程度：ERROR/WARNING/INFO\n- 位置：文件:行号\n- 描述：问题说明\n- 建议：修复方案', 1),
('server-dev', 'lspSymbolInfo', '符号信息', '获取符号的详细信息，包括类型签名、文档注释、所属模块等', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。\n\n分析内容：\n1. 类型签名：完整的类型声明（参数类型、返回类型）\n2. 文档注释：Javadoc/注释内容\n3. 所属模块：所在文件、包/命名空间、类\n4. 可见性：public/private/protected\n5. 使用示例：从项目中提取该符号的典型使用方式\n6. 相关符号：同类型的相关符号推荐\n\n如果 context 非空，在该上下文中分析符号的具体含义。', 1),
('server-dev', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号，支持模糊匹配，返回匹配的符号列表', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。\n\n搜索策略：\n1. 精确匹配：符号名完全匹配\n2. 前缀匹配：符号名以 query 开头\n3. 模糊匹配：符号名包含 query（不区分大小写）\n4. 如果 symbolKind 非 all，只搜索指定类型的符号\n\n输出格式（每条）：\n- 符号名称\n- 类型（class/method/function/variable/interface/enum）\n- 所在文件:行号\n- 简短描述（如有注释）\n\n按匹配度排序，最多返回 20 个结果。', 1),

-- client-dev
('client-dev', 'lspGoToDefinition', '跳转定义', '查找符号（类、方法、变量、接口）的定义位置，返回定义所在的文件路径、行号和上下文代码', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。\n\n分析步骤：\n1. 在项目目录中搜索该符号的定义\n2. 返回定义所在的文件路径、行号\n3. 展示定义的完整代码上下文\n4. 说明符号的类型、参数、返回值等信息', 1),
('client-dev', 'lspFindReferences', '查找引用', '查找符号在项目中的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。\n\n分析步骤：\n1. 搜索项目中所有引用该符号的文件\n2. 对每个引用，说明引用类型\n3. 展示引用的代码上下文\n4. 统计引用总数和分布情况', 1),
('client-dev', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误、警告和改进建议', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。\n\n诊断维度：语法错误、类型错误、逻辑警告、代码规范、性能建议、安全风险。', 1),
('client-dev', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。包括类型签名、文档注释、所属模块、可见性、使用示例。', 1),
('client-dev', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。按匹配度排序，最多返回 20 个结果。', 1),

-- ui-dev
('ui-dev', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('ui-dev', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('ui-dev', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('ui-dev', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('ui-dev', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- tester
('tester', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('tester', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('tester', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('tester', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('tester', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- verifier
('verifier', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('verifier', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('verifier', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('verifier', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('verifier', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- security-expert
('security-expert', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('security-expert', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('security-expert', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('security-expert', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('security-expert', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- performance-engineer
('performance-engineer', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('performance-engineer', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('performance-engineer', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('performance-engineer', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('performance-engineer', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- ai-engineer
('ai-engineer', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('ai-engineer', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('ai-engineer', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('ai-engineer', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('ai-engineer', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- tech-artist
('tech-artist', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('tech-artist', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('tech-artist', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('tech-artist', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('tech-artist', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1),

-- devops
('devops', 'lspGoToDefinition', '跳转定义', '查找符号的定义位置', 'code_intelligence', 0, 80, '{"symbol":"string|required","scope":"string"}', 'prompt', '请在项目中查找符号 "{symbol}" 的定义位置。', 1),
('devops', 'lspFindReferences', '查找引用', '查找符号的所有引用位置', 'code_intelligence', 0, 81, '{"symbol":"string|required","includeDeclaration":"boolean"}', 'prompt', '请在项目中查找符号 "{symbol}" 的所有引用位置。', 1),
('devops', 'lspCodeDiagnostics', '代码诊断', '分析代码文件中的错误和警告', 'code_intelligence', 0, 82, '{"targetPath":"string|required","severity":"enum:error,warning,info,all"}', 'prompt', '请对文件 "{targetPath}" 进行代码诊断分析。', 1),
('devops', 'lspSymbolInfo', '符号信息', '获取符号的详细信息', 'code_intelligence', 0, 83, '{"symbol":"string|required","context":"string"}', 'prompt', '请获取符号 "{symbol}" 的详细信息。', 1),
('devops', 'lspWorkspaceSymbols', '符号搜索', '在项目中搜索符号', 'code_intelligence', 0, 84, '{"query":"string|required","symbolKind":"enum:class,method,function,variable,interface,enum,all"}', 'prompt', '请在项目中搜索匹配 "{query}" 的符号。', 1);

-- ===== Agent 工具能力：快照、会话分叉、子代理、工具权限 =====
-- 快照管理（所有开发类角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, priority, param_schema, execution_type, prompt_template, enabled) VALUES
('producer', 'createSnapshot', '创建快照', '对指定文件创建快照，保存当前状态以便后续恢复', 'snapshot', 0, 90, '{"projectId":"string|required","agentId":"string|required","filePaths":"array|required","description":"string"}', 'java', NULL, 1),
('producer', 'listSnapshots', '查看快照', '查看指定项目和 Agent 的所有快照列表', 'snapshot', 0, 91, '{"projectId":"string|required","agentId":"string|required"}', 'java', NULL, 1),
('producer', 'restoreSnapshot', '恢复快照', '恢复指定快照，将文件还原到快照时的状态', 'snapshot', 0, 92, '{"projectId":"string|required","agentId":"string|required","snapshotId":"string|required"}', 'java', NULL, 1),
('producer', 'undoSnapshot', '撤销快照恢复', '撤销最近一次快照恢复操作', 'snapshot', 0, 93, '{"projectId":"string|required","agentId":"string|required"}', 'java', NULL, 1),
('server-dev', 'createSnapshot', '创建快照', '对指定文件创建快照', 'snapshot', 0, 90, '{"projectId":"string|required","agentId":"string|required","filePaths":"array|required","description":"string"}', 'java', NULL, 1),
('server-dev', 'listSnapshots', '查看快照', '查看快照列表', 'snapshot', 0, 91, '{"projectId":"string|required","agentId":"string|required"}', 'java', NULL, 1),
('server-dev', 'restoreSnapshot', '恢复快照', '恢复指定快照', 'snapshot', 0, 92, '{"projectId":"string|required","agentId":"string|required","snapshotId":"string|required"}', 'java', NULL, 1),
('server-dev', 'undoSnapshot', '撤销快照恢复', '撤销最近一次快照恢复', 'snapshot', 0, 93, '{"projectId":"string|required","agentId":"string|required"}', 'java', NULL, 1);

-- 会话分叉（制作人和高级角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, priority, param_schema, execution_type, prompt_template, enabled) VALUES
('producer', 'createSessionFork', '创建会话分叉', '从当前会话分叉出一个探索性分支', 'session', 0, 95, '{"projectId":"string|required","agentId":"string|required","description":"string"}', 'java', NULL, 1),
('producer', 'listSessionForks', '查看会话分叉', '查看所有会话分叉', 'session', 0, 96, '{"parentAgentId":"string|required"}', 'java', NULL, 1),
('producer', 'mergeSessionFork', '合并会话分叉', '将分叉的上下文合并回主会话', 'session', 0, 97, '{"forkId":"string|required","strategy":"string"}', 'java', NULL, 1),
('producer', 'discardSessionFork', '丢弃会话分叉', '丢弃不需要的会话分叉', 'session', 0, 98, '{"forkId":"string|required"}', 'java', NULL, 1),
('server-dev', 'createSessionFork', '创建会话分叉', '从当前会话分叉出探索分支', 'session', 0, 95, '{"projectId":"string|required","agentId":"string|required","description":"string"}', 'java', NULL, 1),
('server-dev', 'listSessionForks', '查看会话分叉', '查看所有会话分叉', 'session', 0, 96, '{"parentAgentId":"string|required"}', 'java', NULL, 1),
('server-dev', 'mergeSessionFork', '合并会话分叉', '合并分叉回主会话', 'session', 0, 97, '{"forkId":"string|required","strategy":"string"}', 'java', NULL, 1),
('server-dev', 'discardSessionFork', '丢弃会话分叉', '丢弃不需要的分叉', 'session', 0, 98, '{"forkId":"string|required"}', 'java', NULL, 1);

-- 子代理（制作人和高级角色）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, priority, param_schema, execution_type, prompt_template, enabled) VALUES
('producer', 'spawnSubAgent', '创建子代理', '创建子代理来并行处理子任务', 'subagent', 0, 100, '{"parentAgentId":"string|required","projectId":"string|required","task":"string|required","role":"string"}', 'java', NULL, 1),
('producer', 'listSubAgents', '查看子代理', '查看所有子代理', 'subagent', 0, 101, '{"parentAgentId":"string|required"}', 'java', NULL, 1),
('producer', 'terminateSubAgent', '终止子代理', '终止运行中的子代理', 'subagent', 0, 102, '{"subAgentId":"string|required"}', 'java', NULL, 1),
('server-dev', 'spawnSubAgent', '创建子代理', '创建子代理并行处理子任务', 'subagent', 0, 100, '{"parentAgentId":"string|required","projectId":"string|required","task":"string|required","role":"string"}', 'java', NULL, 1),
('server-dev', 'listSubAgents', '查看子代理', '查看所有子代理', 'subagent', 0, 101, '{"parentAgentId":"string|required"}', 'java', NULL, 1),
('server-dev', 'terminateSubAgent', '终止子代理', '终止运行中的子代理', 'subagent', 0, 102, '{"subAgentId":"string|required"}', 'java', NULL, 1);

-- 工具权限（仅制作人）
INSERT IGNORE INTO agent_capabilities (agent_role, capability_name, display_name, description, category, requires_approval, priority, param_schema, execution_type, prompt_template, enabled) VALUES
('producer', 'setToolPermissions', '设置工具权限', '设置 Agent 的工具调用权限规则', 'security', 0, 105, '{"agentId":"string|required","permissions":"array|required"}', 'java', NULL, 1);
