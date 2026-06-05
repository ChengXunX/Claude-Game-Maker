package com.chengxun.gamemaker.service;

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
        server.setEnabled(true);
        server.setTemplate(false);
        server.setProjectId(projectId);

        // 替换环境变量
        if (envVars != null && !envVars.isEmpty()) {
            String env = template.getEnv();
            if (env != null) {
                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                    env = env.replace("${" + entry.getKey() + "}", entry.getValue());
                }
                server.setEnv(env);
            }

            // 替换命令中的变量
            String command = template.getCommand();
            if (command != null) {
                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                    command = command.replace("${" + entry.getKey() + "}", entry.getValue());
                }
                server.setCommand(command);
            }

            String args = template.getArgs();
            if (args != null) {
                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                    args = args.replace("${" + entry.getKey() + "}", entry.getValue());
                }
                server.setArgs(args);
            }
        }

        McpServer saved = serverRepository.save(server);
        log.info("MCP Server installed from template: {} for project {}", templateKey, projectId);
        return saved;
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

                if (server.getEnv() != null && !server.getEnv().isEmpty()) {
                    try {
                        Map<String, String> env = objectMapper.readValue(server.getEnv(), new TypeReference<>() {});
                        serverConfig.put("env", env);
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            } else if (server.getTransportType() == McpServer.TransportType.SSE) {
                serverConfig.put("url", server.getUrl());
                if (server.getHeaders() != null) {
                    try {
                        Map<String, String> headers = objectMapper.readValue(server.getHeaders(), new TypeReference<>() {});
                        serverConfig.put("headers", headers);
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
                        serverConfig.put("headers", headers);
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
