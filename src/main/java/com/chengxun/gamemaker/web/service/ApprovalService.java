package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.service.ApprovalCallbackService;
import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.ApprovalRequestRepository;
import com.chengxun.gamemaker.web.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批服务
 * 管理制作人 Agent 的敏感操作审批流程
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRequestRepository approvalRepository;
    private final AgentManager agentManager;
    private final NotificationService notificationService;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private BusinessMetricsService metricsService;

    @Autowired(required = false)
    private ApprovalCallbackService approvalCallbackService;

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ApiTokenService apiTokenService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.SystemConfigService systemConfigService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.manager.ProjectManager projectManager;

    public ApprovalService(ApprovalRequestRepository approvalRepository,
                          AgentManager agentManager,
                          NotificationService notificationService,
                          AppConfig appConfig, ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.agentManager = agentManager;
        this.notificationService = notificationService;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMetricsService(BusinessMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * 创建审批请求
     *
     * @param projectId   项目 ID
     * @param requesterId 请求者 ID（制作人 Agent）
     * @param requestType 请求类型
     * @param requestData 请求数据（JSON）
     * @param description 请求描述
     * @return 创建的审批请求
     */
    public ApprovalRequest createRequest(String projectId, String requesterId, String requestType,
                                         String requestData, String description) {
        ApprovalRequest request = new ApprovalRequest();
        request.setProjectId(projectId);
        request.setRequesterId(requesterId);
        request.setRequestType(requestType);
        request.setRequestData(requestData);
        request.setDescription(description);
        request.setStatus(ApprovalRequest.ApprovalStatus.PENDING);

        // 设置发起者名称
        String requesterName = resolveRequesterName(requesterId);
        request.setRequesterName(requesterName);

        // 设置优先级（根据请求类型）
        request.setPriority(getPriorityByType(requestType));

        ApprovalRequest saved = approvalRepository.save(request);

        // 通知管理员有新的审批请求（独立事务，避免通知失败导致审批创建回滚）
        try {
            notifyAdminsInNewTransaction(saved);
        } catch (Exception e) {
            log.warn("通知管理员失败（不影响审批创建）: {}", e.getMessage());
        }

        log.info("Approval request created: {} for project {} by {} ({})",
            requestType, projectId, requesterId, requesterName);

        return saved;
    }

    /**
     * 解析发起者名称
     * 根据requesterId判断是用户还是Agent，返回对应的名称
     */
    private String resolveRequesterName(String requesterId) {
        if (requesterId == null || requesterId.isEmpty()) {
            return "未知";
        }

        // 如果是Agent ID（格式：projectId:role）
        if (requesterId.contains(":")) {
            try {
                Agent agent = agentManager.getAgent(requesterId);
                if (agent != null) {
                    return agent.getName() != null ? agent.getName() : requesterId;
                }
            } catch (Exception e) {
                log.warn("获取Agent名称失败: {}", e.getMessage());
            }
            return requesterId;
        }

        // 如果是用户ID（数字）
        try {
            Long userId = Long.parseLong(requesterId);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return user.getNickname() != null ? user.getNickname() : user.getUsername();
            }
        } catch (NumberFormatException e) {
            // 不是数字ID，可能是用户名
        }

        return requesterId;
    }

    /**
     * 审批请求
     *
     * @param requestId 审批请求 ID
     * @param approverId 审批者 ID
     * @param approverName 审批者名称
     * @param approved 是否批准
     * @param comment 审批意见
     * @return 更新后的审批请求
     */
    public ApprovalRequest approve(Long requestId, String approverId, String approverName,
                                   boolean approved, String comment) {
        ApprovalRequest request = approvalRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("审批请求不存在"));

        if (!request.isPending()) {
            throw new RuntimeException("该请求已被处理");
        }

        if (request.isExpired()) {
            request.setStatus(ApprovalRequest.ApprovalStatus.EXPIRED);
            approvalRepository.save(request);
            throw new RuntimeException("该请求已过期");
        }

        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApprovalComment(comment);
        request.setApprovedAt(LocalDateTime.now());

        if (approved) {
            request.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
            // 执行审批通过的操作
            executeApprovedRequest(request);
        } else {
            request.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        }

        // 通知 Producer 审批结果（通过 ApprovalCallbackService）
        if (approvalCallbackService != null) {
            try {
                String requestData = request.getRequestData();
                String approvalType = "CREATE_AGENT".equals(request.getRequestType()) ? "CREATE_AGENT" : request.getRequestType();
                approvalCallbackService.registerApprovalRequest(
                    request.getId().toString(), request.getRequesterId(), approvalType,
                    requestData != null ? requestData : "");
                approvalCallbackService.onApprovalCompleted(request.getId().toString(), approved, comment);
            } catch (Exception e) {
                log.warn("审批回调通知失败: {}", e.getMessage());
            }
        }

        ApprovalRequest saved = approvalRepository.save(request);

        // 通知请求者审批结果
        notifyRequester(saved);

        log.info("Approval request {} {} by {}", requestId,
            approved ? "approved" : "rejected", approverName);

        // 记录审批指标
        metricsService.recordApproval();

        return saved;
    }

    /**
     * 获取指定项目的待审批请求
     *
     * @param projectId 项目ID
     * @return 待审批请求列表
     */
    public List<ApprovalRequest> getPendingRequestsByProject(String projectId) {
        return approvalRepository.findByProjectIdAndStatusOrderByPriorityAscCreatedAtAsc(
            projectId, ApprovalRequest.ApprovalStatus.PENDING);
    }

    /**
     * 获取项目的所有审批请求（不限状态）
     */
    public List<ApprovalRequest> getRequestsByProject(String projectId) {
        return approvalRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 执行审批通过的操作
     */
    private void executeApprovedRequest(ApprovalRequest request) {
        String type = request.getRequestType();
        String data = request.getRequestData();

        try {
            switch (type) {
                case "CREATE_AGENT" -> executeCreateAgent(request);
                case "ASSIGN_API" -> executeAssignApi(request);
                case "DELETE_AGENT", "DISMISS_AGENT" -> executeDeleteAgent(request);
                case "CHANGE_CONFIG" -> executeChangeConfig(request);
                case "EMAIL_CHANGE" -> executeEmailChange(request);
                case "STRATEGIC_DECISION" -> executeStrategicDecisionApproved(request);
                case "DELIVERY" -> executeDeliveryApproved(request);
                default -> log.warn("Unknown request type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to execute approved request: {}", request.getId(), e);
            // 可以添加重试逻辑或通知管理员
        }
    }

    /**
     * 执行创建 Agent 操作
     * 解析审批请求中的 JSON 数据，创建 Agent 实例
     */
    @SuppressWarnings("unchecked")
    private void executeCreateAgent(ApprovalRequest request) {
        try {
            Map<String, String> data = objectMapper.readValue(
                request.getRequestData(), new TypeReference<Map<String, String>>() {});

            String role = data.getOrDefault("role", "system-planner");
            String name = data.getOrDefault("name", role);
            String workDir = data.getOrDefault("workDir", "");
            String projectId = request.getProjectId();

            if (workDir.isEmpty() || projectId == null) {
                log.warn("创建Agent缺少必要参数: workDir={}, projectId={}", workDir, projectId);
                return;
            }

            AgentDefinition.Builder builder = AgentDefinition.builder()
                .id(role)
                .name(name)
                .role(role)
                .description("由制作人招聘，经管理员审批通过")
                .workDir(workDir)
                .projectId(projectId);
            // 不预设 API Key，让 AgentManager.autoAssignToken() 自动分配 Token
            // 如果没有可用 Token，ClaudeCliEngine 会 fallback 到全局配置

            if ("producer".equals(role)) {
                builder.parent(true);
            }

            agentManager.createAgent(builder.build());
            log.info("审批通过，Agent 已创建: {} ({}) for project: {}", name, role, projectId);
        } catch (Exception e) {
            log.error("创建 Agent 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行分配 API Token 操作
     * 解析审批请求中的 JSON 数据，将 Token 绑定到指定 Agent
     */
    @SuppressWarnings("unchecked")
    private void executeAssignApi(ApprovalRequest request) {
        try {
            Map<String, String> data = objectMapper.readValue(
                request.getRequestData(), new TypeReference<Map<String, String>>() {});

            String tokenIdStr = data.get("tokenId");
            String agentId = data.get("agentId");
            String activation = data.getOrDefault("activation", "immediate");

            if (tokenIdStr == null || agentId == null) {
                log.warn("分配API缺少必要参数: tokenId={}, agentId={}", tokenIdStr, agentId);
                return;
            }

            if (apiTokenService == null) {
                log.warn("ApiTokenService 不可用，无法执行 Token 分配");
                return;
            }

            Long tokenId = Long.parseLong(tokenIdStr);
            apiTokenService.assignToken(tokenId, agentId, activation);
            log.info("审批通过，Token 已分配: tokenId={} -> agentId={} ({} activation)", tokenId, agentId, activation);
        } catch (Exception e) {
            log.error("分配 API Token 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行删除 Agent 操作
     */
    @SuppressWarnings("unchecked")
    private void executeDeleteAgent(ApprovalRequest request) {
        try {
            Map<String, String> data = objectMapper.readValue(
                request.getRequestData(), new TypeReference<Map<String, String>>() {});
            String agentId = data.get("agentId");
            if (agentId != null) {
                agentManager.removeAgent(agentId);
                log.info("审批通过，Agent 已移除: {}", agentId);
            }
        } catch (Exception e) {
            log.error("删除 Agent 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行修改系统配置操作
     * 解析审批请求中的 JSON 数据，更新系统配置项
     */
    @SuppressWarnings("unchecked")
    private void executeChangeConfig(ApprovalRequest request) {
        try {
            Map<String, String> data = objectMapper.readValue(
                request.getRequestData(), new TypeReference<Map<String, String>>() {});

            String configKey = data.get("configKey");
            String configValue = data.get("configValue");

            if (configKey == null || configValue == null) {
                log.warn("修改配置缺少必要参数: configKey={}, configValue={}", configKey, configValue);
                return;
            }

            if (systemConfigService == null) {
                log.warn("SystemConfigService 不可用，无法执行配置修改");
                return;
            }

            systemConfigService.setConfig(configKey, configValue);
            log.info("审批通过，系统配置已更新: {} = {}", configKey, configValue);
        } catch (Exception e) {
            log.error("修改系统配置失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行邮箱变更操作
     * 审批通过后自动更新用户邮箱
     */
    @SuppressWarnings("unchecked")
    private void executeEmailChange(ApprovalRequest request) {
        try {
            Map<String, String> data = objectMapper.readValue(
                request.getRequestData(), new TypeReference<Map<String, String>>() {});

            String userIdStr = data.get("userId");
            String newEmail = data.get("newEmail");

            if (userIdStr == null || newEmail == null) {
                log.warn("邮箱变更缺少必要参数: userId={}, newEmail={}", userIdStr, newEmail);
                return;
            }

            Long userId = Long.parseLong(userIdStr);

            // 使用直接更新避免懒加载问题
            userRepository.updateEmailById(userId, newEmail, LocalDateTime.now());

            // 清除用户缓存，确保下次查询返回最新数据
            if (cacheManager != null) {
                org.springframework.cache.Cache usersCache = cacheManager.getCache("users");
                if (usersCache != null) {
                    usersCache.clear();
                }
            }

            log.info("审批通过，邮箱已更新: userId={}, newEmail={}", userId, newEmail);

        } catch (Exception e) {
            log.error("邮箱变更执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行战略决策审批通过的操作
     * 恢复制作人 Agent 的工作，通知其决策已获批准
     */
    private void executeStrategicDecisionApproved(ApprovalRequest request) {
        try {
            String requesterId = request.getRequesterId();
            String projectId = request.getProjectId();

            log.info("战略决策审批通过: requestId={}, requesterId={}, projectId={}",
                request.getId(), requesterId, projectId);

            // 通知制作人决策已获批准，恢复其工作
            // 通过 AgentManager 找到对应的制作人并恢复
            if (agentManager != null) {
                Agent agent = agentManager.getAgent(requesterId);
                if (agent instanceof com.chengxun.gamemaker.agent.ProducerAgent producer) {
                    // 使用 onApprovalCompleted 方法恢复制作人工作
                    producer.onApprovalCompleted(request.getId().toString(), true,
                        "战略决策已获老板批准: " + request.getDescription());
                    log.info("已恢复制作人工作: agentId={}", requesterId);
                } else {
                    log.warn("未找到对应的制作人Agent: {}", requesterId);
                }
            }

            // 发送通知
            sendApprovalResultNotification(request, true, "战略决策已批准");

        } catch (Exception e) {
            log.error("战略决策审批执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行项目交付审批通过的操作
     * 标记项目为已交付状态，通知相关 Agent
     */
    private void executeDeliveryApproved(ApprovalRequest request) {
        try {
            String projectId = request.getProjectId();

            log.info("项目交付审批通过: requestId={}, projectId={}", request.getId(), projectId);

            // 更新项目状态为已交付
            if (projectId != null && projectManager != null) {
                var project = projectManager.getProject(projectId);
                if (project != null) {
                    project.setStatus(com.chengxun.gamemaker.model.GameProject.ProjectStatus.DELIVERED);
                    project.setDeliveredAt(LocalDateTime.now());
                    projectManager.saveProjectConfig(project);
                    log.info("项目已标记为交付完成: projectId={}", projectId);
                }
            }

            // 通知制作人项目已交付
            String requesterId = request.getRequesterId();
            if (agentManager != null) {
                Agent agent = agentManager.getAgent(requesterId);
                if (agent instanceof com.chengxun.gamemaker.agent.ProducerAgent producer) {
                    // 使用 onApprovalCompleted 方法通知制作人
                    producer.onApprovalCompleted(request.getId().toString(), true,
                        "项目交付已获老板批准，项目正式交付完成！");
                    log.info("已通知制作人项目交付完成: agentId={}", requesterId);
                }
            }

            // 发送通知
            sendApprovalResultNotification(request, true, "项目交付已批准");

        } catch (Exception e) {
            log.error("项目交付审批执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送审批结果通知
     */
    private void sendApprovalResultNotification(ApprovalRequest request, boolean approved, String message) {
        try {
            if (notificationService != null && userService != null) {
                String title = approved ? "审批通过" : "审批拒绝";
                String content = String.format("审批类型: %s\n结果: %s\n%s",
                    getRequestTypeDesc(request.getRequestType()),
                    approved ? "通过" : "拒绝",
                    message);

                // 查找项目管理员用户发送通知
                // 使用模板系统发送通知
                Map<String, String> variables = Map.of(
                    "approvalType", getRequestTypeDesc(request.getRequestType()),
                    "result", approved ? "通过" : "拒绝",
                    "message", message,
                    "requestId", request.getId().toString()
                );

                // 尝试通过模板发送，如果模板不存在则直接发送
                try {
                    notificationService.sendNotificationByTemplate(
                        null, // 发送给所有管理员
                        "APPROVAL_RESULT",
                        variables,
                        com.chengxun.gamemaker.web.entity.Notification.NotificationType.SYSTEM
                    );
                } catch (Exception templateEx) {
                    log.debug("审批结果通知模板不存在，跳过发送: {}", templateEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("发送审批结果通知失败: {}", e.getMessage());
        }
    }

    /**
     * 获取项目的待审批请求
     */
    public List<ApprovalRequest> getPendingRequests(String projectId) {
        return approvalRepository.findByProjectIdAndStatusOrderByPriorityAscCreatedAtAsc(
            projectId, ApprovalRequest.ApprovalStatus.PENDING);
    }

    /**
     * 获取所有待审批请求
     */
    public List<ApprovalRequest> getAllPendingRequests() {
        return approvalRepository.findByStatusOrderByPriorityAscCreatedAtAsc(
            ApprovalRequest.ApprovalStatus.PENDING);
    }

    /**
     * 获取所有审批请求（含已处理的）
     */
    public List<ApprovalRequest> getAllRequests() {
        return approvalRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 获取请求者的所有请求
     */
    public List<ApprovalRequest> getRequestsByRequester(String requesterId) {
        return approvalRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    /**
     * 获取待审批请求数量
     */
    public long getPendingCount() {
        return approvalRepository.countByStatus(ApprovalRequest.ApprovalStatus.PENDING);
    }

    /**
     * 获取项目的待审批请求数量
     */
    public long getPendingCountByProject(String projectId) {
        return approvalRepository.countByProjectIdAndStatus(projectId, ApprovalRequest.ApprovalStatus.PENDING);
    }

    /**
     * 根据请求类型获取优先级
     */
    private int getPriorityByType(String requestType) {
        return switch (requestType) {
            case "DELETE_AGENT", "DISMISS_AGENT" -> 1;  // 最高优先级
            case "STRATEGIC_DECISION" -> 2;  // 战略决策 - 高优先级
            case "DELIVERY" -> 2;  // 项目交付 - 高优先级
            case "ASSIGN_API" -> 3;
            case "CREATE_AGENT" -> 5;
            case "EMAIL_CHANGE" -> 6;
            case "CHANGE_CONFIG" -> 7;
            default -> 10;
        };
    }

    /**
     * 获取审批类型的中文描述
     */
    private String getRequestTypeDesc(String requestType) {
        if (requestType == null) return "未知";
        return switch (requestType) {
            case "CREATE_AGENT" -> "招聘 Agent";
            case "DELETE_AGENT" -> "删除 Agent";
            case "DISMISS_AGENT" -> "解雇 Agent";
            case "ASSIGN_API" -> "分配 API Token";
            case "CHANGE_CONFIG" -> "修改配置";
            case "EMAIL_CHANGE" -> "邮箱变更";
            case "STRATEGIC_DECISION" -> "重大决策";
            case "DELIVERY" -> "项目交付";
            default -> requestType;
        };
    }

    /**
     * 通知管理员有新的审批请求
     * 使用模板系统发送多渠道通知
     * 注意：在独立事务中执行，避免通知失败导致审批创建回滚
     */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void notifyAdminsInNewTransaction(ApprovalRequest request) {
        notifyAdmins(request);
    }

    /**
     * 通知管理员有新的审批请求
     * 使用模板系统发送多渠道通知
     */
    private void notifyAdmins(ApprovalRequest request) {
        try {
            String requesterName = request.getRequesterId();
            // 尝试获取请求者名称
            if (userService != null) {
                try {
                    Long userId = Long.parseLong(request.getRequesterId());
                    var user = userService.getUserById(userId);
                    if (user != null) {
                        requesterName = user.getNickname() != null ? user.getNickname() : user.getUsername();
                    }
                } catch (NumberFormatException e) {
                    // Agent 请求，使用 Agent ID
                    Agent agent = agentManager.getAgent(request.getRequesterId());
                    if (agent != null) {
                        requesterName = agent.getName();
                    }
                }
            }

            java.util.Map<String, String> variables = java.util.Map.of(
                "requestType", request.getRequestType() != null ? request.getRequestType() : "",
                "requestTypeDesc", getRequestTypeDesc(request.getRequestType()),
                "requesterName", requesterName != null ? requesterName : request.getRequesterId(),
                "description", request.getDescription() != null ? request.getDescription() : "",
                "projectName", request.getProjectId() != null ? request.getProjectId() : "",
                "title", "新的审批请求"
            );

            // 使用模板通知所有管理员
            if (notificationService != null) {
                notificationService.notifyAdmins("approval", "APPROVAL_NEW",
                    variables, com.chengxun.gamemaker.web.entity.Notification.NotificationType.APPROVAL, "/approvals");
            }

            log.info("Admin notification sent for approval: {} - {}", request.getRequestType(), request.getDescription());
        } catch (Exception e) {
            log.warn("Failed to notify admins: {}", e.getMessage());
        }
    }

    /**
     * 通知请求者审批结果
     * 通过 ApprovalCallbackService 回调 Producer，并发送模板通知
     */
    private void notifyRequester(ApprovalRequest request) {
        try {
            // 1. 发送模板通知给请求者
            if (notificationService != null) {
                String templateCode = request.isApproved() ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
                String approverName = request.getApproverName() != null ? request.getApproverName() : "系统";

                java.util.Map<String, String> variables = java.util.Map.of(
                    "requestType", request.getRequestType() != null ? request.getRequestType() : "",
                    "requestTypeDesc", getRequestTypeDesc(request.getRequestType()),
                    "description", request.getDescription() != null ? request.getDescription() : "",
                    "approverName", approverName,
                    "approvalComment", request.getApprovalComment() != null ? request.getApprovalComment() : "无",
                    "title", request.isApproved() ? "审批通过" : "审批拒绝"
                );

                com.chengxun.gamemaker.web.entity.Notification.NotificationType type =
                    request.isApproved()
                        ? com.chengxun.gamemaker.web.entity.Notification.NotificationType.SUCCESS
                        : com.chengxun.gamemaker.web.entity.Notification.NotificationType.WARNING;

                // 尝试发送给用户（如果 requesterId 是数字）
                try {
                    Long userId = Long.parseLong(request.getRequesterId());
                    String link = "/approvals";
                    notificationService.notifyUser(userId, "approval", templateCode, variables, type, link);
                } catch (NumberFormatException e) {
                    // requesterId 是 Agent ID，通过管理员通知
                    log.debug("Requester is Agent {}, skipping user notification", request.getRequesterId());
                }
            }

            // 2. 使用 ApprovalCallbackService 回调 Agent
            if (approvalCallbackService != null) {
                String approvalId = request.getRequestType() + "-" + request.getId();
                approvalCallbackService.onApprovalCompleted(
                    approvalId,
                    request.isApproved(),
                    request.getApprovalComment() != null ? request.getApprovalComment() : ""
                );
                log.info("已通过 ApprovalCallbackService 通知请求者: {}", request.getRequesterId());
                return;
            }

            // 3. 回退：直接通知 Agent
            Agent requester = agentManager.getAgent(request.getRequesterId());
            if (requester != null) {
                String approvalId = request.getRequestType() + "-" + request.getId();

                // 如果是 Producer，调用通用审批回调
                if (requester instanceof ProducerAgent producer) {
                    producer.onApprovalCompleted(
                        approvalId,
                        request.isApproved(),
                        request.getApprovalComment() != null ? request.getApprovalComment() : ""
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify requester: {}", e.getMessage());
        }
    }

    /**
     * 定时任务：处理过期的审批请求
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void expireOldRequests() {
        int expired = approvalRepository.expirePendingRequests(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} old approval requests", expired);
        }
    }
}
