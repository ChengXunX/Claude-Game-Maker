package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentIntervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent干预记录数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AgentInterventionRepository extends JpaRepository<AgentIntervention, Long> {

    /**
     * 根据干预编号查找
     */
    Optional<AgentIntervention> findByInterventionNo(String interventionNo);

    /**
     * 根据Agent ID查找
     */
    List<AgentIntervention> findByAgentIdOrderByCreatedAtDesc(String agentId);

    /**
     * 根据用户ID查找
     */
    List<AgentIntervention> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据状态查找
     */
    List<AgentIntervention> findByStatusOrderByCreatedAtDesc(AgentIntervention.Status status);

    /**
     * 按多个状态查找干预
     * 可用于查询"所有未完成"等场景（如 PENDING + ACKNOWLEDGED + EXECUTING）
     */
    List<AgentIntervention> findByStatusInOrderByCreatedAtDesc(List<AgentIntervention.Status> statuses);

    /**
     * 查找Agent待处理的干预
     */
    List<AgentIntervention> findByAgentIdAndStatusOrderByCreatedAtDesc(String agentId, AgentIntervention.Status status);

    /**
     * 统计各状态的干预数量
     */
    @Query("SELECT i.status, COUNT(i) FROM AgentIntervention i GROUP BY i.status")
    List<Object[]> countByStatus();

    /**
     * 统计各类型的干预数量
     */
    @Query("SELECT i.interventionType, COUNT(i) FROM AgentIntervention i GROUP BY i.interventionType")
    List<Object[]> countByType();

    /**
     * 统计各Agent的干预数量
     */
    @Query("SELECT i.agentId, i.agentName, COUNT(i) FROM AgentIntervention i GROUP BY i.agentId, i.agentName")
    List<Object[]> countByAgent();

}
