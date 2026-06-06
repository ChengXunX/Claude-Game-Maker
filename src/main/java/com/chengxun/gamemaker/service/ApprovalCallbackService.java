package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审批回调服务
 * 管理审批请求和回调，确保审批完成后能正确通知到发起者
 *
 * 主要功能：
 * - 注册审批请求（记录发起者信息）
 * - 审批完成后回调发起者
 * - 支持多种审批类型（招聘、创建Agent、工作流步骤等）
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class ApprovalCallbackService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalCallbackService.class);

    @Autowired
    private AgentManager agentManager;

    /**
     * 审批请求信息
     */
    public static class ApprovalRequest {
        private final String approvalId;
        private final String requesterId;
        private final String approvalType;
        private final String requestData;
        private final long createdAt;

        public ApprovalRequest(String approvalId, String requesterId, String approvalType, String requestData) {
            this.approvalId = approvalId;
            this.requesterId = requesterId;
            this.approvalType = approvalType;
            this.requestData = requestData;
            this.createdAt = System.currentTimeMillis();
        }

        public String getApprovalId() { return approvalId; }
        public String getRequesterId() { return requesterId; }
        public String getApprovalType() { return approvalType; }
        public String getRequestData() { return requestData; }
        public long getCreatedAt() { return createdAt; }
    }

    /** 待处理的审批请求缓存 */
    private final Map<String, ApprovalRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 注册审批请求
     *
     * @param approvalId 审批 ID
     * @param requesterId 发起者 ID（通常是 Producer 的 ID）
     * @param approvalType 审批类型
     * @param requestData 请求数据（JSON 格式）
     */
    public void registerApprovalRequest(String approvalId, String requesterId, String approvalType, String requestData) {
        ApprovalRequest request = new ApprovalRequest(approvalId, requesterId, approvalType, requestData);
        pendingRequests.put(approvalId, request);
        log.info("注册审批请求: id={}, type={}, requester={}", approvalId, approvalType, requesterId);
    }

    /**
     * 审批完成回调
     * 通知发起者审批结果
     *
     * @param approvalId 审批 ID
     * @param approved 是否通过
     * @param comment 审批意见
     */
    public void onApprovalCompleted(String approvalId, boolean approved, String comment) {
        ApprovalRequest request = pendingRequests.remove(approvalId);
        if (request == null) {
            log.warn("找不到审批请求: {}", approvalId);
            return;
        }

        log.info("审批完成: id={}, approved={}, type={}", approvalId, approved, request.getApprovalType());

        // 查找发起者 Agent
        Agent requester = agentManager.getAgent(request.getRequesterId());
        if (requester == null) {
            log.warn("找不到发起者 Agent: {}", request.getRequesterId());
            return;
        }

        // 只有 Producer 才处理审批回调
        if (!(requester instanceof ProducerAgent producer)) {
            log.warn("发起者不是 Producer: {}", request.getRequesterId());
            return;
        }

        // 根据审批类型分发回调
        String approvalType = request.getApprovalType();
        String requestData = request.getRequestData();

        switch (approvalType) {
            case "RECRUIT" -> {
                // 招聘审批回调
                String role = extractField(requestData, "role");
                producer.onRecruitApprovalCompleted(approvalId, approved, role, comment);
            }
            case "CREATE_AGENT" -> {
                // 创建 Agent 审批回调
                String name = extractField(requestData, "name");
                String role = extractField(requestData, "role");
                String workDir = extractField(requestData, "workDir");
                producer.onCreateAgentApprovalCompleted(approvalId, approved, name, role, null, workDir, comment);
            }
            case "WORKFLOW_STEP" -> {
                // 工作流步骤审批回调
                producer.onApprovalCompleted(approvalId, approved, comment);
            }
            default -> {
                // 通用审批回调
                producer.onApprovalCompleted(approvalId, approved, comment);
            }
        }
    }

    /**
     * 获取待处理的审批请求
     *
     * @param approvalId 审批 ID
     * @return 审批请求，不存在返回 null
     */
    public ApprovalRequest getPendingRequest(String approvalId) {
        return pendingRequests.get(approvalId);
    }

    /**
     * 获取所有待处理的审批请求
     *
     * @return 待处理请求数量
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * 检查审批是否超时
     * 超时的审批请求将被自动拒绝
     *
     * @param timeoutMillis 超时时间（毫秒）
     */
    public void checkTimeouts(long timeoutMillis) {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> {
            ApprovalRequest request = entry.getValue();
            if (now - request.getCreatedAt() > timeoutMillis) {
                log.warn("审批请求超时: id={}, type={}", request.getApprovalId(), request.getApprovalType());
                // 超时自动拒绝
                onApprovalCompleted(request.getApprovalId(), false, "审批超时，自动拒绝");
                return true;
            }
            return false;
        });
    }

    /**
     * 从 JSON 数据中提取字段值
     * 简单实现，只支持顶层字段
     *
     * @param json JSON 数据
     * @param field 字段名
     * @return 字段值，不存在返回空字符串
     */
    private String extractField(String json, String field) {
        if (json == null || json.isEmpty()) return "";

        // 简单的 JSON 字段提取
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";

        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";

        return json.substring(start, end);
    }
}
