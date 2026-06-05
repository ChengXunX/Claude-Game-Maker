package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import com.chengxun.gamemaker.web.repository.ApprovalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private BusinessMetricsService metricsService;

    public ApprovalService(ApprovalRequestRepository approvalRepository,
                          AgentManager agentManager,
                          NotificationService notificationService) {
        this.approvalRepository = approvalRepository;
        this.agentManager = agentManager;
        this.notificationService = notificationService;
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

        // 设置优先级（根据请求类型）
        request.setPriority(getPriorityByType(requestType));

        ApprovalRequest saved = approvalRepository.save(request);

        // 通知管理员有新的审批请求
        notifyAdmins(saved);

        log.info("Approval request created: {} for project {} by {}",
            requestType, projectId, requesterId);

        return saved;
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
     * 执行审批通过的操作
     */
    private void executeApprovedRequest(ApprovalRequest request) {
        String type = request.getRequestType();
        String data = request.getRequestData();

        try {
            switch (type) {
                case "CREATE_AGENT" -> executeCreateAgent(request);
                case "ASSIGN_API" -> executeAssignApi(request);
                case "DELETE_AGENT" -> executeDeleteAgent(request);
                case "CHANGE_CONFIG" -> executeChangeConfig(request);
                default -> log.warn("Unknown request type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to execute approved request: {}", request.getId(), e);
            // 可以添加重试逻辑或通知管理员
        }
    }

    /**
     * 执行创建 Agent 操作
     */
    private void executeCreateAgent(ApprovalRequest request) {
        // 解析请求数据并创建 Agent
        // 这里需要根据实际的数据格式来解析
        log.info("Executing create agent for request: {}", request.getId());
        // TODO: 实际实现
    }

    /**
     * 执行分配 API 操作
     */
    private void executeAssignApi(ApprovalRequest request) {
        log.info("Executing assign API for request: {}", request.getId());
        // TODO: 实际实现
    }

    /**
     * 执行删除 Agent 操作
     */
    private void executeDeleteAgent(ApprovalRequest request) {
        log.info("Executing delete agent for request: {}", request.getId());
        // TODO: 实际实现
    }

    /**
     * 执行修改配置操作
     */
    private void executeChangeConfig(ApprovalRequest request) {
        log.info("Executing change config for request: {}", request.getId());
        // TODO: 实际实现
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
            case "DELETE_AGENT" -> 1;      // 最高优先级
            case "ASSIGN_API" -> 3;
            case "CREATE_AGENT" -> 5;
            case "CHANGE_CONFIG" -> 7;
            default -> 10;
        };
    }

    /**
     * 通知管理员有新的审批请求
     */
    private void notifyAdmins(ApprovalRequest request) {
        try {
            String title = "新的审批请求";
            String content = String.format("类型: %s\n项目: %s\n描述: %s",
                request.getRequestType(), request.getProjectId(), request.getDescription());
            // 发送给所有管理员（使用系统通知）
            // 这里需要遍历管理员用户并发送通知
            log.info("Admin notification: {} - {}", title, content);
        } catch (Exception e) {
            log.warn("Failed to notify admins: {}", e.getMessage());
        }
    }

    /**
     * 通知请求者审批结果
     */
    private void notifyRequester(ApprovalRequest request) {
        try {
            Agent requester = agentManager.getAgent(request.getRequesterId());
            if (requester != null) {
                String message = String.format("您的审批请求已%s\n类型: %s\n审批者: %s\n意见: %s",
                    request.isApproved() ? "批准" : "拒绝",
                    request.getRequestType(),
                    request.getApproverName(),
                    request.getApprovalComment());
                // 发送消息给请求者
                // requester.receiveMessage(...);
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
