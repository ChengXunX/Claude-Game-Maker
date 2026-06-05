-- V12: 创建 MCP 相关表

-- MCP Server 表
CREATE TABLE IF NOT EXISTS mcp_servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    transport_type VARCHAR(20) NOT NULL,
    command VARCHAR(500),
    args TEXT,
    env TEXT,
    url VARCHAR(500),
    headers TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    template BOOLEAN NOT NULL DEFAULT FALSE,
    template_key VARCHAR(50),
    project_id VARCHAR(100),
    tool_count INT NOT NULL DEFAULT 0,
    last_test_at TIMESTAMP NULL,
    last_test_result VARCHAR(500),
    connected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mcp_server_project (project_id),
    INDEX idx_mcp_server_enabled (enabled),
    INDEX idx_mcp_server_name (name),
    UNIQUE INDEX idx_mcp_server_template_key (template_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MCP Tool 表
CREATE TABLE IF NOT EXISTS mcp_tools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    input_schema TEXT,
    category VARCHAR(50),
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    call_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mcp_tool_server (server_id),
    INDEX idx_mcp_tool_name (tool_name),
    INDEX idx_mcp_tool_enabled (enabled),
    FOREIGN KEY (server_id) REFERENCES mcp_servers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent MCP 绑定表
CREATE TABLE IF NOT EXISTS agent_mcp_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_role VARCHAR(50) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    server_id BIGINT NOT NULL,
    tool_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_amb_agent (agent_role, project_id),
    INDEX idx_amb_server (server_id),
    INDEX idx_amb_tool (tool_id),
    FOREIGN KEY (server_id) REFERENCES mcp_servers(id) ON DELETE CASCADE,
    FOREIGN KEY (tool_id) REFERENCES mcp_tools(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
