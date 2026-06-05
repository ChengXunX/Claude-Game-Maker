package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 知识库数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    /**
     * 根据类别和键查找知识
     */
    Optional<KnowledgeBase> findByCategoryAndKnowledgeKey(String category, String knowledgeKey);

    /**
     * 查找系统级知识
     */
    List<KnowledgeBase> findByCategoryAndAccessLevelAndEnabledTrue(String category, String accessLevel);

    /**
     * 查找项目级知识
     */
    List<KnowledgeBase> findByProjectIdAndCategoryAndEnabledTrue(String projectId, String category);

    /**
     * 查找所有可用的系统级知识
     */
    List<KnowledgeBase> findByAccessLevelInAndEnabledTrueOrderByPriorityAsc(List<String> accessLevels);

    /**
     * 查找项目可用的知识（包括系统级和项目级）
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.enabled = true AND " +
           "(k.projectId IS NULL OR k.projectId = :projectId) AND " +
           "k.accessLevel IN :accessLevels ORDER BY k.priority ASC")
    List<KnowledgeBase> findAvailableKnowledge(
        @Param("projectId") String projectId,
        @Param("accessLevels") List<String> accessLevels);

    /**
     * 根据标签查找知识
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.enabled = true AND k.tags LIKE %:tag% ORDER BY k.priority ASC")
    List<KnowledgeBase> findByTag(@Param("tag") String tag);

    /**
     * 搜索知识
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.enabled = true AND " +
           "(k.title LIKE %:keyword% OR k.content LIKE %:keyword%) ORDER BY k.priority ASC")
    List<KnowledgeBase> search(@Param("keyword") String keyword);

    /**
     * 统计各类别知识数量
     */
    @Query("SELECT k.category, COUNT(k) FROM KnowledgeBase k WHERE k.enabled = true GROUP BY k.category")
    List<Object[]> countByCategory();

    /**
     * 查找提示词模板
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.enabled = true AND k.category = 'prompt_template' " +
           "AND k.knowledgeKey = :templateKey")
    Optional<KnowledgeBase> findPromptTemplate(@Param("templateKey") String templateKey);
}
