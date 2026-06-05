package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.WorkflowAuditLogEntity;
import com.chengxun.gamemaker.web.repository.WorkflowAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工作流审计日志服务
 * 记录工作流所有关键操作，用于问题追溯和合规审计
 *
 * 审计范围：
 * - 实例生命周期：创建、启动、暂停、恢复、完成、失败、取消
 * - 步骤执行：开始、完成、失败、重试、超时
 * - 审批流程：请求、批准、拒绝
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class WorkflowAuditService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAuditService.class);

    @Autowired
    private WorkflowAuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== 审计操作类型常量 =====

    /** 实例级操作 */
    public static final String ACTION_INSTANCE_CREATED = "INSTANCE_CREATED";
    public static final String ACTION_INSTANCE_STARTED = "INSTANCE_STARTED";
    public static final String ACTION_INSTANCE_PAUSED = "INSTANCE_PAUSED";
    public static final String ACTION_INSTANCE_RESUMED = "INSTANCE_RESUMED";
    public static final String ACTION_INSTANCE_COMPLETED = "INSTANCE_COMPLETED";
    public static final String ACTION_INSTANCE_FAILED = "INSTANCE_FAILED";
    public static final String ACTION_INSTANCE_CANCELLED = "INSTANCE_CANCELLED";

    /** 步骤级操作 */
    public static final String ACTION_STEP_STARTED = "STEP_STARTED";
    public static final String ACTION_STEP_COMPLETED = "STEP_COMPLETED";
    public static final String ACTION_STEP_FAILED = "STEP_FAILED";
    public static final String ACTION_STEP_RETRIED = "STEP_RETRIED";
    public static final String ACTION_STEP_TIMEOUT = "STEP_TIMEOUT";
    public static final String ACTION_STEP_SKIPPED = "STEP_SKIPPED";

    /** 审批级操作 */
    public static final String ACTION_APPROVAL_REQUESTED = "APPROVAL_REQUESTED";
    public static final String ACTION_APPROVAL_APPROVED = "APPROVAL_APPROVED";
    public static final String ACTION_APPROVAL_REJECTED = "APPROVAL_REJECTED";

    // ===== 操作者类型常量 =====

    public static final String ACTOR_SYSTEM = "SYSTEM";
    public static final String ACTOR_USER = "USER";
    public static final String ACTOR_AGENT = "AGENT";

    /**
     * 记录审计日志
     *
     * @param instanceId 实例ID
     * @param stepId 步骤ID（可为null，表示实例级操作）
     * @param action 操作类型
     * @param actorType 操作者类型
     * @param actorId 操作者ID
     * @param actorName 操作者名称
     * @param detail 详情数据
     */
    public void log(String instanceId, String stepId, String action,
                    String actorType, String actorId, String actorName,
                    Map<String, Object> detail) {
        try {
            WorkflowAuditLogEntity entity = new WorkflowAuditLogEntity();
            entity.setInstanceId(instanceId);
            entity.setStepId(stepId);
            entity.setAction(action);
            entity.setActorType(actorType);
            entity.setActorId(actorId);
            entity.setActorName(actorName);
            if (detail != null && !detail.isEmpty()) {
                entity.setDetailJson(objectMapper.writeValueAsString(detail));
            }
            auditLogRepository.save(entity);
            log.debug("审计日志记录 - 实例: {}, 操作: {}, 操作者: {}:{}", instanceId, action, actorType, actorId);
        } catch (Exception e) {
            log.error("记录审计日志失败 - 实例: {}, 操作: {}", instanceId, action, e);
        }
    }

    /**
     * 记录系统操作的审计日志
     */
    public void logSystem(String instanceId, String stepId, String action, Map<String, Object> detail) {
        log(instanceId, stepId, action, ACTOR_SYSTEM, "workflow-engine", "工作流引擎", detail);
    }

    /**
     * 记录用户操作的审计日志
     */
    public void logUser(String instanceId, String stepId, String action,
                        String userId, String userName, Map<String, Object> detail) {
        log(instanceId, stepId, action, ACTOR_USER, userId, userName, detail);
    }

    /**
     * 记录Agent操作的审计日志
     */
    public void logAgent(String instanceId, String stepId, String action,
                         String agentId, String agentName, Map<String, Object> detail) {
        log(instanceId, stepId, action, ACTOR_AGENT, agentId, agentName, detail);
    }

    /**
     * 获取实例的审计日志列表
     */
    public List<WorkflowAuditLogEntity> getInstanceLogs(String instanceId) {
        return auditLogRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
    }

    /**
     * 获取实例中指定操作类型的审计日志
     */
    public List<WorkflowAuditLogEntity> getInstanceLogsByAction(String instanceId, String action) {
        return auditLogRepository.findByInstanceIdAndActionOrderByCreatedAtAsc(instanceId, action);
    }
}
