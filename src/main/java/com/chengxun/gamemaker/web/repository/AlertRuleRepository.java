package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警规则数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 查找所有启用的告警规则
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * 根据规则类型查找
     */
    List<AlertRule> findByRuleType(String ruleType);

    /**
     * 根据优先级查找
     */
    List<AlertRule> findByPriority(String priority);

    /**
     * 根据规则类型和启用状态查找
     */
    List<AlertRule> findByRuleTypeAndEnabled(String ruleType, boolean enabled);
}
