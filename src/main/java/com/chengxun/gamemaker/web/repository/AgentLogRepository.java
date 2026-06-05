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
           "(:keyword IS NULL OR a.summary LIKE %:keyword% OR a.detail LIKE %:keyword%) AND " +
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
           "(:keyword IS NULL OR a.summary LIKE %:keyword% OR a.detail LIKE %:keyword%) AND " +
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
}
