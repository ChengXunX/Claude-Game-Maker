-- MCP Server 模板分类与认证模式
-- 支持不同资源生成提供商的灵活配置

ALTER TABLE mcp_servers ADD COLUMN category VARCHAR(50) COMMENT '模板分类：resource-image/resource-audio/resource-video/resource-3d/dev/data/collaboration/monitoring/gamedev';
ALTER TABLE mcp_servers ADD COLUMN auth_mode VARCHAR(20) DEFAULT 'env' COMMENT '认证模式：header/body/env';
ALTER TABLE mcp_servers ADD COLUMN auth_header_name VARCHAR(100) DEFAULT 'Authorization' COMMENT '自定义认证头名';
ALTER TABLE mcp_servers ADD COLUMN required_params TEXT COMMENT '必填参数定义（JSON 数组）';
