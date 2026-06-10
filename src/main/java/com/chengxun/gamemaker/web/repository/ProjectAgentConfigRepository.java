package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectAgentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目级Agent配置数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface ProjectAgentConfigRepository extends JpaRepository<ProjectAgentConfig, Long> {

    /**
     * 查找项目的所有Agent配置
     */
    List<ProjectAgentConfig> findByProjectIdAndIsActiveTrue(String projectId);

    /**
     * 查找项目的指定角色Agent配置
     */
    Optional<ProjectAgentConfig> findByProjectIdAndAgentRoleAndIsActiveTrue(String projectId, String agentRole);

    /**
     * 查找项目的指定角色Agent配置（包括未激活的）
     */
    List<ProjectAgentConfig> findByProjectIdAndAgentRole(String projectId, String agentRole);

    /**
     * 检查项目是否已有该角色的配置
     */
    boolean existsByProjectIdAndAgentRoleAndIsActiveTrue(String projectId, String agentRole);
}
