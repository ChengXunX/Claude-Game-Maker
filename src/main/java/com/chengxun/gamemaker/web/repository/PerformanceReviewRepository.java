package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 绩效评审数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    /**
     * 根据评审编号查找
     */
    Optional<PerformanceReview> findByReviewNo(String reviewNo);

    /**
     * 根据被评审Agent ID查找
     */
    List<PerformanceReview> findByAgentIdOrderByCreatedAtDesc(String agentId);

    /**
     * 根据制作人ID查找
     */
    List<PerformanceReview> findByProducerIdOrderByCreatedAtDesc(String producerId);

    /**
     * 根据项目ID查找
     */
    List<PerformanceReview> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 根据评审周期查找
     */
    List<PerformanceReview> findByReviewPeriodOrderByCreatedAtDesc(String reviewPeriod);

    /**
     * 根据Agent ID和评审周期查找
     */
    Optional<PerformanceReview> findByAgentIdAndReviewPeriod(String agentId, String reviewPeriod);

    /**
     * 查找Agent最近N期的评审
     */
    @Query("SELECT r FROM PerformanceReview r WHERE r.agentId = :agentId ORDER BY r.reviewPeriod DESC")
    List<PerformanceReview> findRecentByAgentId(@Param("agentId") String agentId);

    /**
     * 统计Agent的警告次数
     */
    @Query("SELECT COUNT(r) FROM PerformanceReview r WHERE r.agentId = :agentId AND r.isWarning = true")
    Long countWarningsByAgentId(@Param("agentId") String agentId);

    /**
     * 查找低分评审（低于指定分数）
     */
    @Query("SELECT r FROM PerformanceReview r WHERE r.agentId = :agentId AND r.overallScore < :score ORDER BY r.reviewPeriod DESC")
    List<PerformanceReview> findLowScoreReviews(@Param("agentId") String agentId, @Param("score") Integer score);

    /**
     * 统计连续低分期数
     */
    @Query("SELECT r FROM PerformanceReview r WHERE r.agentId = :agentId ORDER BY r.reviewPeriod DESC")
    List<PerformanceReview> findAllByAgentIdOrderByPeriodDesc(@Param("agentId") String agentId);

    /**
     * 统计各Agent的平均评分
     */
    @Query("SELECT r.agentId, r.agentName, AVG(r.overallScore) FROM PerformanceReview r GROUP BY r.agentId, r.agentName")
    List<Object[]> getAverageScoresByAgent();

    /**
     * 统计项目的平均评分
     */
    @Query("SELECT r.projectId, r.projectName, AVG(r.overallScore) FROM PerformanceReview r GROUP BY r.projectId, r.projectName")
    List<Object[]> getAverageScoresByProject();
}
