package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.entity.McpTool;
import com.chengxun.gamemaker.web.repository.McpServerRepository;
import com.chengxun.gamemaker.web.repository.McpToolRepository;
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
 * 模板分类：
 * - dev: 开发工具
 * - data: 数据库
 * - collaboration: 协作工具
 * - monitoring: 监控
 * - gamedev: 游戏开发
 * - resource-image: 图片生成
 * - resource-audio: 音频生成
 * - resource-video: 视频生成
 * - resource-3d: 3D 生成
 *
 * 认证模式：
 * - env: API Key 放环境变量（STDIO 模式默认）
 * - header: API Key 放请求头（HTTP 模式默认）
 * - body: API Key 放请求体
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class McpInitService {

    private static final Logger log = LoggerFactory.getLogger(McpInitService.class);

    private final McpServerRepository serverRepository;
    private final McpToolRepository toolRepository;

    public McpInitService(McpServerRepository serverRepository, McpToolRepository toolRepository) {
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initTemplates() {
        // 检查最新模板是否存在，避免重复初始化
        boolean hasLatest = serverRepository.findByTemplateKey("minimax-image").isPresent();
        if (hasLatest) {
            log.info("MCP templates already up to date ({})", serverRepository.count());
            return;
        }

        log.info("Initializing MCP server templates...");

        // ===== 开发工具 =====
        saveStdioTemplate("filesystem", "文件系统", "读写本地文件系统", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-filesystem\", \"${WORK_DIR}\"]", null);

        saveStdioTemplate("github", "GitHub", "GitHub 仓库操作：Issue、PR、代码搜索等", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-github\"]",
            "{\"GITHUB_TOKEN\":\"${GITHUB_TOKEN}\"}");

        saveStdioTemplate("gitlab", "GitLab", "GitLab 仓库操作", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-gitlab\"]",
            "{\"GITLAB_TOKEN\":\"${GITLAB_TOKEN}\"}");

        saveStdioTemplate("brave-search", "Brave 搜索", "网络搜索", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-brave-search\"]",
            "{\"BRAVE_API_KEY\":\"${BRAVE_API_KEY}\"}");

        saveStdioTemplate("puppeteer", "Puppeteer", "浏览器自动化：截图、爬虫、测试", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-puppeteer\"]", null);

        saveStdioTemplate("fetch", "HTTP Fetch", "发送 HTTP 请求", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-fetch\"]", null);

        saveStdioTemplate("sequential-thinking", "Sequential Thinking", "结构化思维链推理", "dev",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-sequential-thinking\"]", null);

        // ===== 数据库 =====
        saveStdioTemplate("postgres", "PostgreSQL", "PostgreSQL 数据库查询和操作", "data",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-postgres\"]",
            "{\"DATABASE_URL\":\"${DATABASE_URL}\"}");

        saveStdioTemplate("mysql", "MySQL", "MySQL 数据库查询和操作", "data",
            "npx", "[\"-y\", \"mysql-mcp-server\"]",
            "{\"MYSQL_HOST\":\"${MYSQL_HOST}\",\"MYSQL_PORT\":\"${MYSQL_PORT}\",\"MYSQL_USER\":\"${MYSQL_USER}\",\"MYSQL_PASSWORD\":\"${MYSQL_PASSWORD}\",\"MYSQL_DATABASE\":\"${MYSQL_DATABASE}\"}");

        saveStdioTemplate("redis", "Redis", "Redis 缓存操作", "data",
            "npx", "[\"-y\", \"@anthropic/redis-mcp-server\"]",
            "{\"REDIS_URL\":\"${REDIS_URL}\"}");

        saveStdioTemplate("mongodb", "MongoDB", "MongoDB 数据库操作", "data",
            "npx", "[\"-y\", \"@anthropic/mongodb-mcp-server\"]",
            "{\"MONGODB_URI\":\"${MONGODB_URI}\"}");

        // ===== 协作工具 =====
        saveStdioTemplate("feishu-doc", "飞书文档", "飞书文档读写、搜索、评论", "collaboration",
            "npx", "[\"-y\", \"@anthropic/feishu-mcp-server\"]",
            "{\"FEISHU_APP_ID\":\"${FEISHU_APP_ID}\",\"FEISHU_APP_SECRET\":\"${FEISHU_APP_SECRET}\"}");

        saveStdioTemplate("feishu-bitable", "飞书多维表格", "飞书多维表格数据读写", "collaboration",
            "npx", "[\"-y\", \"@anthropic/feishu-bitable-mcp\"]",
            "{\"FEISHU_APP_ID\":\"${FEISHU_APP_ID}\",\"FEISHU_APP_SECRET\":\"${FEISHU_APP_SECRET}\"}");

        saveStdioTemplate("slack", "Slack", "Slack 消息发送和频道管理", "collaboration",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-slack\"]",
            "{\"SLACK_BOT_TOKEN\":\"${SLACK_BOT_TOKEN}\"}");

        saveStdioTemplate("jira", "Jira", "Jira 项目管理", "collaboration",
            "npx", "[\"-y\", \"@anthropic/jira-mcp-server\"]",
            "{\"JIRA_URL\":\"${JIRA_URL}\",\"JIRA_EMAIL\":\"${JIRA_EMAIL}\",\"JIRA_API_TOKEN\":\"${JIRA_API_TOKEN}\"}");

        saveStdioTemplate("linear", "Linear", "Linear 项目管理", "collaboration",
            "npx", "[\"-y\", \"@anthropic/linear-mcp-server\"]",
            "{\"LINEAR_API_KEY\":\"${LINEAR_API_KEY}\"}");

        saveStdioTemplate("notion", "Notion", "Notion 文档协作", "collaboration",
            "npx", "[\"-y\", \"@anthropic/notion-mcp-server\"]",
            "{\"NOTION_API_KEY\":\"${NOTION_API_KEY}\"}");

        // ===== 监控 =====
        saveStdioTemplate("sentry", "Sentry", "错误监控和异常追踪", "monitoring",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-sentry\"]",
            "{\"SENTRY_AUTH_TOKEN\":\"${SENTRY_AUTH_TOKEN}\"}");

        saveStdioTemplate("grafana", "Grafana", "Grafana 监控面板", "monitoring",
            "npx", "[\"-y\", \"@anthropic/grafana-mcp-server\"]",
            "{\"GRAFANA_URL\":\"${GRAFANA_URL}\",\"GRAFANA_API_KEY\":\"${GRAFANA_API_KEY}\"}");

        saveStdioTemplate("sonarqube", "SonarQube", "SonarQube 代码质量", "monitoring",
            "npx", "[\"-y\", \"@anthropic/sonarqube-mcp-server\"]",
            "{\"SONAR_URL\":\"${SONAR_URL}\",\"SONAR_TOKEN\":\"${SONAR_TOKEN}\"}");

        saveStdioTemplate("memory", "Memory", "基于知识图谱的持久化记忆", "monitoring",
            "npx", "[\"-y\", \"@modelcontextprotocol/server-memory\"]", null);

        // ===== 游戏开发 =====
        saveStdioTemplate("unity", "Unity MCP", "Unity 编辑器自动化", "gamedev",
            "npx", "[\"-y\", \"@anthropic/unity-mcp-server\"]",
            "{\"UNITY_PROJECT_PATH\":\"${UNITY_PROJECT_PATH}\"}");

        saveStdioTemplate("godot", "Godot MCP", "Godot 引擎自动化", "gamedev",
            "npx", "[\"-y\", \"@anthropic/godot-mcp-server\"]",
            "{\"GODOT_PROJECT_PATH\":\"${GODOT_PROJECT_PATH}\"}");

        saveStdioTemplate("unreal", "Unreal MCP", "Unreal Engine 自动化", "gamedev",
            "npx", "[\"-y\", \"@anthropic/unreal-mcp-server\"]",
            "{\"UE_PROJECT_PATH\":\"${UE_PROJECT_PATH}\"}");

        saveStdioTemplate("playfab", "PlayFab", "PlayFab 游戏后端", "gamedev",
            "npx", "[\"-y\", \"@anthropic/playfab-mcp-server\"]",
            "{\"PLAYFAB_TITLE_ID\":\"${PLAYFAB_TITLE_ID}\",\"PLAYFAB_SECRET_KEY\":\"${PLAYFAB_SECRET_KEY}\"}");

        saveStdioTemplate("firebase", "Firebase", "Firebase 服务", "gamedev",
            "npx", "[\"-y\", \"@anthropic/firebase-mcp-server\"]",
            "{\"FIREBASE_PROJECT_ID\":\"${FIREBASE_PROJECT_ID}\",\"FIREBASE_SERVICE_ACCOUNT\":\"${FIREBASE_SERVICE_ACCOUNT}\"}");

        saveStdioTemplate("steam", "Steam MCP", "Steam 平台集成", "gamedev",
            "npx", "[\"-y\", \"@anthropic/steam-mcp-server\"]",
            "{\"STEAM_APP_ID\":\"${STEAM_APP_ID}\",\"STEAM_API_KEY\":\"${STEAM_API_KEY}\"}");

        // ===== 资源生成 - 图片 =====

        // MiniMax 图片生成（OpenAI 兼容接口）
        saveHttpTemplate("minimax-image", "MiniMax 图片生成", "MiniMax AI 图片生成：文生图、图生图",
            "resource-image", "https://api.minimax.chat/v1/images/generations",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"eyJ...\"},{\"key\":\"MODEL\",\"label\":\"模型\",\"placeholder\":\"image-01\",\"defaultValue\":\"image-01\"}]");

        // 火山引擎图片生成
        saveHttpTemplate("volcengine-image", "火山引擎图片生成", "火山引擎智能视觉：文生图、图生图、图像编辑",
            "resource-image", "https://visual.volcengineapi.com",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"ak-xxx\"},{\"key\":\"API_SECRET\",\"label\":\"API Secret\",\"placeholder\":\"sk-xxx\"}]");

        // SiliconFlow 图片生成
        saveHttpTemplate("siliconflow-image", "SiliconFlow 图片生成", "硅基流动 AI 图片生成：Flux/SD3/SDXL",
            "resource-image", "https://api.siliconflow.cn/v1/images/generations",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"sk-xxx\"},{\"key\":\"MODEL\",\"label\":\"模型\",\"placeholder\":\"stabilityai/stable-diffusion-3-5-large\",\"defaultValue\":\"stabilityai/stable-diffusion-3-5-large\"}]");

        // ===== 资源生成 - 音频 =====

        // MiniMax 音乐生成
        saveHttpTemplate("minimax-music", "MiniMax 音乐生成", "MiniMax AI 音乐生成：文本描述生成完整音乐",
            "resource-audio", "https://api.minimax.chat/v1/music/generation",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"eyJ...\"}]");

        // MiniMax TTS 语音合成
        saveHttpTemplate("minimax-tts", "MiniMax 语音合成", "MiniMax TTS：高质量中英文语音合成，支持多种音色",
            "resource-audio", "https://api.minimax.chat/v1/t2a_v2",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"eyJ...\"},{\"key\":\"VOICE_ID\",\"label\":\"音色ID\",\"placeholder\":\"male-qn-qingse\",\"defaultValue\":\"male-qn-qingse\"}]");

        // 火山引擎语音合成
        saveHttpTemplate("volcengine-tts", "火山引擎语音合成", "火山引擎 TTS：多种音色、情感语音合成",
            "resource-audio", "https://openspeech.bytedance.com/api/v1/tts",
            "header", "Authorization", "Bearer; ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"ak-xxx\"},{\"key\":\"APP_ID\",\"label\":\"应用ID\",\"placeholder\":\"xxx\"}]");

        // 火山引擎音乐生成
        saveHttpTemplate("volcengine-music", "火山引擎音乐生成", "火山引擎 AI 音乐：文本描述生成音乐",
            "resource-audio", "https://api.volcengine.com/v1/music/generation",
            "header", "Authorization", "Bearer ${API_KEY}",
            "[{\"key\":\"API_KEY\",\"label\":\"API Key\",\"placeholder\":\"ak-xxx\"}]");

        log.info("MCP templates initialized: {} servers", serverRepository.count());
    }

    /**
     * 保存 STDIO 模式模板（环境变量认证）
     */
    private void saveStdioTemplate(String templateKey, String name, String description,
                                    String category, String command, String args, String env) {
        if (serverRepository.findByTemplateKey(templateKey).isPresent()) return;

        McpServer server = new McpServer();
        server.setName(name);
        server.setDescription(description);
        server.setTransportType(McpServer.TransportType.STDIO);
        server.setCommand(command);
        server.setArgs(args);
        server.setEnv(env);
        server.setTemplate(true);
        server.setTemplateKey(templateKey);
        server.setCategory(category);
        server.setAuthMode("env");
        server.setEnabled(true);
        server.setConnected(false);
        serverRepository.save(server);
    }

    /**
     * 保存 HTTP 模式模板（支持 header/body 认证）
     *
     * @param templateKey 模板标识
     * @param name 服务器名称
     * @param description 描述
     * @param category 分类
     * @param url API 端点
     * @param authMode 认证模式：header / body
     * @param authHeaderName 认证头名（如 Authorization、X-API-Key）
     * @param authHeaderValue 认证头值模板（如 "Bearer ${API_KEY}"，${API_KEY} 会被用户输入替换）
     * @param requiredParams 必填参数定义 JSON 数组
     */
    private void saveHttpTemplate(String templateKey, String name, String description,
                                   String category, String url,
                                   String authMode, String authHeaderName, String authHeaderValue,
                                   String requiredParams) {
        if (serverRepository.findByTemplateKey(templateKey).isPresent()) return;

        McpServer server = new McpServer();
        server.setName(name);
        server.setDescription(description);
        server.setTransportType(McpServer.TransportType.STREAMABLE_HTTP);
        server.setUrl(url);
        server.setTemplate(true);
        server.setTemplateKey(templateKey);
        server.setCategory(category);
        server.setAuthMode(authMode);
        server.setAuthHeaderName(authHeaderName);
        // 将认证头值模板存入 headers JSON
        if ("header".equals(authMode) && authHeaderValue != null) {
            server.setHeaders("{\"" + authHeaderName + "\":\"" + authHeaderValue + "\"}");
        }
        server.setRequiredParams(requiredParams);
        server.setEnabled(true);
        server.setConnected(false);
        server.setToolCount(1);  // HTTP 模板默认有 1 个预注册工具
        serverRepository.save(server);

        // 为 HTTP 模板预注册工具（REST API 不支持 MCP 协议发现，需要手动注册）
        registerToolForServer(server, templateKey, name, description, category);
    }

    /**
     * 为 HTTP 模板预注册工具
     * REST API 类型的 MCP Server 不支持 tools/list 协议，需要手动注册工具
     */
    private void registerToolForServer(McpServer server, String templateKey, String name, String description, String category) {
        String toolName = templateKey.replace("-", "_") + "_generate";
        String toolDesc = description;
        String inputSchema = "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"内容描述\"}},\"required\":[\"prompt\"]}";
        String defaultParams = "{}";
        String paramHints = "{\"prompt\":\"描述要生成的内容，尽量详细。英文效果更好。\"}";

        // 根据分类设置特定的参数
        if (category.contains("image")) {
            inputSchema = "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"图片内容描述（英文效果更好）\"}},\"required\":[\"prompt\"]}";
            paramHints = "{\"prompt\":\"描述要生成的图片内容，尽量详细。示例：a cute cartoon cat playing with yarn, digital art style\"}";
        } else if (category.contains("audio")) {
            if (name.contains("TTS") || name.contains("语音")) {
                inputSchema = "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\",\"description\":\"要合成语音的文本\"},\"voice\":{\"type\":\"string\",\"description\":\"音色名称\"}},\"required\":[\"text\"]}";
                paramHints = "{\"text\":\"要转换为语音的文本内容\",\"voice\":\"音色名称，如：male-qn-qingse（青涩男声）、female-shaonv（少女）\"}";
            } else {
                inputSchema = "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"音乐风格描述\"},\"title\":{\"type\":\"string\",\"description\":\"歌曲名称\"}},\"required\":[\"prompt\"]}";
                paramHints = "{\"prompt\":\"描述音乐风格和内容，示例：upbeat electronic dance music with synth bass\",\"title\":\"歌曲名称，示例：Sunset Dreams\"}";
            }
        } else if (category.contains("video")) {
            inputSchema = "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"视频内容描述\"}},\"required\":[\"prompt\"]}";
            paramHints = "{\"prompt\":\"描述视频内容，示例：a cat walking on the beach at sunset, cinematic style\"}";
        } else if (category.contains("3d")) {
            inputSchema = "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"3D 模型描述\"}},\"required\":[\"prompt\"]}";
            paramHints = "{\"prompt\":\"描述 3D 模型，示例：a low-poly treasure chest with gold coins\"}";
        }

        // 检查工具是否已存在
        if (toolRepository.findByServerIdAndToolName(server.getId(), toolName).isPresent()) {
            return;
        }

        McpTool tool = new McpTool();
        tool.setServerId(server.getId());
        tool.setToolName(toolName);
        tool.setDisplayName(name);
        tool.setDescription(toolDesc);
        tool.setInputSchema(inputSchema);
        tool.setDefaultParams(defaultParams);
        tool.setParamHints(paramHints);
        tool.setCategory(category);
        tool.setEnabled(true);
        toolRepository.save(tool);

        log.info("预注册工具: {} for server {}", toolName, server.getName());
    }
}
