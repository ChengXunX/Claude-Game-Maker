package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Token使用统计实体
 * 记录每日Token消耗情况，用于成本控制和资源管理
 *
 * 主要功能：
 * - 按日统计Token消耗
 * - 按Agent统计使用量
 * - 支持配额管理
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "token_usage", indexes = {
    @Index(name = "idx_token_usage_date", columnList = "usageDate"),
    @Index(name = "idx_token_usage_agent", columnList = "agentId"),
    @Index(name = "idx_token_usage_user", columnList = "userId")
})
public class TokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 使用日期 */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /** 用户ID */
    @Column(name = "user_id")
    private Long userId;

    /** Agent ID */
    @Column(name = "agent_id", length = 50)
    private String agentId;

    /** Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 输入Token数量 */
    @Column(name = "input_tokens")
    private Long inputTokens = 0L;

    /** 输出Token数量 */
    @Column(name = "output_tokens")
    private Long outputTokens = 0L;

    /** 总Token数量 */
    @Column(name = "total_tokens")
    private Long totalTokens = 0L;

    /** 调用次数 */
    @Column(name = "call_count")
    private Integer callCount = 0;

    /** 估算成本（美元） */
    @Column(name = "estimated_cost")
    private Double estimatedCost = 0.0;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 业务方法

    /**
     * 添加Token使用量
     * @param input 输入Token
     * @param output 输出Token
     */
    public void addUsage(long input, long output) {
        this.inputTokens += input;
        this.outputTokens += output;
        this.totalTokens = this.inputTokens + this.outputTokens;
        this.callCount++;
        this.updatedAt = LocalDateTime.now();

        // 估算成本（假设：输入$0.01/1K tokens，输出$0.03/1K tokens）
        this.estimatedCost = (this.inputTokens * 0.01 + this.outputTokens * 0.03) / 1000;
    }

    /**
     * 获取平均每次调用Token数
     */
    public double getAvgTokensPerCall() {
        if (callCount == 0) return 0;
        return (double) totalTokens / callCount;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }

    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }

    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }

    public Integer getCallCount() { return callCount; }
    public void setCallCount(Integer callCount) { this.callCount = callCount; }

    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
