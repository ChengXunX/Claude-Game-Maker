package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.PermissionDefinition;
import com.chengxun.gamemaker.web.entity.PermissionRequest;
import com.chengxun.gamemaker.web.entity.PermissionRequest.RequestStatus;
import com.chengxun.gamemaker.web.entity.UserPermission;
import com.chengxun.gamemaker.web.entity.UserPermission.PermissionSource;
import com.chengxun.gamemaker.web.repository.PermissionDefinitionRepository;
import com.chengxun.gamemaker.web.repository.PermissionRequestRepository;
import com.chengxun.gamemaker.web.repository.UserPermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务
 * 管理用户权限的授予、申请和审批
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final UserPermissionRepository permissionRepository;
    private final PermissionRequestRepository requestRepository;
    private final PermissionDefinitionRepository definitionRepository;

    public PermissionService(UserPermissionRepository permissionRepository,
                              PermissionRequestRepository requestRepository,
                              PermissionDefinitionRepository definitionRepository) {
        this.permissionRepository = permissionRepository;
        this.requestRepository = requestRepository;
        this.definitionRepository = definitionRepository;
    }

    // ===== 初始化 =====

    /**
     * 应用启动时初始化默认权限定义
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultPermissions() {
        if (definitionRepository.count() > 0) {
            log.info("Permission definitions already initialized ({} records)", definitionRepository.count());
            return;
        }

        log.info("Initializing default permission definitions...");

        // Agent 管理相关
        saveDefinition("PERM_agents:manage", "Agent 管理", "创建、删除、配置 Agent", "Agent", true, 1);
        saveDefinition("PERM_agents:view", "Agent 查看", "查看 Agent 状态", "Agent", true, 2);
        saveDefinition("PERM_agents:task", "Agent 任务", "向 Agent 发送任务", "Agent", true, 3);

        // 技能相关
        saveDefinition("PERM_skills:view", "技能查看", "查看技能列表", "技能", true, 1);
        saveDefinition("PERM_skills:manage", "技能管理", "创建、编辑、删除技能", "技能", true, 2);

        // Token 相关
        saveDefinition("PERM_tokens:view", "Token 查看", "查看 Token 列表", "Token", true, 1);
        saveDefinition("PERM_tokens:manage", "Token 管理", "创建、分配 Token", "Token", true, 2);

        // 项目相关
        saveDefinition("PERM_projects:view", "项目查看", "查看项目列表", "项目", true, 1);
        saveDefinition("PERM_projects:manage", "项目管理", "创建、删除项目", "项目", true, 2);

        // 系统相关
        saveDefinition("PERM_system:monitor", "系统监控", "查看系统状态、上下文健康", "系统", true, 1);
        saveDefinition("PERM_system:manage", "系统管理", "配置系统参数", "系统", true, 2);

        // AI助手相关
        saveDefinition("PERM_ai:use", "AI助手使用", "使用AI助手问答功能", "AI", true, 1);
        saveDefinition("PERM_ai:admin", "AI助手管理", "管理AI知识库", "AI", true, 2);

        // 审批相关
        saveDefinition("PERM_approval:view", "审批查看", "查看审批请求", "审批", true, 1);
        saveDefinition("PERM_approval:manage", "审批管理", "批准、拒绝审批请求", "审批", true, 2);

        // 通知相关
        saveDefinition("PERM_notification:view", "通知查看", "查看通知", "通知", true, 1);
        saveDefinition("PERM_notification:manage", "通知管理", "管理通知模板", "通知", true, 2);

        // 管理员
        saveDefinition("PERM_admin:manage", "管理员权限", "所有功能", "管理", true, 1);

        // 角色管理
        saveDefinition("PERM_roles:manage", "角色管理", "创建、编辑角色", "管理", true, 2);

        // 用户管理
        saveDefinition("PERM_users:manage", "用户管理", "创建、编辑、删除用户", "管理", true, 3);

        // 日志查看
        saveDefinition("PERM_logs:view", "日志查看", "查看操作日志", "管理", true, 4);

        // 代码审查
        saveDefinition("PERM_code:review", "代码审查", "审查代码提交", "项目", true, 3);

        // 流水线相关
        saveDefinition("PERM_pipeline:view", "流水线查看", "查看CICD流水线", "项目", true, 4);
        saveDefinition("PERM_pipeline:manage", "流水线管理", "管理CICD流水线", "项目", true, 5);
        saveDefinition("PERM_pipeline:execute", "流水线执行", "执行CICD流水线", "项目", true, 6);
        saveDefinition("PERM_pipeline:approve", "流水线审批", "审批CICD流水线", "项目", true, 7);
        saveDefinition("PERM_pipeline:intervene", "流水线干预", "干预CICD流水线", "项目", true, 8);

        // 工作流相关
        saveDefinition("PERM_workflow:view", "工作流查看", "查看工作流", "项目", true, 9);
        saveDefinition("PERM_workflow:manage", "工作流管理", "管理工作流", "项目", true, 10);

        // 仪表盘查看
        saveDefinition("PERM_dashboard:view", "仪表盘查看", "查看仪表盘", "工作台", true, 1);

        // 终端使用
        saveDefinition("PERM_terminal:use", "终端使用", "使用系统终端", "系统", true, 3);

        // 系统配置
        saveDefinition("PERM_system:view", "系统查看", "查看系统配置", "系统", true, 4);
        saveDefinition("PERM_system:config", "系统配置", "查看系统配置", "系统", true, 5);
        saveDefinition("PERM_system:config:manage", "系统配置管理", "管理系统配置", "系统", true, 6);
        saveDefinition("PERM_system:monitor:manage", "系统监控管理", "管理系统监控", "系统", true, 7);

        // 知识库管理
        saveDefinition("PERM_knowledge:manage", "知识库管理", "管理知识库", "项目", true, 11);

        // 日志管理
        saveDefinition("PERM_log:view", "日志查看", "查看日志", "管理", true, 5);

        // Agent管理（兼容旧名称）
        saveDefinition("PERM_agent:view", "Agent查看", "查看Agent状态", "Agent", true, 4);
        saveDefinition("PERM_agent:manage", "Agent管理", "管理Agent", "Agent", true, 5);

        log.info("Default permission definitions initialized: 38");
    }

    private void saveDefinition(String key, String name, String description,
                                 String category, boolean system, int sortOrder) {
        PermissionDefinition def = new PermissionDefinition();
        def.setPermissionKey(key);
        def.setName(name);
        def.setDescription(description);
        def.setCategory(category);
        def.setSystem(system);
        def.setSortOrder(sortOrder);
        def.setEnabled(true);
        definitionRepository.save(def);
    }

    // ===== 权限定义管理 =====

    /**
     * 获取所有启用的权限定义
     */
    public Map<String, String> getAvailablePermissions() {
        return definitionRepository.findByEnabledTrueOrderByCategoryAscSortOrderAsc().stream()
            .collect(Collectors.toMap(
                PermissionDefinition::getPermissionKey,
                d -> d.getName() + "（" + d.getDescription() + "）",
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    /**
     * 获取所有权限定义（管理界面用）
     */
    public List<PermissionDefinition> getAllDefinitions() {
        return definitionRepository.findAllByOrderByCategoryAscSortOrderAsc();
    }

    /**
     * 获取所有分类
     */
    public List<String> getCategories() {
        return definitionRepository.findDistinctCategories();
    }

    /**
     * 保存权限定义
     */
    public PermissionDefinition saveDefinition(PermissionDefinition definition) {
        return definitionRepository.save(definition);
    }

    /**
     * 切换权限启用状态
     */
    public PermissionDefinition toggleDefinition(Long id) {
        PermissionDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("权限定义不存在"));
        def.setEnabled(!def.isEnabled());
        return definitionRepository.save(def);
    }

    /**
     * 删除权限定义（系统内置不可删除）
     */
    public void deleteDefinition(Long id) {
        PermissionDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("权限定义不存在"));
        if (def.isSystem()) {
            throw new RuntimeException("系统内置权限不可删除");
        }
        definitionRepository.deleteById(id);
    }

    // ===== 权限查询 =====

    /**
     * 获取用户的所有有效额外权限
     */
    public List<UserPermission> getUserPermissions(Long userId) {
        return permissionRepository.findValidByUserId(userId);
    }

    /**
     * 获取用户的所有权限标识
     */
    public Set<String> getUserExtraPermissionIds(Long userId) {
        return permissionRepository.findValidByUserId(userId).stream()
            .map(UserPermission::getPermission)
            .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否拥有某权限
     */
    public boolean hasPermission(Long userId, String permission) {
        return permissionRepository.existsByUserIdAndPermission(userId, permission);
    }

    // ===== 权限授予 =====

    /**
     * 管理员直接授予用户权限
     */
    public UserPermission grantPermission(Long userId, String permission, String grantedBy, String reason) {
        Optional<UserPermission> existing = permissionRepository.findByUserIdAndPermission(userId, permission);
        if (existing.isPresent()) {
            UserPermission perm = existing.get();
            if (perm.isValid()) {
                log.info("User {} already has permission {}", userId, permission);
                return perm;
            }
            perm.setEnabled(true);
            perm.setGrantedBy(grantedBy);
            perm.setReason(reason);
            perm.setExpiresAt(null);
            return permissionRepository.save(perm);
        }

        UserPermission perm = new UserPermission();
        perm.setUserId(userId);
        perm.setPermission(permission);
        perm.setSource(PermissionSource.GRANTED);
        perm.setGrantedBy(grantedBy);
        perm.setReason(reason);

        UserPermission saved = permissionRepository.save(perm);
        log.info("Permission {} granted to user {} by {}", permission, userId, grantedBy);
        return saved;
    }

    /**
     * 管理员撤销用户权限
     */
    public void revokePermission(Long userId, String permission) {
        permissionRepository.deleteByUserIdAndPermission(userId, permission);
        log.info("Permission {} revoked from user {}", permission, userId);
    }

    /**
     * 管理员批量授予权限
     */
    public List<UserPermission> grantPermissions(Long userId, List<String> permissions, String grantedBy, String reason) {
        return permissions.stream()
            .map(p -> grantPermission(userId, p, grantedBy, reason))
            .collect(Collectors.toList());
    }

    // ===== 权限申请 =====

    /**
     * 用户提交权限申请
     */
    public PermissionRequest submitRequest(Long userId, String username, String permission, String reason) {
        // 校验权限标识是否存在且启用
        PermissionDefinition def = definitionRepository.findByPermissionKey(permission)
            .orElseThrow(() -> new RuntimeException("权限标识不存在: " + permission));
        if (!def.isEnabled()) {
            throw new RuntimeException("该权限已被禁用，无法申请");
        }

        if (requestRepository.existsByUserIdAndPermissionAndStatus(userId, permission, RequestStatus.PENDING)) {
            throw new RuntimeException("您已提交过该权限的申请，请等待审批");
        }

        if (hasPermission(userId, permission)) {
            throw new RuntimeException("您已拥有该权限");
        }

        PermissionRequest request = new PermissionRequest();
        request.setUserId(userId);
        request.setUsername(username);
        request.setPermission(permission);
        request.setReason(reason);
        request.setStatus(RequestStatus.PENDING);

        PermissionRequest saved = requestRepository.save(request);
        log.info("Permission request submitted: user={}, permission={}", username, permission);
        return saved;
    }

    /**
     * 用户取消申请
     */
    public void cancelRequest(Long requestId, Long userId) {
        PermissionRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此申请");
        }

        if (!request.isPending()) {
            throw new RuntimeException("该申请已被处理");
        }

        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
    }

    // ===== 权限审批 =====

    public List<PermissionRequest> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }

    public Page<PermissionRequest> getPendingRequests(Pageable pageable) {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING, pageable);
    }

    public Page<PermissionRequest> getUserRequests(Long userId, Pageable pageable) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getPendingCount() {
        return requestRepository.countByStatus(RequestStatus.PENDING);
    }

    public PermissionRequest approve(Long requestId, Long approverId, String approverName, String comment) {
        PermissionRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!request.isPending()) {
            throw new RuntimeException("该申请已被处理");
        }

        // 检查权限定义是否仍启用
        PermissionDefinition def = definitionRepository.findByPermissionKey(request.getPermission()).orElse(null);
        if (def == null || !def.isEnabled()) {
            throw new RuntimeException("该权限已被禁用或删除，无法授予");
        }

        grantPermission(request.getUserId(), request.getPermission(),
            approverName, "申请审批通过: " + request.getReason());

        request.setStatus(RequestStatus.APPROVED);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApprovalComment(comment);
        request.setApprovedAt(LocalDateTime.now());

        PermissionRequest saved = requestRepository.save(request);
        log.info("Permission request approved: id={}, user={}, permission={}",
            requestId, request.getUsername(), request.getPermission());
        return saved;
    }

    public PermissionRequest reject(Long requestId, Long approverId, String approverName, String comment) {
        PermissionRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!request.isPending()) {
            throw new RuntimeException("该申请已被处理");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApprovalComment(comment);
        request.setApprovedAt(LocalDateTime.now());

        PermissionRequest saved = requestRepository.save(request);
        log.info("Permission request rejected: id={}, user={}, permission={}, reason={}",
            requestId, request.getUsername(), request.getPermission(), comment);
        return saved;
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredPermissions() {
        int disabled = permissionRepository.disableExpiredPermissions();
        if (disabled > 0) {
            log.info("Disabled {} expired permissions", disabled);
        }
    }
}
