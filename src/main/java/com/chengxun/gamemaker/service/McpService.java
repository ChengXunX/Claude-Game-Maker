package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.AgentMcpBinding;
import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.entity.McpTool;
import com.chengxun.gamemaker.web.repository.AgentMcpBindingRepository;
import com.chengxun.gamemaker.web.repository.McpServerRepository;
import com.chengxun.gamemaker.web.repository.McpToolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 服务
 * 管理 MCP Server、工具发现、工具分配、配置生成
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final McpServerRepository serverRepository;
    private final McpToolRepository toolRepository;
    private final AgentMcpBindingRepository bindingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Lazy
    @Autowired(required = false)
    private AgentManager agentManager;

    public McpService(McpServerRepository serverRepository,
                      McpToolRepository toolRepository,
                      AgentMcpBindingRepository bindingRepository) {
        this.serverRepository = serverRepository;
        this.toolRepository = toolRepository;
        this.bindingRepository = bindingRepository;
    }

    // ===== Server 管理 =====

    /** 获取所有 Server */
    public List<McpServer> getAllServers() {
        return serverRepository.findAllByOrderByNameAsc();
    }

    /** 获取启用的 Server */
    public List<McpServer> getEnabledServers() {
        return serverRepository.findByEnabledTrueOrderByNameAsc();
    }

    /** 获取模板 Server */
    public List<McpServer> getTemplates() {
        return serverRepository.findByTemplateTrueOrderByNameAsc();
    }

    /** 获取项目级 Server */
    public List<McpServer> getProjectServers(String projectId) {
        return serverRepository.findByProjectIdOrderByNameAsc(projectId);
    }

    /** 获取 Server */
    public McpServer getServer(Long id) {
        return serverRepository.findById(id).orElse(null);
    }

    /** 保存 Server */
    public McpServer saveServer(McpServer server) {
        return serverRepository.save(server);
    }

    /** 从模板安装 Server */
    public McpServer installFromTemplate(String templateKey, String projectId, Map<String, String> envVars) {
        McpServer template = serverRepository.findByTemplateKey(templateKey)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateKey));

        McpServer server = new McpServer();
        server.setName(template.getName());
        server.setDescription(template.getDescription());
        server.setTransportType(template.getTransportType());
        server.setCommand(template.getCommand());
        server.setArgs(template.getArgs());
        server.setUrl(template.getUrl());
        server.setHeaders(template.getHeaders());
        server.setCategory(template.getCategory());
        server.setAuthMode(template.getAuthMode());
        server.setAuthHeaderName(template.getAuthHeaderName());
        server.setRequiredParams(template.getRequiredParams());
        server.setEnabled(true);
        server.setTemplate(false);
        server.setProjectId(projectId);

        // 替换所有 ${VAR} 占位符（env/headers/args/command/url）
        if (envVars != null && !envVars.isEmpty()) {
            server.setEnv(replacePlaceholders(server.getEnv(), envVars));
            server.setHeaders(replacePlaceholders(server.getHeaders(), envVars));
            server.setCommand(replacePlaceholders(server.getCommand(), envVars));
            server.setArgs(replacePlaceholders(server.getArgs(), envVars));
            server.setUrl(replacePlaceholders(server.getUrl(), envVars));
        }

        McpServer saved = serverRepository.save(server);
        log.info("MCP Server installed from template: {} for project {}", templateKey, projectId);
        return saved;
    }

    /**
     * 替换字符串中的 ${KEY} 占位符
     */
    private String replacePlaceholders(String text, Map<String, String> vars) {
        if (text == null || text.isEmpty() || vars == null || vars.isEmpty()) return text;
        String result = text;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /** 删除 Server */
    public void deleteServer(Long id) {
        bindingRepository.deleteByServerId(id);
        serverRepository.deleteById(id);
        log.info("MCP Server deleted: {}", id);
    }

    /** 切换启用状态 */
    public McpServer toggleServer(Long id) {
        McpServer server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server 不存在"));
        server.setEnabled(!server.isEnabled());
        return serverRepository.save(server);
    }

    // ===== 工具发现 =====

    /** 测试连接并发现工具 */
    public McpServer testAndDiscover(Long serverId) {
        McpServer server = serverRepository.findById(serverId)
            .orElseThrow(() -> new RuntimeException("Server 不存在"));

        try {
            List<Map<String, Object>> tools = discoverTools(server);
            server.setToolCount(tools.size());
            server.setConnected(true);
            server.setLastTestAt(LocalDateTime.now());
            server.setLastTestResult("连接成功，发现 " + tools.size() + " 个工具");

            // 保存工具
            for (Map<String, Object> toolData : tools) {
                saveTool(server.getId(), toolData);
            }

            serverRepository.save(server);
            log.info("MCP Server tested: {} - {} tools discovered", server.getName(), tools.size());

        } catch (Exception e) {
            server.setConnected(false);
            server.setLastTestAt(LocalDateTime.now());
            server.setLastTestResult("连接失败: " + e.getMessage());
            serverRepository.save(server);
            log.error("MCP Server test failed: {}", server.getName(), e);
        }

        return server;
    }

    /** 发现 MCP Server 的工具列表 */
    private List<Map<String, Object>> discoverTools(McpServer server) throws Exception {
        if (server.getTransportType() == McpServer.TransportType.STDIO) {
            return discoverToolsStdio(server);
        } else if (server.getTransportType() == McpServer.TransportType.SSE ||
                   server.getTransportType() == McpServer.TransportType.STREAMABLE_HTTP) {
            return discoverToolsHttp(server);
        }
        throw new RuntimeException("不支持的传输方式: " + server.getTransportType());
    }

    /** stdio 模式发现工具 */
    private List<Map<String, Object>> discoverToolsStdio(McpServer server) throws Exception {
        // 构建 MCP 初始化请求
        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"game-maker\",\"version\":\"1.0\"}}}";
        String toolsRequest = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";

        ProcessBuilder pb = new ProcessBuilder();
        List<String> command = buildCommand(server);
        pb.command(command);

        // 设置环境变量
        if (server.getEnv() != null && !server.getEnv().isEmpty()) {
            Map<String, String> envMap = objectMapper.readValue(server.getEnv(), new TypeReference<>() {});
            pb.environment().putAll(envMap);
        }

        Process process = pb.start();

        // 发送请求
        OutputStream stdin = process.getOutputStream();
        stdin.write((initRequest + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();

        // 等待初始化响应
        Thread.sleep(1000);

        stdin.write((toolsRequest + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();

        // 读取响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        long timeout = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timeout && (line = reader.readLine()) != null) {
            response.append(line);
            if (line.contains("\"method\":\"tools/list\"") || line.contains("\"id\":2")) {
                break;
            }
        }

        process.destroy();

        // 解析工具列表
        return parseToolsResponse(response.toString());
    }

    /** HTTP 模式发现工具 */
    private List<Map<String, Object>> discoverToolsHttp(McpServer server) throws Exception {
        String url = server.getUrl();
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("HTTP 模式需要配置 URL");
        }

        // 发送 tools/list 请求
        URL requestUrl = URI.create(url).toURL();
        HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        // 设置请求头
        if (server.getHeaders() != null && !server.getHeaders().isEmpty()) {
            Map<String, String> headers = objectMapper.readValue(server.getHeaders(), new TypeReference<>() {});
            headers.forEach(conn::setRequestProperty);
        }

        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}";
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP 请求失败: " + code);
        }

        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return parseToolsResponse(response);
    }

    /** 解析工具响应 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseToolsResponse(String response) {
        try {
            Map<String, Object> root = objectMapper.readValue(response, new TypeReference<>() {});
            Object result = root.get("result");
            if (result instanceof Map) {
                Object tools = ((Map<?, ?>) result).get("tools");
                if (tools instanceof List) {
                    return (List<Map<String, Object>>) tools;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse tools response: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /** 保存工具 */
    private void saveTool(Long serverId, Map<String, Object> toolData) {
        String toolName = (String) toolData.get("name");
        if (toolName == null) return;

        Optional<McpTool> existing = toolRepository.findByServerIdAndToolName(serverId, toolName);
        McpTool tool = existing.orElse(new McpTool());
        tool.setServerId(serverId);
        tool.setToolName(toolName);
        tool.setDisplayName((String) toolData.get("displayName"));
        tool.setDescription((String) toolData.get("description"));

        Object schema = toolData.get("inputSchema");
        if (schema != null) {
            try {
                tool.setInputSchema(objectMapper.writeValueAsString(schema));
            } catch (Exception e) {
                // 忽略
            }
        }

        tool.setEnabled(true);
        toolRepository.save(tool);
    }

    /** 构建 stdio 命令 */
    private List<String> buildCommand(McpServer server) {
        List<String> command = new ArrayList<>();
        String cmd = server.getCommand();
        if (cmd != null && !cmd.isEmpty()) {
            command.addAll(Arrays.asList(cmd.split("\\s+")));
        }

        String args = server.getArgs();
        if (args != null && !args.isEmpty()) {
            try {
                List<String> argsList = objectMapper.readValue(args, new TypeReference<>() {});
                command.addAll(argsList);
            } catch (Exception e) {
                // 尝试按空格分割
                command.addAll(Arrays.asList(args.split("\\s+")));
            }
        }
        return command;
    }

    // ===== 工具管理 =====

    /** 获取单个工具 */
    public McpTool getTool(Long toolId) {
        return toolRepository.findById(toolId).orElse(null);
    }

    /** 保存工具 */
    public McpTool saveTool(McpTool tool) {
        return toolRepository.save(tool);
    }

    /** 获取 Server 的工具列表 */
    public List<McpTool> getTools(Long serverId) {
        return toolRepository.findByServerIdOrderByToolNameAsc(serverId);
    }

    /** 获取启用的工具 */
    public List<McpTool> getEnabledTools(Long serverId) {
        return toolRepository.findByServerIdAndEnabledTrueOrderByToolNameAsc(serverId);
    }

    /** 切换工具启用状态 */
    public McpTool toggleTool(Long toolId) {
        McpTool tool = toolRepository.findById(toolId)
            .orElseThrow(() -> new RuntimeException("工具不存在"));
        tool.setEnabled(!tool.isEnabled());
        return toolRepository.save(tool);
    }

    /** 设置工具是否需要审批 */
    public McpTool setToolApproval(Long toolId, boolean requiresApproval) {
        McpTool tool = toolRepository.findById(toolId)
            .orElseThrow(() -> new RuntimeException("工具不存在"));
        tool.setRequiresApproval(requiresApproval);
        return toolRepository.save(tool);
    }

    // ===== 工具分配 =====

    /** 分配工具给 Agent */
    public AgentMcpBinding bindTool(String agentRole, String projectId, Long serverId, Long toolId) {
        // 检查是否已绑定
        Optional<AgentMcpBinding> existing = bindingRepository
            .findByAgentRoleAndProjectIdAndServerIdAndToolId(agentRole, projectId, serverId, toolId);
        if (existing.isPresent()) {
            AgentMcpBinding binding = existing.get();
            if (!binding.isEnabled()) {
                binding.setEnabled(true);
                return bindingRepository.save(binding);
            }
            return binding;
        }

        AgentMcpBinding binding = new AgentMcpBinding();
        binding.setAgentRole(agentRole);
        binding.setProjectId(projectId);
        binding.setServerId(serverId);
        binding.setToolId(toolId);
        binding.setEnabled(true);
        return bindingRepository.save(binding);
    }

    /** 绑定整个 Server 给 Agent */
    public AgentMcpBinding bindServer(String agentRole, String projectId, Long serverId) {
        return bindTool(agentRole, projectId, serverId, null);
    }

    /** 解绑 */
    public void unbindTool(Long bindingId) {
        bindingRepository.deleteById(bindingId);
    }

    /** 获取 Agent 的绑定列表 */
    public List<AgentMcpBinding> getBindings(String agentRole, String projectId) {
        return bindingRepository.findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(
            agentRole, projectId);
    }

    /** 获取 Agent 绑定的 Server ID 列表 */
    public Set<Long> getBoundServerIds(String agentRole, String projectId) {
        return bindingRepository.findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(
            agentRole, projectId).stream()
            .map(AgentMcpBinding::getServerId)
            .collect(Collectors.toSet());
    }

    // ===== 配置生成 =====

    /**
     * 获取 Agent 可用的 MCP 工具列表
     *
     * @param agentRole Agent 角色
     * @param projectId 项目 ID
     * @return 可用的 MCP 工具列表
     */
    public List<McpTool> getToolsForAgent(String agentRole, String projectId) {
        List<AgentMcpBinding> bindings = getBindings(agentRole, projectId);
        if (bindings.isEmpty()) return List.of();

        Set<Long> serverIds = bindings.stream()
            .map(AgentMcpBinding::getServerId)
            .collect(Collectors.toSet());

        return toolRepository.findAll().stream()
            .filter(t -> serverIds.contains(t.getServerId()))
            .filter(McpTool::isEnabled)
            .toList();
    }

    /**
     * 构建 MCP 工具描述（注入 Agent 提示词）
     * 让 Agent 知道有哪些 MCP 工具可用及其参数格式
     *
     * @param agentRole Agent 角色
     * @param projectId 项目 ID
     * @return 格式的工具描述文本，无工具返回空字符串
     */
    public String buildMcpToolPrompt(String agentRole, String projectId) {
        List<McpTool> tools = getToolsForAgent(agentRole, projectId);
        if (tools.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## 可用的 MCP 工具\n\n");
        sb.append("你可以通过 `callMcpTool` 能力调用以下外部工具生成资源。\n");
        sb.append("只需传入你负责的参数（如 prompt），已预配的参数（如 model）会自动注入。\n\n");

        for (McpTool tool : tools) {
            sb.append("### ").append(tool.getToolName());
            if (tool.getDisplayName() != null && !tool.getDisplayName().isEmpty()) {
                sb.append("（").append(tool.getDisplayName()).append("）");
            }
            sb.append("\n");
            if (tool.getDescription() != null && !tool.getDescription().isEmpty()) {
                sb.append(tool.getDescription()).append("\n");
            }

            // 解析 inputSchema，提取参数信息
            String requiredParams = "";
            String optionalParams = "";
            if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                try {
                    Map<String, Object> schema = objectMapper.readValue(tool.getInputSchema(), new TypeReference<>() {});
                    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                    List<String> required = (List<String>) schema.get("required");
                    Set<String> requiredSet = required != null ? new HashSet<>(required) : Set.of();

                    // 解析 defaultParams，标记已预配的参数
                    Map<String, Object> defaults = new LinkedHashMap<>();
                    if (tool.getDefaultParams() != null && !tool.getDefaultParams().isEmpty()) {
                        try {
                            defaults = objectMapper.readValue(tool.getDefaultParams(), new TypeReference<>() {});
                        } catch (Exception ignored) {}
                    }

                    if (properties != null) {
                        StringBuilder reqSb = new StringBuilder();
                        StringBuilder optSb = new StringBuilder();
                        for (Map.Entry<String, Object> entry : properties.entrySet()) {
                            String paramName = entry.getKey();
                            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
                            String desc = (String) prop.get("description");
                            String type = (String) prop.get("type");

                            // 已预配的参数不需要 Agent 传
                            if (defaults.containsKey(paramName)) {
                                continue;
                            }

                            String paramLine = "  - `" + paramName + "` (" + type + ")";
                            if (desc != null) paramLine += " — " + desc;

                            if (requiredSet.contains(paramName)) {
                                reqSb.append(paramLine).append(" **[必填]**\n");
                            } else {
                                optSb.append(paramLine).append("\n");
                            }
                        }
                        requiredParams = reqSb.toString();
                        optionalParams = optSb.toString();
                    }
                } catch (Exception e) {
                    // 解析失败，降级显示原始 schema
                    requiredParams = "  参数: `" + truncate(tool.getInputSchema(), 200) + "`\n";
                }
            }

            // 显示已预配参数
            if (tool.getDefaultParams() != null && !tool.getDefaultParams().isEmpty()) {
                sb.append("已预配参数: `").append(truncate(tool.getDefaultParams(), 150)).append("`（自动注入，无需传）\n");
            }

            // 显示需要 Agent 传的参数
            if (!requiredParams.isEmpty()) {
                sb.append("你需要传的参数:\n").append(requiredParams);
            }
            if (!optionalParams.isEmpty()) {
                sb.append("可选参数:\n").append(optionalParams);
            }

            // 显示参数提示（AI 填写指导）
            if (tool.getParamHints() != null && !tool.getParamHints().isEmpty()) {
                sb.append("填写指导:\n");
                try {
                    Map<String, String> hints = objectMapper.readValue(tool.getParamHints(), new TypeReference<>() {});
                    for (Map.Entry<String, String> hint : hints.entrySet()) {
                        sb.append("  - `").append(hint.getKey()).append("`: ").append(hint.getValue()).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("  ").append(tool.getParamHints()).append("\n");
                }
            }

            // 生成示例调用
            sb.append("示例: `callMcpTool(\"").append(tool.getToolName()).append("\", {\"prompt\": \"你的描述\"})`\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 调用 MCP 工具
     * 通过 MCP Server 的 JSON-RPC 接口执行工具调用
     *
     * 参数合并优先级（高覆盖低）：
     * 1. Agent 调用时传入的 arguments（最高）
     * 2. Agent 绑定的 Token 配置（apiKey, apiUrl, model）
     * 3. 工具的 defaultParams（最低）
     *
     * @param toolName 工具名称
     * @param arguments 工具参数（JSON 字符串，Agent 传入）
     * @param agentId Agent ID（用于查找 Token 配置，可选）
     * @return 调用结果
     */
    public String invokeTool(String toolName, String arguments, String agentId) {
        // 查找工具
        McpTool tool = toolRepository.findAll().stream()
            .filter(t -> t.getToolName().equals(toolName) && t.isEnabled())
            .findFirst()
            .orElse(null);

        if (tool == null) {
            return "错误：工具不存在或未启用: " + toolName;
        }

        McpServer server = serverRepository.findById(tool.getServerId()).orElse(null);
        if (server == null || !server.isEnabled()) {
            return "错误：MCP Server 不可用: " + tool.getServerId();
        }

        try {
            // 统计调用次数
            tool.setCallCount(tool.getCallCount() + 1);
            toolRepository.save(tool);

            // 合并参数：工具默认 → Agent Token → MCP Server 自有配置 → Agent 传入
            String mergedArgs = mergeArguments(server, tool, arguments, agentId);

            if (server.getTransportType() == McpServer.TransportType.SSE
                || server.getTransportType() == McpServer.TransportType.STREAMABLE_HTTP) {
                return invokeToolHttp(server, tool, mergedArgs);
            } else {
                return "错误：暂不支持 STDIO 类型的 MCP Server 直接调用，请通过 Claude CLI 使用";
            }
        } catch (Exception e) {
            log.error("MCP 工具调用失败: tool={}", toolName, e);
            return "错误：工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 合并工具参数
     * 优先级：Agent 传入 > 工具默认参数
     */
    @SuppressWarnings("unchecked")
    private String mergeArguments(McpServer server, McpTool tool, String agentArgs, String agentId) {
        Map<String, Object> merged = new LinkedHashMap<>();

        // 1. 工具默认参数（低优先级）
        if (tool.getDefaultParams() != null && !tool.getDefaultParams().isEmpty()) {
            try {
                merged.putAll(objectMapper.readValue(tool.getDefaultParams(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("解析工具默认参数失败: tool={}", tool.getToolName());
            }
        }

        // 2. Agent 传入参数（最高优先级，覆盖默认值）
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                Map<String, Object> agentParams = objectMapper.readValue(agentArgs, new TypeReference<Map<String, Object>>() {});
                merged.putAll(agentParams);
            } catch (Exception e) {
                log.warn("解析 Agent 参数失败: {}", agentArgs);
            }
        }

        try {
            return objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            return agentArgs != null ? agentArgs : "{}";
        }
    }

    /** 从 AgentManager 获取 Agent */
    private Agent getAgentFromManager(String agentId) {
        if (agentManager == null) return null;
        return agentManager.getAgent(agentId);
    }

    /**
     * 通过 HTTP 调用 MCP 工具
     * 支持两种模式：
     * 1. MCP JSON-RPC 协议（标准 MCP Server）
     * 2. 直接 REST API 调用（如 MiniMax、火山引擎等非 MCP 的 HTTP API）
     */
    private String invokeToolHttp(McpServer server, McpTool tool, String arguments) throws Exception {
        String url = server.getUrl();
        if (url == null || url.isEmpty()) {
            return "错误：MCP Server URL 未配置";
        }

        // 判断是 MCP JSON-RPC 还是 REST API
        // 如果工具有 defaultParams，说明是 REST API 模式（预配置的 HTTP API）
        boolean isRestApi = tool != null && tool.getDefaultParams() != null && !tool.getDefaultParams().isEmpty();

        String requestBody;
        if (isRestApi) {
            // REST API 模式：直接构建请求体
            Map<String, Object> body = new LinkedHashMap<>();
            // 先加载工具默认参数
            try {
                body.putAll(objectMapper.readValue(tool.getDefaultParams(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("解析工具默认参数失败: {}", tool.getToolName());
            }
            // 再叠加 Agent 传入的参数（覆盖默认值）
            if (arguments != null && !arguments.isEmpty()) {
                try {
                    body.putAll(objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    log.warn("解析 Agent 参数失败: {}", arguments);
                }
            }
            requestBody = objectMapper.writeValueAsString(body);
        } else {
            // MCP JSON-RPC 模式
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", System.currentTimeMillis());
            request.put("method", "tools/call");

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", tool != null ? tool.getToolName() : "unknown");
            if (arguments != null && !arguments.isEmpty()) {
                params.put("arguments", objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {}));
            } else {
                params.put("arguments", Map.of());
            }
            request.put("params", params);
            requestBody = objectMapper.writeValueAsString(request);
        }

        log.info("调用工具: server={}, tool={}, mode={}, body={}", url,
            tool != null ? tool.getToolName() : "unknown",
            isRestApi ? "REST" : "MCP",
            requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        if (server.getHeaders() != null && !server.getHeaders().isEmpty()) {
            Map<String, String> headers = objectMapper.readValue(server.getHeaders(), new TypeReference<>() {});
            headers.forEach(conn::setRequestProperty);
        }

        conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) {
            return String.format("错误：HTTP %d: %s", code, truncate(response, 500));
        }

        // REST API 模式：直接返回响应（格式化）
        if (isRestApi) {
            try {
                Map<String, Object> root = objectMapper.readValue(response, new TypeReference<>() {});
                // 提取关键信息（如图片 URL）
                return extractRestApiResult(root, tool.getToolName());
            } catch (Exception e) {
                return response;
            }
        }

        // MCP JSON-RPC 模式：解析 JSON-RPC 响应
        Map<String, Object> root = objectMapper.readValue(response, new TypeReference<>() {});
        if (root.containsKey("error")) {
            return "错误：" + objectMapper.writeValueAsString(root.get("error"));
        }
        Object result = root.get("result");
        if (result != null) {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        }
        return response;
    }

    /**
     * 从 REST API 响应中提取关键结果
     * 针对不同提供商的响应格式做适配
     */
    @SuppressWarnings("unchecked")
    private String extractRestApiResult(Map<String, Object> root, String toolName) {
        StringBuilder result = new StringBuilder();

        // 检查业务状态码
        Object baseResp = root.get("base_resp");
        if (baseResp instanceof Map) {
            Map<String, Object> resp = (Map<String, Object>) baseResp;
            int statusCode = resp.get("status_code") instanceof Number ? ((Number) resp.get("status_code")).intValue() : -1;
            String statusMsg = (String) resp.get("status_msg");
            if (statusCode != 0) {
                return "错误：" + statusMsg;
            }
        }

        // 提取图片 URL
        Object data = root.get("data");
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object imageUrls = dataMap.get("image_urls");
            if (imageUrls instanceof List) {
                List<String> urls = (List<String>) imageUrls;
                result.append("图片生成成功！\n");
                for (int i = 0; i < urls.size(); i++) {
                    result.append("图片 ").append(i + 1).append(": ").append(urls.get(i)).append("\n");
                }
                return result.toString().trim();
            }
            // 提取音频 URL
            Object audioUrl = dataMap.get("audio_url");
            if (audioUrl instanceof String) {
                return "音频生成成功！\n链接: " + audioUrl;
            }
            // 提取视频 URL
            Object videoUrl = dataMap.get("video_url");
            if (videoUrl instanceof String) {
                return "视频生成成功！\n链接: " + videoUrl;
            }
        }

        // 兜底：返回格式化的 JSON
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return root.toString();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 为 Agent 生成 MCP 配置 JSON
     * 用于 Claude CLI 的 --mcp-config 参数
     *
     * @param agentRole Agent 角色
     * @param projectId 项目 ID
     * @return MCP 配置 JSON 字符串
     */
    public String generateMcpConfig(String agentRole, String projectId) {
        List<AgentMcpBinding> bindings = getBindings(agentRole, projectId);
        if (bindings.isEmpty()) {
            return null;
        }

        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> mcpServers = new LinkedHashMap<>();

        Set<Long> processedServers = new HashSet<>();

        for (AgentMcpBinding binding : bindings) {
            if (processedServers.contains(binding.getServerId())) continue;

            McpServer server = serverRepository.findById(binding.getServerId()).orElse(null);
            if (server == null || !server.isEnabled()) continue;

            Map<String, Object> serverConfig = new LinkedHashMap<>();

            if (server.getTransportType() == McpServer.TransportType.STDIO) {
                serverConfig.put("command", server.getCommand());

                if (server.getArgs() != null && !server.getArgs().isEmpty()) {
                    try {
                        List<String> args = objectMapper.readValue(server.getArgs(), new TypeReference<>() {});
                        serverConfig.put("args", args);
                    } catch (Exception e) {
                        serverConfig.put("args", List.of(server.getArgs()));
                    }
                }

                // 合并环境变量：原有 env + MCP Server 独立 AI 配置
                Map<String, String> env = new LinkedHashMap<>();
                if (server.getEnv() != null && !server.getEnv().isEmpty()) {
                    try {
                        env.putAll(objectMapper.readValue(server.getEnv(), new TypeReference<>() {}));
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                if (!env.isEmpty()) {
                    serverConfig.put("env", env);
                }
            } else if (server.getTransportType() == McpServer.TransportType.SSE) {
                serverConfig.put("url", server.getUrl());
                if (server.getHeaders() != null) {
                    try {
                        Map<String, String> headers = objectMapper.readValue(server.getHeaders(), new TypeReference<>() {});
                        if (!headers.isEmpty()) {
                            serverConfig.put("headers", headers);
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            } else if (server.getTransportType() == McpServer.TransportType.STREAMABLE_HTTP) {
                serverConfig.put("type", "streamable-http");
                serverConfig.put("url", server.getUrl());
                if (server.getHeaders() != null) {
                    try {
                        Map<String, String> headers = objectMapper.readValue(server.getHeaders(), new TypeReference<>() {});
                        if (!headers.isEmpty()) {
                            serverConfig.put("headers", headers);
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            }

            mcpServers.put(server.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"), serverConfig);
            processedServers.add(server.getId());
        }

        config.put("mcpServers", mcpServers);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (Exception e) {
            log.error("Failed to generate MCP config", e);
            return "{}";
        }
    }
}
