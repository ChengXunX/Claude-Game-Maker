package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.repository.McpServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP 模板初始化服务
 * 预置常用 MCP Server 模板，用户可一键安装
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class McpInitService {

    private static final Logger log = LoggerFactory.getLogger(McpInitService.class);

    private final McpServerRepository serverRepository;

    public McpInitService(McpServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initTemplates() {
        long count = serverRepository.count();
        if (count > 0) {
            log.info("MCP templates already initialized ({} servers)", count);
            return;
        }

        log.info("Initializing MCP server templates...");

        // 文件系统
        saveTemplate("filesystem", "文件系统", "读写本地文件系统",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-filesystem\", \"${WORK_DIR}\"]",
            null, null, "file", true);

        // GitHub
        saveTemplate("github", "GitHub", "GitHub 仓库操作：Issue、PR、代码搜索等",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-github\"]",
            "{\"GITHUB_TOKEN\":\"${GITHUB_TOKEN}\"}", null, "dev", true);

        // GitLab
        saveTemplate("gitlab", "GitLab", "GitLab 仓库操作",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-gitlab\"]",
            "{\"GITLAB_TOKEN\":\"${GITLAB_TOKEN}\"}", null, "dev", true);

        // PostgreSQL 数据库
        saveTemplate("postgres", "PostgreSQL", "PostgreSQL 数据库查询和操作",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-postgres\"]",
            "{\"DATABASE_URL\":\"${DATABASE_URL}\"}", null, "data", true);

        // MySQL 数据库
        saveTemplate("mysql", "MySQL", "MySQL 数据库查询和操作",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"mysql-mcp-server\"]",
            "{\"MYSQL_HOST\":\"${MYSQL_HOST}\",\"MYSQL_PORT\":\"${MYSQL_PORT}\",\"MYSQL_USER\":\"${MYSQL_USER}\",\"MYSQL_PASSWORD\":\"${MYSQL_PASSWORD}\",\"MYSQL_DATABASE\":\"${MYSQL_DATABASE}\"}",
            null, "data", true);

        // 飞书文档
        saveTemplate("feishu-doc", "飞书文档", "飞书文档读写、搜索、评论",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/feishu-mcp-server\"]",
            "{\"FEISHU_APP_ID\":\"${FEISHU_APP_ID}\",\"FEISHU_APP_SECRET\":\"${FEISHU_APP_SECRET}\"}",
            null, "collaboration", true);

        // 飞书多维表格
        saveTemplate("feishu-bitable", "飞书多维表格", "飞书多维表格数据读写",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/feishu-bitable-mcp\"]",
            "{\"FEISHU_APP_ID\":\"${FEISHU_APP_ID}\",\"FEISHU_APP_SECRET\":\"${FEISHU_APP_SECRET}\"}",
            null, "collaboration", true);

        // Slack
        saveTemplate("slack", "Slack", "Slack 消息发送和频道管理",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-slack\"]",
            "{\"SLACK_BOT_TOKEN\":\"${SLACK_BOT_TOKEN}\"}", null, "collaboration", true);

        // Brave Search
        saveTemplate("brave-search", "Brave 搜索", "网络搜索",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-brave-search\"]",
            "{\"BRAVE_API_KEY\":\"${BRAVE_API_KEY}\"}", null, "search", true);

        // Puppeteer（浏览器自动化）
        saveTemplate("puppeteer", "Puppeteer", "浏览器自动化：截图、爬虫、测试",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-puppeteer\"]",
            null, null, "automation", true);

        // Fetch（HTTP 请求）
        saveTemplate("fetch", "HTTP Fetch", "发送 HTTP 请求",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-fetch\"]",
            null, null, "network", true);

        // Memory（知识图谱）
        saveTemplate("memory", "Memory", "基于知识图谱的持久化记忆",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-memory\"]",
            null, null, "memory", true);

        // Sequential Thinking
        saveTemplate("sequential-thinking", "Sequential Thinking", "结构化思维链推理",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-sequential-thinking\"]",
            null, null, "reasoning", true);

        // Sentry
        saveTemplate("sentry", "Sentry", "错误监控和异常追踪",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@modelcontextprotocol/server-sentry\"]",
            "{\"SENTRY_AUTH_TOKEN\":\"${SENTRY_AUTH_TOKEN}\"}", null, "monitoring", true);

        log.info("MCP templates initialized: 14 servers");
    }

    private void saveTemplate(String templateKey, String name, String description,
                               McpServer.TransportType transportType,
                               String command, String args, String env, String url,
                               String category, boolean enabled) {
        McpServer server = new McpServer();
        server.setName(name);
        server.setDescription(description);
        server.setTransportType(transportType);
        server.setCommand(command);
        server.setArgs(args);
        server.setEnv(env);
        server.setUrl(url);
        server.setTemplate(true);
        server.setTemplateKey(templateKey);
        server.setEnabled(enabled);
        server.setConnected(false);
        serverRepository.save(server);
    }
}
