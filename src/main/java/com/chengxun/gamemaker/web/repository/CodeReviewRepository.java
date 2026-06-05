package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.CodeReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码审查数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    /**
     * 根据Agent ID查找审查记录
     */
    List<CodeReview> findByAgentIdOrderByCreatedAtDesc(String agentId);

    /**
     * 根据项目ID查找审查记录
     */
    List<CodeReview> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 根据状态查找审查记录
     */
    List<CodeReview> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查找待审查的记录
     */
    Page<CodeReview> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    /**
     * 统计各状态的审查数量
     */
    @Query("SELECT r.status, COUNT(r) FROM CodeReview r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * 统计各Agent的审查数量
     */
    @Query("SELECT r.agentId, r.agentName, COUNT(r) FROM CodeReview r GROUP BY r.agentId, r.agentName")
    List<Object[]> countByAgent();

    /**
     * 获取最近的审查记录
     */
    Page<CodeReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找指定时间范围内的审查记录
     */
    @Query("SELECT r FROM CodeReview r WHERE r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    List<CodeReview> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计平均审查评分
     */
    @Query("SELECT AVG(r.score) FROM CodeReview r WHERE r.score IS NOT NULL")
    Double calculateAverageScore();

    /**
     * 查找需要修改的审查记录
     */
    List<CodeReview> findByStatusAndAgentId(String status, String agentId);

    /**
     * 根据Git仓库ID查找审查记录
     */
    List<CodeReview> findByGitRepositoryIdOrderByCreatedAtDesc(Long gitRepositoryId);

    /**
     * 根据Agent ID和Git仓库ID查找审查记录
     */
    List<CodeReview> findByAgentIdAndGitRepositoryIdOrderByCreatedAtDesc(String agentId, Long gitRepositoryId);

    /**
     * 根据Git仓库ID和状态查找审查记录
     */
    List<CodeReview> findByGitRepositoryIdAndStatusOrderByCreatedAtDesc(Long gitRepositoryId, String status);

    /**
     * 统计Git仓库的审查数量
     */
    @Query("SELECT r.gitRepositoryId, r.gitRepositoryName, COUNT(r) FROM CodeReview r WHERE r.gitRepositoryId IS NOT NULL GROUP BY r.gitRepositoryId, r.gitRepositoryName")
    List<Object[]> countByGitRepository();

    /**
     * 统计各仓库类型的审查数量
     */
    @Query("SELECT r.repositoryType, COUNT(r) FROM CodeReview r WHERE r.repositoryType IS NOT NULL GROUP BY r.repositoryType")
    List<Object[]> countByRepositoryType();
}
