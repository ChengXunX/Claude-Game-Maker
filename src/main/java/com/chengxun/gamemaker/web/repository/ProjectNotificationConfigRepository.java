package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目通知配置数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ProjectNotificationConfigRepository extends JpaRepository<ProjectNotificationConfig, Long> {

    /** 查找项目的所有通知配置 */
    List<ProjectNotificationConfig> findByProjectId(String projectId);

    /** 查找项目的特定模板配置 */
    Optional<ProjectNotificationConfig> findByProjectIdAndTemplateCode(String projectId, String templateCode);

    /** 查找启用某模板的所有项目配置 */
    List<ProjectNotificationConfig> findByTemplateCodeAndEnabledTrue(String templateCode);

    /** 检查项目是否配置了某模板 */
    boolean existsByProjectIdAndTemplateCode(String projectId, String templateCode);
}
