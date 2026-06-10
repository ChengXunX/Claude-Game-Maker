package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, Long> {

    Page<AgentLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AgentLog> findTop50ByOrderByCreatedAtDesc();

    Page<AgentLog> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    Page<AgentLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AgentLog> findByLevelOrderByCreatedAtDesc(String level, Pageable pageable);

    @Query("SELECT a FROM AgentLog a WHERE " +
           "(:agentId IS NULL OR a.agentId = :agentId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:level IS NULL OR a.level = :level) AND " +
           "(:keyword IS NULL OR a.summary LIKE %:keyword% OR a.detail LIKE %:keyword% OR a.decision LIKE %:keyword%) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime) " +
           "ORDER BY a.createdAt DESC")
    Page<AgentLog> searchLogs(@Param("agentId") String agentId,
                              @Param("action") String action,
                              @Param("level") String level,
                              @Param("keyword") String keyword,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              Pageable pageable);

    @Query("SELECT a FROM AgentLog a WHERE " +
           "(:agentId IS NULL OR a.agentId = :agentId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:level IS NULL OR a.level = :level) AND " +
           "(:keyword IS NULL OR a.summary LIKE %:keyword% OR a.detail LIKE %:keyword% OR a.decision LIKE %:keyword%) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime) " +
           "ORDER BY a.createdAt DESC")
    List<AgentLog> searchLogs(@Param("agentId") String agentId,
                              @Param("action") String action,
                              @Param("level") String level,
                              @Param("keyword") String keyword,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a.action, COUNT(a) FROM AgentLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction();

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a GROUP BY a.agentId ORDER BY COUNT(a) DESC")
    List<Object[]> countByAgent();

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.action = :action AND a.createdAt >= :since GROUP BY a.agentId ORDER BY COUNT(a) DESC")
    List<Object[]> countByAgentAndActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.action = 'TASK_COMPLETED' AND a.createdAt >= :since GROUP BY a.agentId")
    List<Object[]> countCompletedTasksByAgentSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.action = 'TASK_FAILED' AND a.createdAt >= :since GROUP BY a.agentId")
    List<Object[]> countFailedTasksByAgentSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.action = 'DECISION' AND a.createdAt >= :since GROUP BY a.agentId")
    List<Object[]> countDecisionsByAgentSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.action = 'AI_CALL' AND a.createdAt >= :since GROUP BY a.agentId")
    List<Object[]> countAiCallsByAgentSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.agentId, COUNT(a) FROM AgentLog a WHERE a.level = 'ERROR' AND a.createdAt >= :since GROUP BY a.agentId")
    List<Object[]> countErrorsByAgentSince(@Param("since") LocalDateTime since);

    /**
     * 统计指定Agent在指定时间后的指定action的日志数量
     */
    long countByAgentIdAndActionAndCreatedAtAfter(String agentId, String action, LocalDateTime createdAt);

    /**
     * 统计指定Agent在指定时间后的指定level的日志数量
     */
    long countByAgentIdAndLevelAndCreatedAtAfter(String agentId, String level, LocalDateTime createdAt);
}
