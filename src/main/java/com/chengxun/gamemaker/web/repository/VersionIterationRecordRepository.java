package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.VersionIterationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 版本迭代记录仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface VersionIterationRecordRepository extends JpaRepository<VersionIterationRecord, Long> {

    /**
     * 获取项目的所有版本迭代记录
     */
    List<VersionIterationRecord> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 获取项目的最新版本迭代记录
     */
    Optional<VersionIterationRecord> findFirstByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 获取项目的指定版本迭代记录
     */
    Optional<VersionIterationRecord> findByProjectIdAndVersion(String projectId, String version);

    /**
     * 统计项目的版本迭代次数
     */
    long countByProjectId(String projectId);

    /**
     * 统计项目通过验收的版本迭代次数
     */
    long countByProjectIdAndPassed(String projectId, boolean passed);

    /**
     * 获取项目的平均评估分数
     */
    @Query("SELECT AVG(r.evaluationScore) FROM VersionIterationRecord r WHERE r.projectId = :projectId")
    Double getAverageScoreByProjectId(@Param("projectId") String projectId);

    /**
     * 获取所有项目的版本迭代统计
     */
    @Query("SELECT r.projectId, COUNT(r), AVG(r.evaluationScore), " +
           "SUM(CASE WHEN r.passed = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.result = 'COMPLETED' THEN 1 ELSE 0 END) " +
           "FROM VersionIterationRecord r GROUP BY r.projectId")
    List<Object[]> getProjectIterationStats();
}
