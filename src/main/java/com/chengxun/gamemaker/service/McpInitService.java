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
        // 检查是否有新增模板需要添加
        List<String> allKeys = List.of(
            "filesystem", "github", "gitlab", "postgres", "mysql", "feishu-doc", "feishu-bitable",
            "slack", "brave-search", "puppeteer", "fetch", "memory", "sequential-thinking", "sentry",
            "unity", "godot", "unreal", "playfab", "firebase", "steam",
            "redis", "mongodb", "jira", "linear", "notion", "grafana", "sonarqube"
        );
        long existingCount = serverRepository.count();

        // 检查最后一个新增模板是否存在，如果存在说明已经全部初始化
        boolean hasLatest = serverRepository.findByTemplateKey("sonarqube").isPresent();
        if (hasLatest) {
            log.info("MCP templates already up to date ({} servers)", existingCount);
            return;
        }

        log.info("Adding new MCP templates...");

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

        // ===== 游戏开发专用 =====

        // Unity MCP
        saveTemplate("unity", "Unity MCP", "Unity 编辑器自动化：场景管理、资产导入、构建、Play 模式控制",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/unity-mcp-server\"]",
            "{\"UNITY_PROJECT_PATH\":\"${UNITY_PROJECT_PATH}\"}", null, "gamedev", true);

        // Godot MCP
        saveTemplate("godot", "Godot MCP", "Godot 引擎自动化：场景树操作、节点管理、脚本编辑、运行调试",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/godot-mcp-server\"]",
            "{\"GODOT_PROJECT_PATH\":\"${GODOT_PROJECT_PATH}\"}", null, "gamedev", true);

        // Unreal Engine MCP
        saveTemplate("unreal", "Unreal MCP", "Unreal Engine 自动化：蓝图操作、资产管理、关卡编辑、构建打包",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/unreal-mcp-server\"]",
            "{\"UE_PROJECT_PATH\":\"${UE_PROJECT_PATH}\"}", null, "gamedev", true);

        // ===== 游戏后端服务 =====

        // PlayFab MCP
        saveTemplate("playfab", "PlayFab", "PlayFab 游戏后端：玩家数据、排行榜、匹配系统、虚拟货币、成就",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/playfab-mcp-server\"]",
            "{\"PLAYFAB_TITLE_ID\":\"${PLAYFAB_TITLE_ID}\",\"PLAYFAB_SECRET_KEY\":\"${PLAYFAB_SECRET_KEY}\"}", null, "gamedev", true);

        // Firebase MCP
        saveTemplate("firebase", "Firebase", "Firebase 服务：实时数据库、认证、云函数、FCM 推送、崩溃报告",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/firebase-mcp-server\"]",
            "{\"FIREBASE_PROJECT_ID\":\"${FIREBASE_PROJECT_ID}\",\"FIREBASE_SERVICE_ACCOUNT\":\"${FIREBASE_SERVICE_ACCOUNT}\"}", null, "gamedev", true);

        // Steam MCP
        saveTemplate("steam", "Steam MCP", "Steam 平台集成：成就管理、排行榜、DLC、商店页面、Steamworks API",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/steam-mcp-server\"]",
            "{\"STEAM_APP_ID\":\"${STEAM_APP_ID}\",\"STEAM_API_KEY\":\"${STEAM_API_KEY}\"}", null, "gamedev", true);

        // ===== 数据库 =====

        // Redis MCP
        saveTemplate("redis", "Redis", "Redis 缓存操作：键值读写、发布订阅、过期管理、缓存预热",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/redis-mcp-server\"]",
            "{\"REDIS_URL\":\"${REDIS_URL}\"}", null, "data", true);

        // MongoDB MCP
        saveTemplate("mongodb", "MongoDB", "MongoDB 数据库操作：文档查询、聚合管道、索引管理",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/mongodb-mcp-server\"]",
            "{\"MONGODB_URI\":\"${MONGODB_URI}\"}", null, "data", true);

        // ===== 项目管理 & 协作 =====

        // Jira MCP
        saveTemplate("jira", "Jira", "Jira 项目管理：Issue 创建/更新、Sprint 管理、看板操作、状态流转",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/jira-mcp-server\"]",
            "{\"JIRA_URL\":\"${JIRA_URL}\",\"JIRA_EMAIL\":\"${JIRA_EMAIL}\",\"JIRA_API_TOKEN\":\"${JIRA_API_TOKEN}\"}", null, "collaboration", true);

        // Linear MCP
        saveTemplate("linear", "Linear", "Linear 项目管理：Issue 追踪、Cycle 管理、团队工作流、路线图",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/linear-mcp-server\"]",
            "{\"LINEAR_API_KEY\":\"${LINEAR_API_KEY}\"}", null, "collaboration", true);

        // Notion MCP
        saveTemplate("notion", "Notion", "Notion 文档协作：页面读写、数据库查询、评论管理、搜索",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/notion-mcp-server\"]",
            "{\"NOTION_API_KEY\":\"${NOTION_API_KEY}\"}", null, "collaboration", true);

        // ===== 监控 & 质量 =====

        // Grafana MCP
        saveTemplate("grafana", "Grafana", "Grafana 监控面板：查询指标、仪表板管理、告警规则、数据源",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/grafana-mcp-server\"]",
            "{\"GRAFANA_URL\":\"${GRAFANA_URL}\",\"GRAFANA_API_KEY\":\"${GRAFANA_API_KEY}\"}", null, "monitoring", true);

        // SonarQube MCP
        saveTemplate("sonarqube", "SonarQube", "SonarQube 代码质量：扫描结果、质量门禁、Bug/漏洞/异味统计",
            McpServer.TransportType.STDIO,
            "npx", "[\"-y\", \"@anthropic/sonarqube-mcp-server\"]",
            "{\"SONAR_URL\":\"${SONAR_URL}\",\"SONAR_TOKEN\":\"${SONAR_TOKEN}\"}", null, "monitoring", true);

        log.info("MCP templates initialized: 27 servers");
    }

    private void saveTemplate(String templateKey, String name, String description,
                               McpServer.TransportType transportType,
                               String command, String args, String env, String url,
                               String category, boolean enabled) {
        // 跳过已存在的模板
        if (serverRepository.findByTemplateKey(templateKey).isPresent()) {
            return;
        }

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
