package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Agent健康指标数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AgentHealthRepository extends JpaRepository<AgentHealth, Long> {

    /**
     * 根据Agent ID查找最新健康记录
     */
    Optional<AgentHealth> findFirstByAgentIdOrderByCheckTimeDesc(String agentId);

    /**
     * 根据Agent ID查找所有健康记录
     */
    List<AgentHealth> findByAgentIdOrderByCheckTimeDesc(String agentId);

    /**
     * 查找项目的所有 Agent 健康记录
     */
    List<AgentHealth> findByProjectIdOrderByCheckTimeDesc(String projectId);

    /**
     * 查找项目的最新健康记录（按 Agent 去重）
     */
    @Query("SELECT h FROM AgentHealth h WHERE h.projectId = :projectId AND h.id IN " +
           "(SELECT MAX(h2.id) FROM AgentHealth h2 WHERE h2.projectId = :projectId GROUP BY h2.agentId)")
    List<AgentHealth> findLatestByProjectId(@Param("projectId") String projectId);

    /**
     * 统计项目的各健康状态 Agent 数量
     */
    @Query("SELECT h.healthStatus, COUNT(DISTINCT h.agentId) FROM AgentHealth h WHERE h.projectId = :projectId GROUP BY h.healthStatus")
    List<Object[]> countByHealthStatusAndProjectId(@Param("projectId") String projectId);

    /**
     * 根据健康状态查找
     */
    List<AgentHealth> findByHealthStatus(AgentHealth.HealthStatus status);

    /**
     * 查找需要重启的Agent
     */
    @Query("SELECT h FROM AgentHealth h WHERE h.healthStatus = 'UNHEALTHY' AND h.consecutiveErrors >= 10")
    List<AgentHealth> findAgentsNeedingRestart();

    /**
     * 查找响应缓慢的Agent
     */
    @Query("SELECT h FROM AgentHealth h WHERE h.avgResponseTimeMs > 3000")
    List<AgentHealth> findSlowAgents();

    /**
     * 统计各健康状态的Agent数量
     */
    @Query("SELECT h.healthStatus, COUNT(DISTINCT h.agentId) FROM AgentHealth h GROUP BY h.healthStatus")
    List<Object[]> countByHealthStatus();

    /**
     * 查找最近N小时内的健康记录
     */
    @Query("SELECT h FROM AgentHealth h WHERE h.checkTime >= :since ORDER BY h.checkTime DESC")
    List<AgentHealth> findRecentHealthRecords(@Param("since") LocalDateTime since);

    /**
     * 查找Agent的健康历史
     */
    @Query("SELECT h FROM AgentHealth h WHERE h.agentId = :agentId AND h.checkTime >= :since ORDER BY h.checkTime DESC")
    List<AgentHealth> findAgentHealthHistory(@Param("agentId") String agentId, @Param("since") LocalDateTime since);
}
