package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问接口
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键查找全局配置（projectId 为 null）
     */
    Optional<SystemConfig> findByConfigKeyAndProjectIdIsNull(String configKey);

    /**
     * 根据配置键查找项目级配置
     */
    Optional<SystemConfig> findByConfigKeyAndProjectId(String configKey, String projectId);

    /**
     * 根据配置键查找配置（兼容旧代码，返回第一个匹配）
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据分组查找所有配置（兼容旧代码）
     */
    List<SystemConfig> findByGroup(String group);

    /**
     * 根据分组查找所有全局配置
     */
    List<SystemConfig> findByGroupAndProjectIdIsNull(String group);

    /**
     * 根据分组查找项目的所有配置（包括全局和项目级）
     */
    List<SystemConfig> findByGroupAndProjectIdOrGroupAndProjectIdIsNull(String group, String projectId, String group2);

    /**
     * 查找项目的所有项目级配置
     */
    List<SystemConfig> findByProjectId(String projectId);

    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 根据配置键删除配置
     */
    void deleteByConfigKey(String configKey);
}
