package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent性能数据访问层
 * 提供Agent性能指标的数据库操作
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AgentPerformanceRepository extends JpaRepository<AgentPerformance, Long> {

    /**
     * 根据Agent ID查找性能记录
     */
    Optional<AgentPerformance> findByAgentId(String agentId);

    /**
     * 根据项目 ID 和 Agent ID 查找性能记录
     */
    Optional<AgentPerformance> findByProjectIdAndAgentId(String projectId, String agentId);

    /**
     * 根据Agent角色查找性能记录
     */
    List<AgentPerformance> findByAgentRole(String agentRole);

    /**
     * 查找项目的所有 Agent 性能记录
     */
    List<AgentPerformance> findByProjectIdOrderByOverallScoreDesc(String projectId);

    /**
     * 查找项目的可用 Agent（负载低于阈值且评分高于阈值）
     */
    @Query("SELECT p FROM AgentPerformance p WHERE p.projectId = :projectId AND p.currentLoad <= :maxLoad AND p.overallScore >= :minScore ORDER BY p.overallScore DESC")
    List<AgentPerformance> findAvailableAgentsByProject(@Param("projectId") String projectId, @Param("maxLoad") Integer maxLoad, @Param("minScore") Double minScore);

    /**
     * 按综合评分降序排列所有Agent
     * @return 性能记录列表
     */
    List<AgentPerformance> findAllByOrderByOverallScoreDesc();

    /**
     * 按可靠性评分降序排列所有Agent
     * @return 性能记录列表
     */
    List<AgentPerformance> findAllByOrderByReliabilityScoreDesc();

    /**
     * 按效率评分降序排列所有Agent
     * @return 性能记录列表
     */
    List<AgentPerformance> findAllByOrderByEfficiencyScoreDesc();

    /**
     * 查找综合评分高于指定值的Agent
     * @param score 评分阈值
     * @return 性能记录列表
     */
    List<AgentPerformance> findByOverallScoreGreaterThanEqualOrderByOverallScoreDesc(Double score);

    /**
     * 查找当前负载低于指定值的Agent
     * @param load 负载阈值
     * @return 性能记录列表
     */
    List<AgentPerformance> findByCurrentLoadLessThanEqualOrderByCurrentLoadAsc(Integer load);

    /**
     * 查找可用的Agent（负载低于指定值且综合评分高于指定值）
     * @param maxLoad 最大负载
     * @param minScore 最小评分
     * @return 性能记录列表
     */
    @Query("SELECT p FROM AgentPerformance p WHERE p.currentLoad <= :maxLoad AND p.overallScore >= :minScore ORDER BY p.overallScore DESC")
    List<AgentPerformance> findAvailableAgents(@Param("maxLoad") Integer maxLoad, @Param("minScore") Double minScore);

    /**
     * 根据角色查找最佳Agent（综合评分最高且负载可接受）
     * @param role Agent角色
     * @param maxLoad 最大负载
     * @return 性能记录列表
     */
    @Query("SELECT p FROM AgentPerformance p WHERE p.agentRole = :role AND p.currentLoad <= :maxLoad ORDER BY p.overallScore DESC")
    List<AgentPerformance> findBestAgentsByRole(@Param("role") String role, @Param("maxLoad") Integer maxLoad);

    /**
     * 统计各角色的Agent数量
     * @return 角色统计结果
     */
    @Query("SELECT p.agentRole, COUNT(p) FROM AgentPerformance p GROUP BY p.agentRole")
    List<Object[]> countByRole();

    /**
     * 统计各等级的Agent数量
     * @return 等级统计结果
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN p.overallScore >= 90 THEN 'S' " +
           "  WHEN p.overallScore >= 80 THEN 'A' " +
           "  WHEN p.overallScore >= 70 THEN 'B' " +
           "  WHEN p.overallScore >= 60 THEN 'C' " +
           "  ELSE 'D' " +
           "END as grade, " +
           "COUNT(p) " +
           "FROM AgentPerformance p GROUP BY " +
           "CASE " +
           "  WHEN p.overallScore >= 90 THEN 'S' " +
           "  WHEN p.overallScore >= 80 THEN 'A' " +
           "  WHEN p.overallScore >= 70 THEN 'B' " +
           "  WHEN p.overallScore >= 60 THEN 'C' " +
           "  ELSE 'D' " +
           "END")
    List<Object[]> countByGrade();

    /**
     * 查找最近活跃的Agent
     * @param limit 限制数量
     * @return 性能记录列表
     */
    @Query("SELECT p FROM AgentPerformance p ORDER BY p.lastTaskAt DESC")
    List<AgentPerformance> findRecentlyActive();

    /**
     * 查找需要关注的Agent（评分低或负载高）
     * @return 性能记录列表
     */
    @Query("SELECT p FROM AgentPerformance p WHERE p.overallScore < 60 OR p.currentLoad > 80 ORDER BY p.overallScore ASC")
    List<AgentPerformance> findAgentsNeedingAttention();
}
