package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog;
import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog.InvocationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 能力调用日志仓库
 * 提供调用日志的查询和统计方法
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface CapabilityInvocationLogRepository extends JpaRepository<CapabilityInvocationLog, Long> {

    /**
     * 按 Agent 查询调用日志（最新在前）
     */
    Page<CapabilityInvocationLog> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    /**
     * 按项目查询调用日志
     */
    Page<CapabilityInvocationLog> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    /**
     * 按状态查询
     */
    List<CapabilityInvocationLog> findByStatusOrderByCreatedAtDesc(InvocationStatus status);

    /**
     * 按时间范围查询
     */
    Page<CapabilityInvocationLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 按 Agent 和状态统计
     */
    @Query("SELECT COUNT(l) FROM CapabilityInvocationLog l WHERE l.agentId = :agentId AND l.status = :status")
    long countByAgentIdAndStatus(@Param("agentId") String agentId, @Param("status") InvocationStatus status);

    /**
     * 按项目和时间范围统计
     */
    @Query("SELECT COUNT(l) FROM CapabilityInvocationLog l WHERE l.projectId = :projectId AND l.createdAt BETWEEN :start AND :end")
    long countByProjectIdAndTimeRange(@Param("projectId") String projectId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /**
     * 按能力名称统计调用次数
     */
    @Query("SELECT l.capabilityName, COUNT(l) FROM CapabilityInvocationLog l WHERE l.createdAt BETWEEN :start AND :end GROUP BY l.capabilityName ORDER BY COUNT(l) DESC")
    List<Object[]> countByCapabilityName(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按能力名称统计成功率
     */
    @Query("SELECT l.capabilityName, " +
           "COUNT(l), " +
           "SUM(CASE WHEN l.status = 'EXECUTED' THEN 1 ELSE 0 END) " +
           "FROM CapabilityInvocationLog l WHERE l.createdAt BETWEEN :start AND :end GROUP BY l.capabilityName")
    List<Object[]> statsByCapabilityName(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找超时的待审批请求
     */
    @Query("SELECT l FROM CapabilityInvocationLog l WHERE l.status = 'PENDING_APPROVAL' AND l.createdAt < :timeout")
    List<CapabilityInvocationLog> findExpiredPendingApprovals(@Param("timeout") LocalDateTime timeout);

    /**
     * 按审批请求 ID 查找调用日志
     */
    Optional<CapabilityInvocationLog> findByApprovalRequestId(Long approvalRequestId);

    /**
     * 查找最近的调用记录（用于冷却时间检查）
     */
    @Query("SELECT l FROM CapabilityInvocationLog l WHERE l.agentId = :agentId AND l.capabilityName = :capabilityName AND l.createdAt > :since ORDER BY l.createdAt DESC")
    List<CapabilityInvocationLog> findRecentInvocations(@Param("agentId") String agentId,
                                                         @Param("capabilityName") String capabilityName,
                                                         @Param("since") LocalDateTime since);

    /**
     * 统计总调用次数
     */
    @Query("SELECT COUNT(l) FROM CapabilityInvocationLog l WHERE l.createdAt BETWEEN :start AND :end")
    long totalCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计各状态的调用次数
     */
    @Query("SELECT l.status, COUNT(l) FROM CapabilityInvocationLog l WHERE l.createdAt BETWEEN :start AND :end GROUP BY l.status")
    List<Object[]> countByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按 Agent 统计调用次数（Top N）
     */
    @Query("SELECT l.agentId, COUNT(l) FROM CapabilityInvocationLog l WHERE l.createdAt BETWEEN :start AND :end GROUP BY l.agentId ORDER BY COUNT(l) DESC")
    List<Object[]> countByAgent(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
}
