package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 项目 Token 绑定实体
 * 定义项目使用哪些 API Token
 *
 * 设计原则：
 * - Token 全局统一管理（ApiToken）
 * - 项目自选使用哪个 Token，可绑定多个并设置优先级
 * - 没有绑定的项目使用系统默认 Token
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "project_token_binding", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "token_id"})
})
public class ProjectTokenBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @NotBlank(message = "项目ID不能为空")
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** Token ID（关联 ApiToken.id） */
    @Column(name = "token_id", nullable = false)
    private Long tokenId;

    /** 是否为项目默认 Token */
    @Column(name = "is_default")
    private boolean isDefault = false;

    /** 优先级（数字越小优先级越高） */
    @Column(name = "priority")
    private Integer priority = 100;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
