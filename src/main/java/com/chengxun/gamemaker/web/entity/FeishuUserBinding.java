package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 飞书用户绑定实体
 * 存储飞书 open_id 与系统 user_id 的绑定关系
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "feishu_user_bindings")
public class FeishuUserBinding {

    /** 绑定状态枚举 */
    public enum BindingStatus {
        PENDING,  // 待绑定（验证码已生成）
        BOUND     // 已绑定
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 飞书用户 open_id（待绑定时可为空） */
    @Column(name = "open_id", length = 100)
    private String openId;

    /** 系统用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 绑定验证码 */
    @Column(name = "binding_code", length = 10)
    private String bindingCode;

    /** 绑定状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BindingStatus status = BindingStatus.PENDING;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBindingCode() { return bindingCode; }
    public void setBindingCode(String bindingCode) { this.bindingCode = bindingCode; }

    public BindingStatus getStatus() { return status; }
    public void setStatus(BindingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
