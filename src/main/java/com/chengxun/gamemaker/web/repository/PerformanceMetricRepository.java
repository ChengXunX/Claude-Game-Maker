package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.PerformanceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能指标数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface PerformanceMetricRepository extends JpaRepository<PerformanceMetric, Long> {

    /**
     * 根据指标名称查找
     */
    List<PerformanceMetric> findByMetricName(String metricName);

    /**
     * 根据指标类型查找
     */
    List<PerformanceMetric> findByMetricType(String metricType);

    /**
     * 根据指标名称和时间范围查找
     */
    @Query("SELECT m FROM PerformanceMetric m WHERE m.metricName = :name AND m.createdAt BETWEEN :start AND :end ORDER BY m.createdAt")
    List<PerformanceMetric> findByMetricNameAndTimeRange(
        @Param("name") String metricName,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 根据Agent ID和时间范围查找
     */
    @Query("SELECT m FROM PerformanceMetric m WHERE m.agentId = :agentId AND m.createdAt BETWEEN :start AND :end ORDER BY m.createdAt")
    List<PerformanceMetric> findByAgentIdAndTimeRange(
        @Param("agentId") String agentId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 计算指标平均值
     */
    @Query("SELECT AVG(m.value) FROM PerformanceMetric m WHERE m.metricName = :name AND m.createdAt BETWEEN :start AND :end")
    Double calculateAverage(
        @Param("name") String metricName,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 计算指标最大值
     */
    @Query("SELECT MAX(m.value) FROM PerformanceMetric m WHERE m.metricName = :name AND m.createdAt BETWEEN :start AND :end")
    Double calculateMax(
        @Param("name") String metricName,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 计算指标最小值
     */
    @Query("SELECT MIN(m.value) FROM PerformanceMetric m WHERE m.metricName = :name AND m.createdAt BETWEEN :start AND :end")
    Double calculateMin(
        @Param("name") String metricName,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 统计各指标类型的数量
     */
    @Query("SELECT m.metricType, COUNT(m) FROM PerformanceMetric m GROUP BY m.metricType")
    List<Object[]> countByMetricType();

    /**
     * 获取最近的指标记录
     */
    @Query("SELECT m FROM PerformanceMetric m WHERE m.metricName = :name ORDER BY m.createdAt DESC")
    List<PerformanceMetric> findRecentByName(@Param("name") String metricName);

    /**
     * 分批删除指定时间之前的指标数据
     *
     * @param cutoff 截止时间
     * @param limit 每批删除数量
     * @return 实际删除的记录数
     */
    @Modifying
    @Query(value = "DELETE FROM performance_metrics WHERE created_at < :cutoff LIMIT :limit", nativeQuery = true)
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
