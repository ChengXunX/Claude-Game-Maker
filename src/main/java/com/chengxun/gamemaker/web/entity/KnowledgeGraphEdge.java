package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识图谱边实体
 * 表示知识图谱中两个节点之间的关系
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "knowledge_graph_edges", indexes = {
    @Index(name = "idx_kge_project", columnList = "project_id"),
    @Index(name = "idx_kge_from", columnList = "from_node_id"),
    @Index(name = "idx_kge_to", columnList = "to_node_id"),
    @Index(name = "idx_kge_type", columnList = "relation_type")
})
public class KnowledgeGraphEdge {

    /** 关系类型 */
    public enum RelationType {
        DEPENDS_ON, PRODUCES, VERIFIES, USES, BELONGS_TO, COLLABORATES_WITH, EVOLVES_FROM, FIXES
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "from_node_id", nullable = false)
    private Long fromNodeId;

    @Column(name = "to_node_id", nullable = false)
    private Long toNodeId;

    /** 关系类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", length = 30, nullable = false)
    private RelationType relationType;

    /** 关系属性（JSON） */
    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Long getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(Long fromNodeId) { this.fromNodeId = fromNodeId; }
    public Long getToNodeId() { return toNodeId; }
    public void setToNodeId(Long toNodeId) { this.toNodeId = toNodeId; }
    public RelationType getRelationType() { return relationType; }
    public void setRelationType(RelationType relationType) { this.relationType = relationType; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
