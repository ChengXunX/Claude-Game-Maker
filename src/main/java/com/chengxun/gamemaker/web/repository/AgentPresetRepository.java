package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 预设数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AgentPresetRepository extends JpaRepository<AgentPreset, Long> {

    /** 按角色查找预设 */
    List<AgentPreset> findByRole(String role);

    /** 查找系统内置预设 */
    List<AgentPreset> findBySystemTrue();

    /** 查找用户自定义预设 */
    List<AgentPreset> findBySystemFalse();

    /** 按名称模糊搜索 */
    List<AgentPreset> findByNameContainingIgnoreCase(String name);

    /** 按角色和是否系统内置查找 */
    List<AgentPreset> findByRoleAndSystem(String role, boolean system);
}
