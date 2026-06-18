package com.chengxun.gamemaker.web.dto;

import com.chengxun.gamemaker.web.entity.ApiToken;
import java.time.LocalDateTime;

/**
 * API Token数据传输对象
 * 用于向前端传输Token信息，API Key脱敏显示
 */
public class ApiTokenDTO {

    private Long id;
    private String name;
    private String maskedApiKey;  // 脱敏后的API Key
    private String apiUrl;
    private String model;
    private Integer maxTokens;
    private String status;
    private Long usageCount;
    private Long totalTokensUsed;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean assigned;

    /**
     * 从ApiToken实体转换为ApiTokenDTO
     */
    public static ApiTokenDTO fromEntity(ApiToken token) {
        if (token == null) return null;

        ApiTokenDTO dto = new ApiTokenDTO();
        dto.setId(token.getId());
        dto.setName(token.getName());
        dto.setMaskedApiKey(token.getMaskedApiKey());  // 使用脱敏方法
        dto.setApiUrl(token.getApiUrl());
        dto.setModel(token.getModel());
        dto.setMaxTokens(token.getMaxTokens());
        dto.setStatus(token.getStatus() != null ? token.getStatus().name() : null);
        dto.setUsageCount(token.getUsageCount());
        dto.setTotalTokensUsed(token.getTotalTokensUsed());
        dto.setLastUsedAt(token.getLastUsedAt());
        dto.setExpiresAt(token.getExpiresAt());
        dto.setDescription(token.getDescription());
        dto.setCreatedBy(token.getCreatedBy());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setAssigned(token.isInUse());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMaskedApiKey() { return maskedApiKey; }
    public void setMaskedApiKey(String maskedApiKey) { this.maskedApiKey = maskedApiKey; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public Long getTotalTokensUsed() { return totalTokensUsed; }
    public void setTotalTokensUsed(Long totalTokensUsed) { this.totalTokensUsed = totalTokensUsed; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAssigned() { return assigned; }
    public void setAssigned(boolean assigned) { this.assigned = assigned; }
}
