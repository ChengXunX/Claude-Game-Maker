package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.DocumentIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档索引数据访问接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface DocumentIndexRepository extends JpaRepository<DocumentIndex, Long> {

    /**
     * 根据文件路径查找
     */
    Optional<DocumentIndex> findByFilePath(String filePath);

    /**
     * 根据内容哈希查找（用于去重）
     */
    Optional<DocumentIndex> findByContentHash(String contentHash);

    /**
     * 根据文档类型查找
     */
    List<DocumentIndex> findByDocType(String docType);

    /**
     * 根据 Agent ID 查找
     */
    List<DocumentIndex> findByAgentId(String agentId);

    /**
     * 根据项目 ID 查找
     */
    List<DocumentIndex> findByProjectId(String projectId);

    /**
     * 全文搜索（标题、摘要、关键词）
     */
    @Query("SELECT d FROM DocumentIndex d WHERE " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<DocumentIndex> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计各类型文档数量
     */
    @Query("SELECT d.docType, COUNT(d) FROM DocumentIndex d GROUP BY d.docType")
    List<Object[]> countByDocType();

    /**
     * 统计各 Agent 文档数量
     */
    @Query("SELECT d.agentId, COUNT(d) FROM DocumentIndex d WHERE d.agentId IS NOT NULL GROUP BY d.agentId")
    List<Object[]> countByAgent();
}
