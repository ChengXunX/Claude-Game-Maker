package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentMcpBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent MCP 绑定仓库
 */
@Repository
public interface AgentMcpBindingRepository extends JpaRepository<AgentMcpBinding, Long> {

    /** 获取某角色在某项目下的所有绑定 */
    List<AgentMcpBinding> findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(
        String agentRole, String projectId);

    /** 获取某 Server 的所有绑定 */
    List<AgentMcpBinding> findByServerIdAndEnabledTrue(Long serverId);

    /** 获取某 Tool 的所有绑定 */
    List<AgentMcpBinding> findByToolIdAndEnabledTrue(Long toolId);

    /** 查找特定绑定 */
    Optional<AgentMcpBinding> findByAgentRoleAndProjectIdAndServerIdAndToolId(
        String agentRole, String projectId, Long serverId, Long toolId);

    /** 检查是否已绑定 */
    @Query("SELECT COUNT(b) > 0 FROM AgentMcpBinding b " +
           "WHERE b.agentRole = :role AND b.projectId = :projectId " +
           "AND b.serverId = :serverId AND (b.toolId = :toolId OR b.toolId IS NULL) " +
           "AND b.enabled = true")
    boolean existsByRoleAndProjectAndServer(@Param("role") String agentRole,
                                             @Param("projectId") String projectId,
                                             @Param("serverId") Long serverId,
                                             @Param("toolId") Long toolId);

    /** 删除某 Server 的所有绑定 */
    void deleteByServerId(Long serverId);

    /** 删除某 Agent 角色的所有绑定 */
    void deleteByAgentRoleAndProjectId(String agentRole, String projectId);
}
