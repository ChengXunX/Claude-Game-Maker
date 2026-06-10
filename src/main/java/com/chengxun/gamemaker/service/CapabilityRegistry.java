package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.repository.AgentCapabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 能力注册服务
 * 管理所有 Agent 角色的能力定义，提供查询、验证和热更新功能
 *
 * 核心职责：
 * - 从数据库加载能力定义并缓存
 * - 支持项目级覆盖（projectId 非空时覆盖全局默认）
 * - 提供参数验证
 * - 支持热更新（清除缓存后重新加载）
 * - 能力变更后自动通知相关 Agent
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CapabilityRegistry {

    private static final Logger log = LoggerFactory.getLogger(CapabilityRegistry.class);

    /** 读写锁，保护缓存的并发访问 */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AgentCapabilityRepository capabilityRepository;

    @Autowired
    @Lazy
    private AgentManager agentManager;

    /**
     * 内存缓存
     * key: cacheKey（格式: agentRole 或 agentRole:projectId）
     * value: 该角色的能力列表（已按优先级排序）
     */
    private final ConcurrentHashMap<String, List<AgentCapability>> cache = new ConcurrentHashMap<>();

    /** 所有能力的扁平缓存（管理界面用） */
    private volatile List<AgentCapability> allCapabilities = Collections.emptyList();

    /** 能力变更历史记录 */
    private final List<CapabilityChangeRecord> changeHistory = new ArrayList<>();

    public CapabilityRegistry(AgentCapabilityRepository capabilityRepository) {
        this.capabilityRepository = capabilityRepository;
        reloadAll();
    }

    // ===== 查询能力 =====

    /**
     * 获取某角色的启用能力（项目级覆盖全局）
     * 优先返回项目级能力，如果没有项目级覆盖则返回全局默认
     *
     * @param agentRole Agent 角色
     * @param projectId 项目 ID（可为 null，表示使用全局默认）
     * @return 能力列表（按优先级排序）
     */
    public List<AgentCapability> getCapabilities(String agentRole, String projectId) {
        lock.readLock().lock();
        try {
            if (projectId != null && !projectId.isEmpty()) {
                String projectKey = agentRole + ":" + projectId;
                List<AgentCapability> projectCaps = cache.get(projectKey);
                if (projectCaps != null && !projectCaps.isEmpty()) {
                    return projectCaps;
                }
            }
            return getGlobalCapabilities(agentRole);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取某角色的全局默认能力
     */
    public List<AgentCapability> getGlobalCapabilities(String agentRole) {
        lock.readLock().lock();
        try {
            return cache.getOrDefault(agentRole, Collections.emptyList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取某角色的所有启用能力（不区分项目）
     */
    public List<AgentCapability> getCapabilities(String agentRole) {
        return getGlobalCapabilities(agentRole);
    }

    /**
     * 获取单个能力定义
     */
    public AgentCapability getCapability(String agentRole, String capabilityName, String projectId) {
        // 先查项目级
        if (projectId != null && !projectId.isEmpty()) {
            Optional<AgentCapability> projectCap = capabilityRepository
                .findByCapabilityNameAndAgentRoleAndProjectId(capabilityName, agentRole, projectId);
            if (projectCap.isPresent() && projectCap.get().isEnabled()) {
                return projectCap.get();
            }
        }
        // 再查全局
        Optional<AgentCapability> globalCap = capabilityRepository
            .findByCapabilityNameAndAgentRoleAndProjectIdIsNull(capabilityName, agentRole);
        if (globalCap.isPresent() && globalCap.get().isEnabled()) {
            return globalCap.get();
        }
        return null;
    }

    /**
     * 获取单个能力（简化版，不区分项目）
     */
    public AgentCapability getCapability(String agentRole, String capabilityName) {
        return getCapability(agentRole, capabilityName, null);
    }

    /**
     * 检查能力是否启用
     */
    public boolean isCapabilityEnabled(String agentRole, String capabilityName) {
        AgentCapability cap = getCapability(agentRole, capabilityName);
        return cap != null && cap.isEnabled();
    }

    /**
     * 检查能力是否需要审批
     */
    public boolean requiresApproval(String agentRole, String capabilityName) {
        AgentCapability cap = getCapability(agentRole, capabilityName);
        return cap != null && cap.isRequiresApproval();
    }

    /**
     * 获取需要审批的能力列表
     */
    public List<AgentCapability> getApprovalRequiredCapabilities(String agentRole) {
        return capabilityRepository.findByAgentRoleAndRequiresApprovalTrueAndEnabledTrue(agentRole);
    }

    /**
     * 获取所有能力（管理界面用）
     */
    public List<AgentCapability> getAllCapabilities() {
        return allCapabilities;
    }

    /**
     * 按分类查询能力
     */
    public List<AgentCapability> getCapabilitiesByCategory(String category) {
        return capabilityRepository.findByCategoryAndEnabledTrueOrderByPriorityAsc(category);
    }

    /**
     * 按项目查询所有能力覆盖
     */
    public List<AgentCapability> getCapabilitiesByProject(String projectId) {
        return capabilityRepository.findByProjectIdOrderByAgentRoleAscPriorityAsc(projectId);
    }

    /**
     * 获取所有已注册的角色列表
     */
    public Set<String> getRegisteredRoles() {
        return cache.keySet().stream()
            .filter(k -> !k.contains(":"))
            .collect(Collectors.toSet());
    }

    // ===== 参数验证 =====

    /**
     * 验证参数是否符合 paramSchema
     *
     * @param agentRole      Agent 角色
     * @param capabilityName 能力名称
     * @param params         待验证的参数
     * @return 验证结果（成功或错误信息列表）
     */
    public ValidationResult validateParams(String agentRole, String capabilityName, Map<String, Object> params) {
        AgentCapability cap = getCapability(agentRole, capabilityName);
        if (cap == null) {
            return ValidationResult.error("能力不存在: " + capabilityName);
        }

        if (cap.getParamSchema() == null || cap.getParamSchema().isEmpty()) {
            return ValidationResult.success(); // 无 schema 定义，跳过验证
        }

        List<String> errors = new ArrayList<>();
        try {
            // 解析 paramSchema: {"name":"string|required","role":"enum:server-dev,client-dev|required"}
            // 简化实现：只检查必填参数
            String schema = cap.getParamSchema();
            if (schema.contains("required")) {
                // 提取 required 字段名
                String[] parts = schema.replace("{", "").replace("}", "").split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.contains("required")) {
                        // 提取字段名
                        String fieldName = part.split(":")[0].trim()
                            .replace("\"", "").replace("'", "").trim();
                        if (fieldName.isEmpty()) continue;

                        if (params == null || !params.containsKey(fieldName) || params.get(fieldName) == null) {
                            errors.add("缺少必填参数: " + fieldName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to validate params for capability {}: {}", capabilityName, e.getMessage());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.error(errors);
    }

    // ===== 热更新 =====

    /**
     * 重新加载某角色的能力缓存
     */
    public void reloadCapabilities(String agentRole) {
        // 加载全局默认
        List<AgentCapability> globalCaps = capabilityRepository
            .findByAgentRoleAndProjectIdIsNullAndEnabledTrueOrderByPriorityAsc(agentRole);
        cache.put(agentRole, globalCaps);

        // 加载项目级覆盖
        List<AgentCapability> allCaps = capabilityRepository.findByAgentRoleOrderByPriorityAsc(agentRole);
        allCaps.stream()
            .filter(c -> c.getProjectId() != null && !c.getProjectId().isEmpty() && c.isEnabled())
            .collect(Collectors.groupingBy(AgentCapability::getProjectId))
            .forEach((projectId, caps) -> {
                caps.sort(Comparator.comparingInt(AgentCapability::getPriority));
                cache.put(agentRole + ":" + projectId, caps);
            });

        log.info("Reloaded capabilities for role: {} (global={}, projectOverrides={})",
            agentRole, globalCaps.size(),
            allCaps.stream().filter(AgentCapability::isProjectOverride).count());
    }

    /**
     * 重新加载所有能力缓存
     */
    public void reloadAll() {
        lock.writeLock().lock();
        try {
            cache.clear();
            allCapabilities = capabilityRepository.findAllByOrderByAgentRoleAscPriorityAsc();

            // 按角色分组加载
            allCapabilities.stream()
                .filter(AgentCapability::isEnabled)
                .collect(Collectors.groupingBy(AgentCapability::getAgentRole))
                .forEach((role, caps) -> {
                    // 全局默认
                    List<AgentCapability> globalCaps = caps.stream()
                        .filter(AgentCapability::isGlobal)
                        .sorted(Comparator.comparingInt(AgentCapability::getPriority))
                        .collect(Collectors.toList());
                    cache.put(role, globalCaps);

                    // 项目级覆盖
                    caps.stream()
                        .filter(AgentCapability::isProjectOverride)
                        .collect(Collectors.groupingBy(AgentCapability::getProjectId))
                        .forEach((projectId, projectCaps) -> {
                            projectCaps.sort(Comparator.comparingInt(AgentCapability::getPriority));
                            cache.put(role + ":" + projectId, projectCaps);
                        });
                });

            log.info("Reloaded all capabilities: {} total, {} roles",
                allCapabilities.size(), cache.keySet().stream().filter(k -> !k.contains(":")).count());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        cache.clear();
        allCapabilities = Collections.emptyList();
        log.info("Capability cache cleared");
    }

    // ===== 能力管理 =====

    /**
     * 保存能力定义
     * 保存后自动重新加载缓存并通知相关 Agent
     */
    public AgentCapability save(AgentCapability capability) {
        boolean isNew = capability.getId() == null;
        AgentCapability saved = capabilityRepository.save(capability);

        // 重新加载对应角色的缓存
        reloadCapabilities(saved.getAgentRole());

        // 记录变更历史
        recordChange(saved, isNew ? "CREATE" : "UPDATE",
            isNew ? "创建能力" : "更新能力");

        // 通知相关 Agent
        notifyAgentsCapabilityChanged(saved.getAgentRole(), saved.getProjectId(),
            isNew ? "新增" : "更新", saved.getCapabilityName());

        return saved;
    }

    /**
     * 切换能力启用状态
     * 切换后自动重新加载缓存并通知相关 Agent
     */
    public AgentCapability toggleEnabled(Long capabilityId) {
        AgentCapability cap = capabilityRepository.findById(capabilityId)
            .orElseThrow(() -> new RuntimeException("能力不存在: " + capabilityId));

        boolean oldEnabled = cap.isEnabled();
        cap.setEnabled(!cap.isEnabled());
        AgentCapability saved = capabilityRepository.save(cap);

        // 重新加载对应角色的缓存
        reloadCapabilities(saved.getAgentRole());

        // 记录变更历史
        String action = saved.isEnabled() ? "启用" : "禁用";
        recordChange(saved, "TOGGLE", action + "能力");

        // 通知相关 Agent
        notifyAgentsCapabilityChanged(saved.getAgentRole(), saved.getProjectId(),
            action, saved.getCapabilityName());

        return saved;
    }

    /**
     * 删除能力
     * 删除后自动重新加载缓存并通知相关 Agent
     */
    public void delete(Long capabilityId) {
        AgentCapability cap = capabilityRepository.findById(capabilityId)
            .orElseThrow(() -> new RuntimeException("能力不存在: " + capabilityId));

        String role = cap.getAgentRole();
        String projectId = cap.getProjectId();
        String capName = cap.getCapabilityName();

        capabilityRepository.deleteById(capabilityId);

        // 重新加载对应角色的缓存
        reloadCapabilities(role);

        // 记录变更历史
        recordChange(cap, "DELETE", "删除能力");

        // 通知相关 Agent
        notifyAgentsCapabilityChanged(role, projectId, "删除", capName);
    }

    /**
     * 批量更新能力
     * 批量操作后自动重新加载缓存并通知相关 Agent
     */
    public List<AgentCapability> batchUpdate(List<AgentCapability> capabilities) {
        List<AgentCapability> saved = capabilityRepository.saveAll(capabilities);

        // 收集受影响的角色
        Set<String> affectedRoles = saved.stream()
            .map(AgentCapability::getAgentRole)
            .collect(Collectors.toSet());

        // 重新加载所有受影响角色的缓存
        affectedRoles.forEach(this::reloadCapabilities);

        // 记录变更历史
        recordChange(null, "BATCH_UPDATE",
            String.format("批量更新 %d 个能力", saved.size()));

        // 通知所有受影响角色的 Agent
        affectedRoles.forEach(role ->
            notifyAgentsCapabilityChanged(role, null, "批量更新", "多个能力"));

        return saved;
    }

    /**
     * 记录能力变更历史
     */
    private synchronized void recordChange(AgentCapability capability, String action, String description) {
        CapabilityChangeRecord record = new CapabilityChangeRecord();
        record.setTimestamp(LocalDateTime.now());
        record.setAction(action);
        record.setDescription(description);
        if (capability != null) {
            record.setCapabilityName(capability.getCapabilityName());
            record.setAgentRole(capability.getAgentRole());
            record.setProjectId(capability.getProjectId());
        }
        changeHistory.add(record);

        // 只保留最近 1000 条记录
        if (changeHistory.size() > 1000) {
            changeHistory.remove(0);
        }

        log.info("Capability change recorded: {} - {}", action, description);
    }

    /**
     * 通知相关 Agent 能力已变更
     */
    private void notifyAgentsCapabilityChanged(String agentRole, String projectId,
                                                String action, String capabilityName) {
        if (agentManager == null) {
            log.debug("AgentManager not available, skipping notification");
            return;
        }

        try {
            // 构建通知消息
            String message = String.format(
                "## 能力变更通知\n\n" +
                "**操作**: %s\n" +
                "**能力**: %s\n" +
                "**角色**: %s\n" +
                "%s\n" +
                "**时间**: %s\n\n" +
                "你的能力配置已更新，新的能力将在下一个工作周期生效。",
                action, capabilityName, agentRole,
                projectId != null ? "**项目**: " + projectId + "\n" : "",
                LocalDateTime.now());

            // 获取相关 Agent 并发送通知
            if (projectId != null && !projectId.isEmpty()) {
                // 通知项目下的特定角色 Agent
                String agentId = projectId + ":" + agentRole;
                sendNotificationToAgent(agentId, message);
            } else {
                // 通知所有该角色的 Agent
                agentManager.getAllAgents().stream()
                    .filter(agent -> agentRole.equals(agent.getRole()))
                    .forEach(agent -> sendNotificationToAgent(agent.getId(), message));
            }

            log.info("Notified agents about capability change: role={}, action={}, capability={}",
                agentRole, action, capabilityName);
        } catch (Exception e) {
            log.warn("Failed to notify agents about capability change: {}", e.getMessage());
        }
    }

    /**
     * 向单个 Agent 发送通知
     */
    private void sendNotificationToAgent(String agentId, String message) {
        try {
            com.chengxun.gamemaker.agent.Agent agent = agentManager.getAgent(agentId);
            if (agent != null && agent.isAlive()) {
                AgentMessage notification = AgentMessage.builder()
                    .fromAgentId("system")
                    .toAgentId(agentId)
                    .type(AgentMessage.MessageType.NOTIFY)
                    .content(message)
                    .build();
                agent.receiveMessage(notification);
            }
        } catch (Exception e) {
            log.debug("Failed to send notification to agent {}: {}", agentId, e.getMessage());
        }
    }

    /**
     * 获取能力变更历史
     */
    public List<CapabilityChangeRecord> getChangeHistory() {
        return new ArrayList<>(changeHistory);
    }

    /**
     * 获取能力变更历史（按角色过滤）
     */
    public List<CapabilityChangeRecord> getChangeHistoryByRole(String agentRole) {
        return changeHistory.stream()
            .filter(record -> agentRole.equals(record.getAgentRole()))
            .collect(Collectors.toList());
    }

    /**
     * 能力变更记录
     */
    public static class CapabilityChangeRecord {
        private LocalDateTime timestamp;
        private String action;
        private String description;
        private String capabilityName;
        private String agentRole;
        private String projectId;

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCapabilityName() { return capabilityName; }
        public void setCapabilityName(String capabilityName) { this.capabilityName = capabilityName; }
        public String getAgentRole() { return agentRole; }
        public void setAgentRole(String agentRole) { this.agentRole = agentRole; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
    }

    // ===== 验证结果内部类 =====

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyList());
        }

        public static ValidationResult error(String... errors) {
            return new ValidationResult(false, Arrays.asList(errors));
        }

        public static ValidationResult error(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public String getErrorMessage() { return String.join("; ", errors); }
    }
}
