package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.McpService;
import com.chengxun.gamemaker.web.dto.ErrorResponse;
import com.chengxun.gamemaker.web.entity.AgentMcpBinding;
import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.entity.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MCP 管理控制器
 * 提供 MCP Server 管理、工具发现、工具分配的 API 和页面
 *
 * @author chengxun
 * @since 2.0.0
 */
@Controller
@RequestMapping({"/mcp", "/api/mcp"})
@PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    // ===== 页面 =====

    /** MCP 管理主页 */
    @GetMapping
    public String mcpPage(Model model) {
        model.addAttribute("servers", mcpService.getAllServers());
        model.addAttribute("templates", mcpService.getTemplates());
        return "mcp/index";
    }

    // ===== Server API =====

    /** 获取所有 Server */
    @GetMapping("/api/servers")
    @ResponseBody
    public List<McpServer> getAllServers() {
        return mcpService.getAllServers();
    }

    /** 获取模板 */
    @GetMapping("/api/templates")
    @ResponseBody
    public List<McpServer> getTemplates() {
        return mcpService.getTemplates();
    }

    /** 获取项目级 Server */
    @GetMapping("/api/servers/project/{projectId}")
    @ResponseBody
    public List<McpServer> getProjectServers(@PathVariable String projectId) {
        return mcpService.getProjectServers(projectId);
    }

    /** 创建 Server */
    @PostMapping("/api/servers")
    @ResponseBody
    public ResponseEntity<?> createServer(@RequestBody McpServer server) {
        McpServer saved = mcpService.saveServer(server);
        log.info("MCP Server created: {}", saved.getName());
        return ResponseEntity.ok(saved);
    }

    /** 从模板安装 */
    @PostMapping("/api/servers/install")
    @ResponseBody
    public ResponseEntity<?> installFromTemplate(@RequestBody Map<String, Object> body) {
        String templateKey = (String) body.get("templateKey");
        String projectId = (String) body.get("projectId");

        @SuppressWarnings("unchecked")
        Map<String, String> envVars = (Map<String, String>) body.get("envVars");

        try {
            McpServer server = mcpService.installFromTemplate(templateKey, projectId, envVars);
            return ResponseEntity.ok(server);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /** 更新 Server */
    @PutMapping("/api/servers/{id}")
    @ResponseBody
    public ResponseEntity<?> updateServer(@PathVariable Long id, @RequestBody McpServer server) {
        McpServer existing = mcpService.getServer(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        // 更新已有实体的字段，避免 JPA 创建新记录
        existing.setName(server.getName());
        existing.setDescription(server.getDescription());
        existing.setTransportType(server.getTransportType());
        existing.setCommand(server.getCommand());
        existing.setArgs(server.getArgs());
        existing.setEnv(server.getEnv());
        existing.setUrl(server.getUrl());
        existing.setHeaders(server.getHeaders());
        existing.setProjectId(server.getProjectId());
        existing.setAuthMode(server.getAuthMode());
        existing.setAuthHeaderName(server.getAuthHeaderName());
        existing.setCategory(server.getCategory());
        existing.setEnabled(server.isEnabled());
        McpServer saved = mcpService.saveServer(existing);
        return ResponseEntity.ok(saved);
    }

    /** 删除 Server */
    @DeleteMapping("/api/servers/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteServer(@PathVariable Long id) {
        mcpService.deleteServer(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 切换启用状态 */
    @PostMapping("/api/servers/{id}/toggle")
    @ResponseBody
    public McpServer toggleServer(@PathVariable Long id) {
        return mcpService.toggleServer(id);
    }

    /** 测试连接并发现工具 */
    @PostMapping("/api/servers/{id}/test")
    @ResponseBody
    public ResponseEntity<?> testServer(@PathVariable Long id) {
        try {
            McpServer server = mcpService.testAndDiscover(id);
            return ResponseEntity.ok(server);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    // ===== Tool API =====

    /** 获取 Server 的工具 */
    @GetMapping("/api/servers/{serverId}/tools")
    @ResponseBody
    public List<McpTool> getTools(@PathVariable Long serverId) {
        return mcpService.getTools(serverId);
    }

    /** 切换工具启用状态 */
    @PostMapping("/api/tools/{toolId}/toggle")
    @ResponseBody
    public McpTool toggleTool(@PathVariable Long toolId) {
        return mcpService.toggleTool(toolId);
    }

    /** 设置工具是否需要审批 */
    @PostMapping("/api/tools/{toolId}/approval")
    @ResponseBody
    public McpTool setToolApproval(@PathVariable Long toolId, @RequestBody Map<String, Boolean> body) {
        boolean requiresApproval = body.getOrDefault("requiresApproval", false);
        return mcpService.setToolApproval(toolId, requiresApproval);
    }

    /** 更新工具配置（默认参数、AI 提示等） */
    @PutMapping("/api/tools/{toolId}")
    @ResponseBody
    public ResponseEntity<?> updateTool(@PathVariable Long toolId, @RequestBody Map<String, Object> body) {
        try {
            McpTool tool = mcpService.getTool(toolId);
            if (tool == null) return ResponseEntity.notFound().build();

            if (body.containsKey("defaultParams")) {
                tool.setDefaultParams((String) body.get("defaultParams"));
            }
            if (body.containsKey("paramHints")) {
                tool.setParamHints((String) body.get("paramHints"));
            }
            if (body.containsKey("displayName")) {
                tool.setDisplayName((String) body.get("displayName"));
            }
            mcpService.saveTool(tool);
            return ResponseEntity.ok(tool);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 绑定 API =====

    /** 绑定工具给 Agent */
    @PostMapping("/api/bind")
    @ResponseBody
    public ResponseEntity<?> bindTool(@RequestBody Map<String, Object> body) {
        String agentRole = (String) body.get("agentRole");
        String projectId = (String) body.get("projectId");
        Long serverId = ((Number) body.get("serverId")).longValue();
        Long toolId = body.get("toolId") != null ? ((Number) body.get("toolId")).longValue() : null;

        AgentMcpBinding binding = mcpService.bindTool(agentRole, projectId, serverId, toolId);
        return ResponseEntity.ok(binding);
    }

    /** 解绑 */
    @PostMapping("/api/unbind/{bindingId}")
    @ResponseBody
    public ResponseEntity<?> unbind(@PathVariable Long bindingId) {
        mcpService.unbindTool(bindingId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 获取 Agent 的绑定列表 */
    @GetMapping("/api/bindings/{agentRole}")
    @ResponseBody
    public List<AgentMcpBinding> getBindings(@PathVariable String agentRole,
                                              @RequestParam String projectId) {
        return mcpService.getBindings(agentRole, projectId);
    }

    /** 获取 Agent 绑定的 Server ID 列表 */
    @GetMapping("/api/bindings/{agentRole}/servers")
    @ResponseBody
    public Set<Long> getBoundServerIds(@PathVariable String agentRole,
                                        @RequestParam String projectId) {
        return mcpService.getBoundServerIds(agentRole, projectId);
    }

    /** 生成 Agent 的 MCP 配置 */
    @GetMapping("/api/config/{agentRole}")
    @ResponseBody
    public ResponseEntity<?> generateConfig(@PathVariable String agentRole,
                                             @RequestParam String projectId) {
        String config = mcpService.generateMcpConfig(agentRole, projectId);
        if (config == null) {
            return ResponseEntity.ok(Map.of("configured", false, "message", "未配置 MCP"));
        }
        return ResponseEntity.ok(Map.of("configured", true, "config", config));
    }
}
