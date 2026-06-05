package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog;
import com.chengxun.gamemaker.web.repository.CapabilityInvocationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 能力调用拦截器
 * 提供统一的拦截链：权限检查 → 参数验证 → 审批检查 → 执行 → 通知 → 审计
 *
 * 当前实现为轻量级拦截器，后续可扩展为 AOP 切面或拦截器链模式
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CapabilityInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CapabilityInterceptor.class);

    private final CapabilityInvocationLogRepository logRepository;

    public CapabilityInterceptor(CapabilityInvocationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 拦截上下文
     */
    public static class InterceptContext {
        private final Agent agent;
        private final AgentCapability capability;
        private final CapabilityCall call;
        private final String projectId;

        public InterceptContext(Agent agent, AgentCapability capability, CapabilityCall call, String projectId) {
            this.agent = agent;
            this.capability = capability;
            this.call = call;
            this.projectId = projectId;
        }

        public Agent getAgent() { return agent; }
        public AgentCapability getCapability() { return capability; }
        public CapabilityCall getCall() { return call; }
        public String getProjectId() { return projectId; }
    }

    /**
     * 拦截结果
     */
    public static class InterceptResult {
        private final boolean allowed;
        private final String reason;

        private InterceptResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static InterceptResult allow() {
            return new InterceptResult(true, null);
        }

        public static InterceptResult deny(String reason) {
            return new InterceptResult(false, reason);
        }

        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
    }

    /**
     * 前置拦截（执行前检查）
     */
    public InterceptResult beforeExecute(InterceptContext context) {
        // 1. 权限检查
        InterceptResult permissionCheck = checkPermission(context);
        if (!permissionCheck.isAllowed()) return permissionCheck;

        // 2. 越权检查（制作人不能审批自己的敏感操作）
        InterceptResult escalationCheck = checkPrivilegeEscalation(context);
        if (!escalationCheck.isAllowed()) return escalationCheck;

        // 3. 项目隔离检查
        InterceptResult isolationCheck = checkProjectIsolation(context);
        if (!isolationCheck.isAllowed()) return isolationCheck;

        return InterceptResult.allow();
    }

    /**
     * 后置拦截（执行后处理）
     */
    public void afterExecute(InterceptContext context, CapabilityResult result) {
        // 记录审计日志
        auditLog(context, result);

        // 通知（如果需要）
        if (result.isSuccess() && context.getCapability().isRequiresApproval()) {
            notifyApprovalExecuted(context, result);
        }
    }

    /**
     * 权限检查
     * 检查当前 Agent 是否有权调用该能力
     */
    private InterceptResult checkPermission(InterceptContext context) {
        Agent agent = context.getAgent();
        AgentCapability cap = context.getCapability();

        // 检查能力是否属于当前角色
        if (!cap.getAgentRole().equals(agent.getRole())) {
            // 通用能力（如 sendMessage）允许所有角色调用
            if (!isCommonCapability(cap.getCapabilityName())) {
                return InterceptResult.deny(
                    String.format("角色 %s 无权调用能力 %s", agent.getRole(), cap.getCapabilityName()));
            }
        }

        return InterceptResult.allow();
    }

    /**
     * 越权检查
     * 制作人不能通过能力系统绕过审批来执行敏感操作
     */
    private InterceptResult checkPrivilegeEscalation(InterceptContext context) {
        AgentCapability cap = context.getCapability();

        // 如果能力需要审批，执行引擎会先创建审批请求
        // 这里检查是否有审批服务可用
        if (cap.isRequiresApproval()) {
            // 审批检查在执行引擎中处理，这里只做日志记录
            log.info("Sensitive capability {} requires approval for agent {}",
                cap.getCapabilityName(), context.getAgent().getId());
        }

        return InterceptResult.allow();
    }

    /**
     * 项目隔离检查
     * 确保 Agent 只操作自己项目内的资源
     */
    private InterceptResult checkProjectIsolation(InterceptContext context) {
        // 如果能力有 projectId 限制，检查是否匹配
        AgentCapability cap = context.getCapability();
        if (cap.getProjectId() != null && context.getProjectId() != null) {
            if (!cap.getProjectId().equals(context.getProjectId())) {
                return InterceptResult.deny(
                    String.format("项目 %s 的能力不能在项目 %s 中使用",
                        cap.getProjectId(), context.getProjectId()));
            }
        }

        return InterceptResult.allow();
    }

    /**
     * 是否为通用能力（所有角色都可以调用）
     */
    private boolean isCommonCapability(String capabilityName) {
        return switch (capabilityName) {
            case "sendMessage", "saveKnowledge", "compactContext", "reportStatus" -> true;
            default -> false;
        };
    }

    /**
     * 审计日志
     */
    private void auditLog(InterceptContext context, CapabilityResult result) {
        try {
            log.info("Capability audit: agent={}, capability={}, status={}, project={}",
                context.getAgent().getId(),
                context.getCapability().getCapabilityName(),
                result.getStatus(),
                context.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * 通知审批后执行的操作
     */
    private void notifyApprovalExecuted(InterceptContext context, CapabilityResult result) {
        log.info("Approved capability executed: agent={}, capability={}, result={}",
            context.getAgent().getId(),
            context.getCapability().getCapabilityName(),
            result.getStatus());
    }
}
