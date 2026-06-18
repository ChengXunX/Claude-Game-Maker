package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.KnowledgeGraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeGraphEdgeRepository extends JpaRepository<KnowledgeGraphEdge, Long> {

    List<KnowledgeGraphEdge> findByProjectId(String projectId);

    List<KnowledgeGraphEdge> findByFromNodeId(Long fromNodeId);

    List<KnowledgeGraphEdge> findByToNodeId(Long toNodeId);

    @Query("SELECT e FROM KnowledgeGraphEdge e WHERE e.fromNodeId = :nodeId OR e.toNodeId = :nodeId")
    List<KnowledgeGraphEdge> findByNodeId(@Param("nodeId") Long nodeId);

    @Query("SELECT e FROM KnowledgeGraphEdge e WHERE e.projectId = :projectId AND e.relationType = :type")
    List<KnowledgeGraphEdge> findByProjectAndType(@Param("projectId") String projectId, @Param("type") KnowledgeGraphEdge.RelationType type);

    @Query("SELECT COUNT(e) FROM KnowledgeGraphEdge e WHERE e.projectId = :projectId")
    long countByProject(@Param("projectId") String projectId);

    void deleteByProjectId(String projectId);
}
