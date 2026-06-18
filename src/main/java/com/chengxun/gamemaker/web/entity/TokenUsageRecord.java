package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Token 使用记录实体
 * 用于滑动窗口配额计算，记录每次 Token 使用的时间戳和用量
 *
 * 主要功能：
 * - 记录每次 API 调用的 token 消耗
 * - 支持按时间窗口查询累计用量
 * - 定期清理过期记录
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "token_usage_records", indexes = {
    @Index(name = "idx_usage_token_time", columnList = "token_id,used_at")
})
public class TokenUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token ID */
    @Column(name = "token_id", nullable = false)
    private Long tokenId;

    /** 本次使用的 token 数量 */
    @Column(name = "tokens_used", nullable = false)
    private Long tokensUsed;

    /** 使用时间 */
    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();

    public TokenUsageRecord() {}

    public TokenUsageRecord(Long tokenId, Long tokensUsed) {
        this.tokenId = tokenId;
        this.tokensUsed = tokensUsed;
        this.usedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }

    public Long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Long tokensUsed) { this.tokensUsed = tokensUsed; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
