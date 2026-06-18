package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent 能力仓库
 * 提供能力定义的 CRUD 和查询方法
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface AgentCapabilityRepository extends JpaRepository<AgentCapability, Long> {

    /**
     * 获取某角色的所有启用能力（按优先级排序）
     */
    List<AgentCapability> findByAgentRoleAndEnabledTrueOrderByPriorityAsc(String agentRole);

    /**
     * 获取某角色的全局默认能力（projectId 为空）
     */
    List<AgentCapability> findByAgentRoleAndProjectIdIsNullAndEnabledTrueOrderByPriorityAsc(String agentRole);

    /**
     * 获取某角色在某项目下的能力（项目级覆盖）
     */
    List<AgentCapability> findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(String agentRole, String projectId);

    /**
     * 获取某角色的所有能力（包括禁用的，管理界面用）
     */
    List<AgentCapability> findByAgentRoleOrderByPriorityAsc(String agentRole);

    /**
     * 获取所有能力（管理界面用）
     */
    List<AgentCapability> findAllByOrderByAgentRoleAscPriorityAsc();

    /**
     * 按分类查询能力
     */
    List<AgentCapability> findByCategoryAndEnabledTrueOrderByPriorityAsc(String category);

    /**
     * 按项目查询所有能力
     */
    List<AgentCapability> findByProjectIdOrderByAgentRoleAscPriorityAsc(String projectId);

    /**
     * 查找特定能力
     */
    Optional<AgentCapability> findByCapabilityNameAndAgentRoleAndProjectId(String capabilityName, String agentRole, String projectId);

    /**
     * 查找全局默认能力（可能存在重复记录，返回第一条）
     */
    List<AgentCapability> findByCapabilityNameAndAgentRoleAndProjectIdIsNull(String capabilityName, String agentRole);

    /**
     * 检查能力是否存在且启用
     */
    @Query("SELECT COUNT(c) > 0 FROM AgentCapability c WHERE c.agentRole = :role AND c.capabilityName = :name AND c.enabled = true")
    boolean existsByRoleAndNameEnabled(@Param("role") String agentRole, @Param("name") String capabilityName);

    /**
     * 按分类统计能力数量
     */
    @Query("SELECT c.category, COUNT(c) FROM AgentCapability c WHERE c.enabled = true GROUP BY c.category")
    List<Object[]> countByCategory();

    /**
     * 按角色统计能力数量
     */
    @Query("SELECT c.agentRole, COUNT(c) FROM AgentCapability c WHERE c.enabled = true GROUP BY c.agentRole")
    List<Object[]> countByAgentRole();

    /**
     * 批量启用/禁用某角色的能力
     */
    @Modifying
    @Query("UPDATE AgentCapability c SET c.enabled = :enabled, c.updatedAt = CURRENT_TIMESTAMP WHERE c.agentRole = :role")
    int updateEnabledByAgentRole(@Param("role") String agentRole, @Param("enabled") boolean enabled);

    /**
     * 批量启用/禁用某项目的能力
     */
    @Modifying
    @Query("UPDATE AgentCapability c SET c.enabled = :enabled, c.updatedAt = CURRENT_TIMESTAMP WHERE c.projectId = :projectId")
    int updateEnabledByProjectId(@Param("projectId") String projectId, @Param("enabled") boolean enabled);

    /**
     * 删除某项目的所有能力覆盖
     */
    void deleteByProjectId(String projectId);

    /**
     * 查找需要审批的能力
     */
    List<AgentCapability> findByAgentRoleAndRequiresApprovalTrueAndEnabledTrue(String agentRole);
}
