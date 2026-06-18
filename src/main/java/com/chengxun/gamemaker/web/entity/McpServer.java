package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MCP Server 实体
 * 管理 MCP (Model Context Protocol) 服务器配置
 *
 * 支持三种传输方式：
 * - stdio: 本地进程（通过命令行启动）
 * - sse: Server-Sent Events（HTTP 长连接）
 * - streamable_http: Streamable HTTP（标准 HTTP 请求）
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "mcp_servers", indexes = {
    @Index(name = "idx_mcp_server_project", columnList = "projectId"),
    @Index(name = "idx_mcp_server_enabled", columnList = "enabled"),
    @Index(name = "idx_mcp_server_name", columnList = "name")
})
public class McpServer {

    /** 传输方式 */
    public enum TransportType {
        /** 本地进程 */
        STDIO,
        /** Server-Sent Events */
        SSE,
        /** Streamable HTTP */
        STREAMABLE_HTTP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 服务器名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 服务器描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 传输方式 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransportType transportType;

    /** 命令（stdio 模式）：如 npx, node, python */
    @Column(length = 500)
    private String command;

    /** 命令参数（JSON 数组格式）：如 ["-y", "@modelcontextprotocol/server-github"] */
    @Column(columnDefinition = "TEXT")
    private String args;

    /** 环境变量（JSON 格式）：如 {"GITHUB_TOKEN": "xxx"} */
    @Column(columnDefinition = "TEXT")
    private String env;

    /** URL（SSE/Streamable HTTP 模式） */
    @Column(length = 500)
    private String url;

    /** 请求头（JSON 格式，SSE/HTTP 模式）：如 {"Authorization": "Bearer xxx"} */
    @Column(columnDefinition = "TEXT")
    private String headers;

    /** 模板分类：resource-image/resource-audio/resource-video/resource-3d/dev/data/collaboration/monitoring/gamedev */
    @Column(length = 50)
    private String category;

    /** 认证模式：header（请求头）/ body（请求体）/ env（环境变量） */
    @Column(length = 20)
    private String authMode = "env";

    /** 自定义认证头名（authMode=header 时生效），默认 Authorization */
    @Column(length = 100)
    private String authHeaderName = "Authorization";

    /** 必填参数定义（JSON 数组格式）：[{"key":"API_KEY","label":"API Key","placeholder":"sk-xxx"}] */
    @Column(columnDefinition = "TEXT")
    private String requiredParams;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 是否为模板（预设的 MCP Server） */
    @Column(nullable = false)
    private boolean template = false;

    /** 模板标识（如 github, filesystem, feishu） */
    @Column(length = 50)
    private String templateKey;

    /** 所属项目 ID（null 表示全局） */
    @Column(length = 100)
    private String projectId;

    /** 已发现的工具数量 */
    @Column(nullable = false)
    private int toolCount = 0;

    /** 最后一次连接测试时间 */
    private LocalDateTime lastTestAt;

    /** 最后一次测试结果 */
    @Column(length = 500)
    private String lastTestResult;

    /** 连接状态 */
    @Column(nullable = false)
    private boolean connected = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TransportType getTransportType() { return transportType; }
    public void setTransportType(TransportType transportType) { this.transportType = transportType; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getArgs() { return args; }
    public void setArgs(String args) { this.args = args; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }

    public String getAuthHeaderName() { return authHeaderName; }
    public void setAuthHeaderName(String authHeaderName) { this.authHeaderName = authHeaderName; }

    public String getRequiredParams() { return requiredParams; }
    public void setRequiredParams(String requiredParams) { this.requiredParams = requiredParams; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTemplate() { return template; }
    public void setTemplate(boolean template) { this.template = template; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getToolCount() { return toolCount; }
    public void setToolCount(int toolCount) { this.toolCount = toolCount; }

    public LocalDateTime getLastTestAt() { return lastTestAt; }
    public void setLastTestAt(LocalDateTime lastTestAt) { this.lastTestAt = lastTestAt; }

    public String getLastTestResult() { return lastTestResult; }
    public void setLastTestResult(String lastTestResult) { this.lastTestResult = lastTestResult; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("McpServer[%s] type=%s, tools=%d, enabled=%b",
            name, transportType, toolCount, enabled);
    }
}
