package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 项目成员实体
 * 定义用户在项目中的角色和权限
 *
 * 权限模型：
 * - OWNER：项目拥有者（制作人），完全控制权
 * - MANAGER：项目经理，可管理 Agent 和配置
 * - DEVELOPER：开发者，可查看和操作 Agent
 * - VIEWER：只读，只能查看项目状态
 *
 * 管理员（ADMIN 角色）自动拥有所有项目的完全权限，无需在此表中配置
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "project_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "user_id"})
})
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID（非外键，项目在文件系统管理） */
    @NotBlank(message = "项目ID不能为空")
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** 用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 在项目中的角色 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role = ProjectRole.VIEWER;

    /** 备注 */
    @Column(length = 255)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 项目角色枚举
     */
    public enum ProjectRole {
        /** 项目拥有者（制作人），完全控制权 */
        OWNER,
        /** 项目经理，可管理 Agent 和配置 */
        MANAGER,
        /** 开发者，可查看和操作 Agent */
        DEVELOPER,
        /** 只读，只能查看项目状态 */
        VIEWER
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否有足够的权限级别
     * 权限层级：OWNER > MANAGER > DEVELOPER > VIEWER
     *
     * @param required 需要的最低权限
     * @return 是否满足权限要求
     */
    public boolean hasPermission(ProjectRole required) {
        return this.role.ordinal() <= required.ordinal();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ProjectRole getRole() { return role; }
    public void setRole(ProjectRole role) { this.role = role; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
