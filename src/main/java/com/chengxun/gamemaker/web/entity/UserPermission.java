package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户权限实体
 * 存储用户的额外权限（独立于角色）
 *
 * 权限来源：
 * - GRANTED: 管理员直接授予
 * - APPROVED: 申请审批通过
 * - INHERITED: 从角色继承（只读，不存储）
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "user_permissions", indexes = {
    @Index(name = "idx_up_user", columnList = "userId"),
    @Index(name = "idx_up_perm", columnList = "permission"),
    @Index(name = "idx_up_user_perm", columnList = "userId, permission", unique = true)
})
public class UserPermission {

    public enum PermissionSource {
        GRANTED,    // 管理员直接授予
        APPROVED    // 申请审批通过
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID */
    @Column(nullable = false)
    private Long userId;

    /** 权限标识（如 PERM_agents:manage） */
    @Column(nullable = false, length = 100)
    private String permission;

    /** 权限来源 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionSource source;

    /** 授权者（管理员用户名） */
    @Column(length = 100)
    private String grantedBy;

    /** 授权原因 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 过期时间（null 表示永不过期） */
    private LocalDateTime expiresAt;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public PermissionSource getSource() { return source; }
    public void setSource(PermissionSource source) { this.source = source; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** 是否已过期 */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /** 是否有效（启用且未过期） */
    public boolean isValid() {
        return enabled && !isExpired();
    }

    @Override
    public String toString() {
        return String.format("UserPermission[user=%d, perm=%s, source=%s]", userId, permission, source);
    }
}
