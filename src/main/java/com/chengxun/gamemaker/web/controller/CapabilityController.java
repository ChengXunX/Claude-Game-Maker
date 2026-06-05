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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 能力管理 REST API
 * 提供能力定义的 CRUD、调用日志查询和上下文健康状态查询
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/capabilities")
@PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
public class CapabilityController {

    private static final Logger log = LoggerFactory.getLogger(CapabilityController.class);

    private final CapabilityRegistry capabilityRegistry;
    private final CapabilityInvocationLogRepository logRepository;
    private final ContextMonitor contextMonitor;

    public CapabilityController(CapabilityRegistry capabilityRegistry,
                                 CapabilityInvocationLogRepository logRepository,
                                 ContextMonitor contextMonitor) {
        this.capabilityRegistry = capabilityRegistry;
        this.logRepository = logRepository;
        this.contextMonitor = contextMonitor;
    }

    // ===== 能力管理 =====

    /**
     * 获取所有能力（管理界面用）
     */
    @GetMapping
    public ResponseEntity<List<AgentCapability>> getAllCapabilities() {
        return ResponseEntity.ok(capabilityRegistry.getAllCapabilities());
    }

    /**
     * 获取某角色的能力列表
     */
    @GetMapping("/role/{agentRole}")
    public ResponseEntity<List<AgentCapability>> getCapabilitiesByRole(@PathVariable String agentRole) {
        return ResponseEntity.ok(capabilityRegistry.getCapabilities(agentRole));
    }

    /**
     * 获取某角色在某项目下的能力
     */
    @GetMapping("/role/{agentRole}/project/{projectId}")
    public ResponseEntity<List<AgentCapability>> getCapabilitiesByRoleAndProject(
            @PathVariable String agentRole, @PathVariable String projectId) {
        return ResponseEntity.ok(capabilityRegistry.getCapabilities(agentRole, projectId));
    }

    /**
     * 创建能力
     */
    @PostMapping
    public ResponseEntity<AgentCapability> createCapability(@RequestBody AgentCapability capability) {
        AgentCapability saved = capabilityRegistry.save(capability);
        log.info("Capability created: {} for role {}", saved.getCapabilityName(), saved.getAgentRole());
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新能力
     */
    @PutMapping("/{id}")
    public ResponseEntity<AgentCapability> updateCapability(@PathVariable Long id,
                                                            @RequestBody AgentCapability capability) {
        capability.setId(id);
        AgentCapability saved = capabilityRegistry.save(capability);
        log.info("Capability updated: {} (id={})", saved.getCapabilityName(), id);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除能力
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCapability(@PathVariable Long id) {
        capabilityRegistry.delete(id);
        log.info("Capability deleted: id={}", id);
        return ResponseEntity.ok().build();
    }

    /**
     * 启用/禁用能力
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<AgentCapability> toggleCapability(@PathVariable Long id) {
        AgentCapability toggled = capabilityRegistry.toggleEnabled(id);
        log.info("Capability toggled: {} -> enabled={}", toggled.getCapabilityName(), toggled.isEnabled());
        return ResponseEntity.ok(toggled);
    }

    /**
     * 重新加载能力缓存
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadCapabilities() {
        capabilityRegistry.reloadAll();
        log.info("Capability cache reloaded");
        return ResponseEntity.ok(Map.of("success", true, "message", "能力缓存已重新加载"));
    }

    /**
     * 按分类查询能力
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AgentCapability>> getCapabilitiesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(capabilityRegistry.getCapabilitiesByCategory(category));
    }

    /**
     * 按项目查询能力覆盖
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<AgentCapability>> getCapabilitiesByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(capabilityRegistry.getCapabilitiesByProject(projectId));
    }

    /**
     * 获取所有已注册的角色
     */
    @GetMapping("/roles")
    public ResponseEntity<java.util.Set<String>> getRegisteredRoles() {
        return ResponseEntity.ok(capabilityRegistry.getRegisteredRoles());
    }

    // ===== 调用日志 =====

    /**
     * 查询调用日志（分页）
     */
    @GetMapping("/invocations")
    public ResponseEntity<Page<CapabilityInvocationLog>> getInvocationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(logRepository.findAll(PageRequest.of(page, size)));
    }

    /**
     * 按 Agent 查询调用日志
     */
    @GetMapping("/invocations/agent/{agentId}")
    public ResponseEntity<Page<CapabilityInvocationLog>> getInvocationLogsByAgent(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(logRepository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(page, size)));
    }

    /**
     * 按项目查询调用日志
     */
    @GetMapping("/invocations/project/{projectId}")
    public ResponseEntity<Page<CapabilityInvocationLog>> getInvocationLogsByProject(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(logRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, size)));
    }

    // ===== 上下文健康状态 =====

    /**
     * 获取所有 Agent 的上下文健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, ContextMonitor.ContextHealthStatus>> getAllHealthStatus() {
        return ResponseEntity.ok(contextMonitor.getAllHealthStatus());
    }

    /**
     * 获取上下文健康统计
     */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        return ResponseEntity.ok(contextMonitor.getHealthSummary());
    }

    /**
     * 手动触发 Agent 上下文恢复
     */
    @PostMapping("/health/{agentId}/recover")
    public ResponseEntity<Map<String, Object>> recoverAgent(@PathVariable String agentId) {
        boolean success = contextMonitor.manualRecover(agentId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "agentId", agentId,
            "message", success ? "恢复已触发" : "恢复失败"
        ));
    }
}
