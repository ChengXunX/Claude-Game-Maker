package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识图谱节点实体
 * 表示知识图谱中的一个实体节点（Agent、技能、文档、里程碑等）
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "knowledge_graph_nodes", indexes = {
    @Index(name = "idx_kgn_project", columnList = "project_id"),
    @Index(name = "idx_kgn_type", columnList = "node_type"),
    @Index(name = "idx_kgn_ref", columnList = "node_ref_id")
})
public class KnowledgeGraphNode {

    /** 节点类型 */
    public enum NodeType {
        AGENT, SKILL, DOCUMENT, MILESTONE, VERIFICATION, TASK, KNOWLEDGE, PROJECT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    /** 节点类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", length = 30, nullable = false)
    private NodeType nodeType;

    /** 节点引用ID（如 agentId, skillName, docPath 等） */
    @Column(name = "node_ref_id", length = 200)
    private String nodeRefId;

    /** 节点显示名称 */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /** 节点属性（JSON） */
    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }
    public String getNodeRefId() { return nodeRefId; }
    public void setNodeRefId(String nodeRefId) { this.nodeRefId = nodeRefId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
