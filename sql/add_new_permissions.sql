-- 添加新页面相关权限定义
-- 确保所有新功能的权限在数据库中存在

-- 能力管理相关权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_capabilities:view', '能力查看', '查看Agent能力定义', 'Agent', true, 10);

INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_capabilities:manage', '能力管理', '创建、编辑、删除Agent能力', 'Agent', true, 11);

-- MCP管理相关权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_mcp:view', 'MCP查看', '查看MCP服务器', 'Agent', true, 12);

INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_mcp:manage', 'MCP管理', '管理MCP服务器', 'Agent', true, 13);

-- 文件管理相关权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_files:view', '文件查看', '查看Agent文件', 'Agent', true, 14);

INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_files:manage', '文件管理', '上传、删除Agent文件', 'Agent', true, 15);

-- 系统常量相关权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_constants:view', '常量查看', '查看系统常量', '系统', true, 20);

INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_constants:manage', '常量管理', '编辑系统常量', '系统', true, 21);

-- 权限管理相关权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_permissions:view', '权限查看', '查看权限列表', '管理', true, 30);

INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_permissions:manage', '权限管理', '管理权限定义和审批', '管理', true, 31);

-- API文档权限（所有登录用户可见）
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_api:view', 'API文档', '查看API文档', '系统', true, 22);

-- 通知偏好权限（所有登录用户可见）
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_notification:preferences', '通知偏好', '配置通知接收偏好', '通知', true, 40);

-- 上下文监控权限
INSERT IGNORE INTO permission_definitions (permission, name, description, category, enabled, display_order)
VALUES ('PERM_context:monitor', '上下文监控', '监控Agent上下文健康', 'Agent', true, 16);

-- 为ADMIN角色添加所有新权限
INSERT IGNORE INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN permission_definitions p
WHERE r.name = 'ADMIN'
AND p.permission IN (
    'PERM_capabilities:view',
    'PERM_capabilities:manage',
    'PERM_mcp:view',
    'PERM_mcp:manage',
    'PERM_files:view',
    'PERM_files:manage',
    'PERM_constants:view',
    'PERM_constants:manage',
    'PERM_permissions:view',
    'PERM_permissions:manage',
    'PERM_api:view',
    'PERM_notification:preferences',
    'PERM_context:monitor'
);

-- 为PROJECT_MANAGER角色添加查看权限
INSERT IGNORE INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN permission_definitions p
WHERE r.name = 'PROJECT_MANAGER'
AND p.permission IN (
    'PERM_capabilities:view',
    'PERM_mcp:view',
    'PERM_files:view',
    'PERM_constants:view',
    'PERM_api:view',
    'PERM_notification:preferences',
    'PERM_context:monitor'
);

-- 为DEVELOPER角色添加基本查看权限
INSERT IGNORE INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN permission_definitions p
WHERE r.name = 'DEVELOPER'
AND p.permission IN (
    'PERM_capabilities:view',
    'PERM_files:view',
    'PERM_api:view',
    'PERM_notification:preferences'
);
