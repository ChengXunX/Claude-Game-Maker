package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.CapabilityRegistry;
import com.chengxun.gamemaker.service.ContextMonitor;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog;
import com.chengxun.gamemaker.web.repository.CapabilityInvocationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 能力管理页面控制器
 * 提供能力管理和上下文监控的页面路由，以及页面专用的 API（session 认证）
 *
 * @author chengxun
 * @since 2.0.0
 */
@Controller
@PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
public class CapabilityPageController {

    private static final Logger log = LoggerFactory.getLogger(CapabilityPageController.class);

    private final CapabilityRegistry capabilityRegistry;
    private final CapabilityInvocationLogRepository logRepository;
    private final ContextMonitor contextMonitor;

    public CapabilityPageController(CapabilityRegistry capabilityRegistry,
                                     CapabilityInvocationLogRepository logRepository,
                                     ContextMonitor contextMonitor) {
        this.capabilityRegistry = capabilityRegistry;
        this.logRepository = logRepository;
        this.contextMonitor = contextMonitor;
    }

    /**
     * 能力管理页面
     */
    @GetMapping("/capabilities")
    public String capabilities(Model model, Authentication authentication) {
        addUserPermissions(model, authentication);
        return "capabilities";
    }

    /**
     * 上下文监控页面（复用 capabilities 页面的健康状态部分）
     */
    @GetMapping("/context-health")
    public String contextHealth(Model model, Authentication authentication) {
        addUserPermissions(model, authentication);
        return "capabilities";
    }

    /**
     * 将用户权限添加到 Model，供前端控制元素显示
     */
    private void addUserPermissions(Model model, Authentication authentication) {
        if (authentication != null) {
            List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            model.addAttribute("userPermissions", authorities);
            model.addAttribute("username", authentication.getName());

            boolean canManage = authorities.contains("PERM_agents:manage")
                || authorities.contains("PERM_admin:manage");
            boolean canMonitor = authorities.contains("PERM_agents:manage")
                || authorities.contains("PERM_system:monitor")
                || authorities.contains("PERM_admin:manage");
            boolean isAdmin = authorities.contains("PERM_admin:manage")
                || authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            model.addAttribute("canManageCapabilities", canManage);
            model.addAttribute("canMonitorContext", canMonitor);
            model.addAttribute("isAdmin", isAdmin);
        }
    }

    // ===== 页面专用 API（session 认证，非 /api/ 前缀） =====

    @GetMapping("/capabilities/api/all")
    @ResponseBody
    public List<AgentCapability> getAllCapabilities() {
        return capabilityRegistry.getAllCapabilities();
    }

    @GetMapping("/capabilities/api/roles")
    @ResponseBody
    public java.util.Set<String> getRoles() {
        return capabilityRegistry.getRegisteredRoles();
    }

    @PostMapping("/capabilities/api/{id}/toggle")
    @ResponseBody
    public AgentCapability toggleCapability(@PathVariable Long id) {
        return capabilityRegistry.toggleEnabled(id);
    }

    @PostMapping("/capabilities/api/reload")
    @ResponseBody
    public Map<String, Object> reloadCapabilities() {
        capabilityRegistry.reloadAll();
        return Map.of("success", true, "message", "能力缓存已重新加载");
    }

    @PostMapping("/capabilities/api/create")
    @ResponseBody
    public ResponseEntity<?> createCapability(@RequestBody AgentCapability capability) {
        // 输入校验
        if (capability.getCapabilityName() == null || !capability.getCapabilityName().matches("^[a-zA-Z][a-zA-Z0-9_]{1,50}$")) {
            return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("能力名称格式错误，必须以字母开头，只允许字母数字下划线"));
        }
        if (capability.getAgentRole() == null || capability.getAgentRole().isBlank()) {
            return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("角色不能为空"));
        }
        AgentCapability saved = capabilityRegistry.save(capability);
        log.info("Capability created via page: {} for role {}", saved.getCapabilityName(), saved.getAgentRole());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/capabilities/api/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateCapability(@PathVariable Long id, @RequestBody AgentCapability capability) {
        capability.setId(id);
        AgentCapability saved = capabilityRegistry.save(capability);
        log.info("Capability updated via page: {} (id={})", saved.getCapabilityName(), id);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/capabilities/api/health")
    @ResponseBody
    public Map<String, ContextMonitor.ContextHealthStatus> getHealthStatus() {
        return contextMonitor.getAllHealthStatus();
    }

    @GetMapping("/capabilities/api/health/summary")
    @ResponseBody
    public Map<String, Object> getHealthSummary() {
        return contextMonitor.getHealthSummary();
    }

    @PostMapping("/capabilities/api/health/{agentId}/recover")
    @ResponseBody
    public Map<String, Object> recoverAgent(@PathVariable String agentId) {
        boolean success = contextMonitor.manualRecover(agentId);
        return Map.of("success", success, "agentId", agentId, "message", success ? "恢复已触发" : "恢复失败");
    }
}
