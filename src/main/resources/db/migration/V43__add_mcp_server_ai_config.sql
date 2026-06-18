-- MCP Server 独立 AI 配置字段
-- 用于 MCP Server 自身的 AI 服务凭据（如图片生成 API），独立于 Agent Token 配置

ALTER TABLE mcp_servers ADD COLUMN ai_api_key VARCHAR(500) COMMENT 'AI 服务 API Key';
ALTER TABLE mcp_servers ADD COLUMN ai_api_url VARCHAR(500) COMMENT 'AI 服务 API URL';
ALTER TABLE mcp_servers ADD COLUMN ai_model VARCHAR(100) COMMENT 'AI 服务模型名';
