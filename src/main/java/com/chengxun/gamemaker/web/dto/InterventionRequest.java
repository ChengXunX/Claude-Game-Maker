package com.chengxun.gamemaker.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 干预请求DTO
 * 用于Agent干预接口的参数验证
 *
 * @author chengxun
 * @since 1.0.0
 */
public class InterventionRequest {

    /** Agent ID，必填 */
    @NotBlank(message = "Agent ID不能为空")
    @Size(max = 50, message = "Agent ID长度不能超过50个字符")
    private String agentId;

    /** 干预类型 */
    private String interventionType;

    /** 指令内容 */
    @Size(max = 10000, message = "指令内容不能超过10000字符")
    private String instruction;

    /** 干预原因 */
    @Size(max = 2000, message = "原因不能超过2000字符")
    private String reason;

    /** 任务ID（可选） */
    @Size(max = 100, message = "任务ID长度不能超过100个字符")
    private String taskId;

    /** 原始决策（覆盖决策时使用） */
    @Size(max = 10000, message = "原决策不能超过10000字符")
    private String originalDecision;

    /** 新决策（覆盖决策时使用） */
    @Size(max = 10000, message = "新决策不能超过10000字符")
    private String newDecision;

    /** 新方向（调整方向时使用） */
    @Size(max = 10000, message = "新方向不能超过10000字符")
    private String newDirection;

    // Getters and Setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getInterventionType() { return interventionType; }
    public void setInterventionType(String interventionType) { this.interventionType = interventionType; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getOriginalDecision() { return originalDecision; }
    public void setOriginalDecision(String originalDecision) { this.originalDecision = originalDecision; }

    public String getNewDecision() { return newDecision; }
    public void setNewDecision(String newDecision) { this.newDecision = newDecision; }

    public String getNewDirection() { return newDirection; }
    public void setNewDirection(String newDirection) { this.newDirection = newDirection; }
}
