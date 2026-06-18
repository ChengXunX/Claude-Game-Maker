package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.KnowledgeGraphNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeGraphNodeRepository extends JpaRepository<KnowledgeGraphNode, Long> {

    List<KnowledgeGraphNode> findByProjectId(String projectId);

    List<KnowledgeGraphNode> findByProjectIdAndNodeType(String projectId, KnowledgeGraphNode.NodeType nodeType);

    Optional<KnowledgeGraphNode> findByProjectIdAndNodeRefId(String projectId, String nodeRefId);

    @Query("SELECT n FROM KnowledgeGraphNode n WHERE n.projectId = :projectId AND n.displayName LIKE %:keyword%")
    List<KnowledgeGraphNode> searchByDisplayName(@Param("projectId") String projectId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(n) FROM KnowledgeGraphNode n WHERE n.projectId = :projectId")
    long countByProject(@Param("projectId") String projectId);

    void deleteByProjectId(String projectId);
}
