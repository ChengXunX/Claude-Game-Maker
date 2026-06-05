package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    /**
     * 根据状态查找告警记录
     */
    List<AlertRecord> findByStatus(String status);

    /**
     * 根据优先级查找告警记录
     */
    List<AlertRecord> findByPriority(String priority);

    /**
     * 根据Agent ID查找告警记录
     */
    List<AlertRecord> findByAgentId(String agentId);

    /**
     * 查找待处理的告警
     */
    List<AlertRecord> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查找指定时间范围内的告警
     */
    @Query("SELECT r FROM AlertRecord r WHERE r.createdAt BETWEEN :startTime AND :endTime ORDER BY r.createdAt DESC")
    List<AlertRecord> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态的告警数量
     */
    @Query("SELECT r.status, COUNT(r) FROM AlertRecord r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * 统计各优先级的告警数量
     */
    @Query("SELECT r.priority, COUNT(r) FROM AlertRecord r GROUP BY r.priority")
    List<Object[]> countByPriority();

    /**
     * 查找最近的告警记录
     */
    Page<AlertRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找未通知的告警
     */
    List<AlertRecord> findByNotified(String notified);

    /**
     * 统计指定时间内的告警数量
     */
    @Query("SELECT COUNT(r) FROM AlertRecord r WHERE r.createdAt >= :since")
    Long countSince(@Param("since") LocalDateTime since);
}
