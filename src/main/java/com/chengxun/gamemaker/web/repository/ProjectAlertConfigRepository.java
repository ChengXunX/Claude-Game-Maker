package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目告警配置数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ProjectAlertConfigRepository extends JpaRepository<ProjectAlertConfig, Long> {

    /** 查找项目的所有告警配置 */
    List<ProjectAlertConfig> findByProjectId(String projectId);

    /** 查找项目的特定规则配置 */
    Optional<ProjectAlertConfig> findByProjectIdAndRuleId(String projectId, Long ruleId);

    /** 查找启用某规则的所有项目配置 */
    List<ProjectAlertConfig> findByRuleIdAndEnabledTrue(Long ruleId);

    /** 检查项目是否配置了某规则 */
    boolean existsByProjectIdAndRuleId(String projectId, Long ruleId);
}
