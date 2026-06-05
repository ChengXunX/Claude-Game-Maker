package com.chengxun.gamemaker.service;

/**
 * 能力执行结果
 * 封装单个能力调用的执行结果
 *
 * @author chengxun
 * @since 2.0.0
 */
public class CapabilityResult {

    /** 执行状态 */
    public enum Status {
        /** 执行成功 */
        SUCCESS,
        /** 执行失败 */
        FAILED,
        /** 待审批（已创建审批请求） */
        PENDING_APPROVAL,
        /** 能力未找到 */
        NOT_FOUND,
        /** 能力已禁用 */
        DISABLED,
        /** 参数验证失败 */
        INVALID_PARAMS,
        /** 冷却中 */
        COOLDOWN
    }

    private final Status status;
    private final Object data;
    private final String error;
    private final Long approvalRequestId;

    private CapabilityResult(Status status, Object data, String error, Long approvalRequestId) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.approvalRequestId = approvalRequestId;
    }

    public static CapabilityResult success(Object data) {
        return new CapabilityResult(Status.SUCCESS, data, null, null);
    }

    public static CapabilityResult success() {
        return new CapabilityResult(Status.SUCCESS, null, null, null);
    }

    public static CapabilityResult failed(String error) {
        return new CapabilityResult(Status.FAILED, null, error, null);
    }

    public static CapabilityResult pendingApproval(Long approvalRequestId) {
        return new CapabilityResult(Status.PENDING_APPROVAL, null, null, approvalRequestId);
    }

    public static CapabilityResult notFound(String capabilityName) {
        return new CapabilityResult(Status.NOT_FOUND, null, "能力不存在: " + capabilityName, null);
    }

    public static CapabilityResult disabled(String capabilityName) {
        return new CapabilityResult(Status.DISABLED, null, "能力已禁用: " + capabilityName, null);
    }

    public static CapabilityResult invalidParams(String error) {
        return new CapabilityResult(Status.INVALID_PARAMS, null, error, null);
    }

    public static CapabilityResult cooldown(String capabilityName) {
        return new CapabilityResult(Status.COOLDOWN, null, "能力冷却中: " + capabilityName, null);
    }

    public Status getStatus() { return status; }
    public Object getData() { return data; }
    public String getError() { return error; }
    public Long getApprovalRequestId() { return approvalRequestId; }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isPendingApproval() { return status == Status.PENDING_APPROVAL; }
    public boolean isFailed() { return status == Status.FAILED; }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "CapabilityResult[SUCCESS] data=" + (data != null ? data.toString().substring(0, Math.min(100, data.toString().length())) : "null");
        }
        return String.format("CapabilityResult[%s] error=%s", status, error);
    }
}
