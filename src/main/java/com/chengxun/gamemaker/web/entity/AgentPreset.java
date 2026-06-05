package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Agent 预设实体
 * 全局 Agent 能力模板，可被项目引用创建 Agent
 *
 * 主要功能：
 * - 定义 Agent 的角色、能力、配置等预设参数
 * - 支持系统内置预设和用户自定义预设
 * - 项目创建时可从预设初始化 Agent
 * - 优秀 Agent 的能力可导出为全局预设
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "agent_presets")
public class AgentPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 预设名称（如：标准制作人、资深服务端开发） */
    @NotBlank(message = "名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    /** Agent 角色（producer、server-dev、ui-dev 等） */
    @NotBlank(message = "角色不能为空")
    @Column(nullable = false, length = 50)
    private String role;

    /** 预设描述 */
    @Column(length = 500)
    private String description;

    /** 推理深度 1-5 */
    @Column(name = "reasoning_depth")
    private int reasoningDepth = 3;

    /** 能力标签（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String capabilities;

    /** 自定义标签（JSON 对象） */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /** 支持的文件类型（JSON 数组） */
    @Column(name = "supported_file_types", columnDefinition = "TEXT")
    private String supportedFileTypes;

    /** 不支持的功能（JSON 数组） */
    @Column(name = "unsupported_features", columnDefinition = "TEXT")
    private String unsupportedFeatures;

    /** 最大上下文大小（token 数） */
    @Column(name = "max_context_size")
    private int maxContextSize = 100000;

    /** 是否支持图片生成 */
    @Column(name = "supports_image_generation")
    private boolean supportsImageGeneration = false;

    /** 是否支持代码执行 */
    @Column(name = "supports_code_execution")
    private boolean supportsCodeExecution = true;

    /** 是否支持文件操作 */
    @Column(name = "supports_file_operations")
    private boolean supportsFileOperations = true;

    /** API 提供商 */
    @Column(name = "api_provider", length = 50)
    private String apiProvider = "anthropic";

    /** 是否系统内置（内置预设不可删除） */
    @Column(name = "is_system")
    private boolean system = false;

    /** 来源 Agent ID（从运行 Agent 导出时记录） */
    @Column(name = "source_agent_id", length = 200)
    private String sourceAgentId;

    /** 来源项目 ID */
    @Column(name = "source_project_id", length = 100)
    private String sourceProjectId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getReasoningDepth() { return reasoningDepth; }
    public void setReasoningDepth(int reasoningDepth) { this.reasoningDepth = reasoningDepth; }

    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getSupportedFileTypes() { return supportedFileTypes; }
    public void setSupportedFileTypes(String supportedFileTypes) { this.supportedFileTypes = supportedFileTypes; }

    public String getUnsupportedFeatures() { return unsupportedFeatures; }
    public void setUnsupportedFeatures(String unsupportedFeatures) { this.unsupportedFeatures = unsupportedFeatures; }

    public int getMaxContextSize() { return maxContextSize; }
    public void setMaxContextSize(int maxContextSize) { this.maxContextSize = maxContextSize; }

    public boolean isSupportsImageGeneration() { return supportsImageGeneration; }
    public void setSupportsImageGeneration(boolean supportsImageGeneration) { this.supportsImageGeneration = supportsImageGeneration; }

    public boolean isSupportsCodeExecution() { return supportsCodeExecution; }
    public void setSupportsCodeExecution(boolean supportsCodeExecution) { this.supportsCodeExecution = supportsCodeExecution; }

    public boolean isSupportsFileOperations() { return supportsFileOperations; }
    public void setSupportsFileOperations(boolean supportsFileOperations) { this.supportsFileOperations = supportsFileOperations; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String apiProvider) { this.apiProvider = apiProvider; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public String getSourceAgentId() { return sourceAgentId; }
    public void setSourceAgentId(String sourceAgentId) { this.sourceAgentId = sourceAgentId; }

    public String getSourceProjectId() { return sourceProjectId; }
    public void setSourceProjectId(String sourceProjectId) { this.sourceProjectId = sourceProjectId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
