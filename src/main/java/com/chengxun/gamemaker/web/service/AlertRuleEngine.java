package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AlertRule;
import com.chengxun.gamemaker.web.entity.AlertRecord;
import com.chengxun.gamemaker.web.repository.AlertRuleRepository;
import com.chengxun.gamemaker.web.repository.AlertRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 告警规则引擎
 * 负责评估告警规则并触发告警
 *
 * 主要功能：
 * - 规则评估
 * - 告警触发
 * - 告警抑制
 * - 告警恢复
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AlertRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleEngine.class);

    @Autowired
    private AlertRuleRepository ruleRepository;

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private MetricsCollectorService metricsService;

    @Autowired
    private AlertNotificationService notificationService;

    /** 已触发的告警（用于抑制重复告警） */
    private final Map<String, LocalDateTime> triggeredAlerts = new HashMap<>();

    /**
     * 定期评估告警规则
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void evaluateRules() {
        log.debug("Evaluating alert rules...");

        List<AlertRule> rules = ruleRepository.findByEnabledTrue();

        for (AlertRule rule : rules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Failed to evaluate rule {}: {}", rule.getId(), e.getMessage());
            }
        }
    }

    /**
     * 评估单个规则
     */
    private void evaluateRule(AlertRule rule) {
        // 检查是否应该抑制
        if (shouldSuppress(rule)) {
            return;
        }

        // 获取指标值
        double currentValue = getMetricValue(rule.getMetric(), null);

        // 评估条件
        boolean triggered = evaluateCondition(rule, currentValue);

        if (triggered) {
            // 触发告警
            triggerAlert(rule, currentValue);
        } else {
            // 检查是否需要恢复
            checkRecovery(rule, currentValue);
        }
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(AlertRule rule, double currentValue) {
        String operator = rule.getOperator();
        double threshold = rule.getThreshold();

        return switch (operator) {
            case ">" -> currentValue > threshold;
            case ">=" -> currentValue >= threshold;
            case "<" -> currentValue < threshold;
            case "<=" -> currentValue <= threshold;
            case "==" -> currentValue == threshold;
            case "!=" -> currentValue != threshold;
            default -> false;
        };
    }

    /**
     * 获取指标值
     */
    private double getMetricValue(String metricName, Map<String, String> tags) {
        // 从MetricsCollectorService获取指标值
        // 这里简化实现，实际应该根据metricName查询对应的指标
        return 0;
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, double currentValue) {
        String alertKey = rule.getId() + ":" + rule.getMetric();

        // 检查是否已经触发过
        if (triggeredAlerts.containsKey(alertKey)) {
            return;
        }

        // 创建告警记录
        AlertRecord alert = new AlertRecord();
        alert.setRuleId(rule.getId());
        alert.setRuleName(rule.getName());
        alert.setMetric(rule.getMetric());
        alert.setTriggerValue(currentValue);
        alert.setThresholdValue(rule.getThreshold());
        alert.setPriority(rule.getPriority());
        alert.setStatus("ALERTING");
        alert.setTitle(rule.getName());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setDetail(String.format("指标 %s 当前值 %.2f %s %.2f",
            rule.getMetric(), currentValue, rule.getOperator(), rule.getThreshold()));

        alertRecordRepository.save(alert);

        // 记录已触发
        triggeredAlerts.put(alertKey, LocalDateTime.now());

        // 发送通知
        notificationService.sendAlertNotification(alert);

        log.warn("Alert triggered: {} - {}", rule.getName(), alert.getDetail());
    }

    /**
     * 检查恢复
     */
    private void checkRecovery(AlertRule rule, double currentValue) {
        String alertKey = rule.getId() + ":" + rule.getMetric();

        // 检查是否有已触发的告警
        if (!triggeredAlerts.containsKey(alertKey)) {
            return;
        }

        // 检查恢复条件（持续一段时间不触发）
        LocalDateTime triggeredTime = triggeredAlerts.get(alertKey);
        int recoveryMinutes = rule.getDurationSeconds() != null ? rule.getDurationSeconds() / 60 : 10; // 默认10分钟
        if (triggeredTime.plusMinutes(recoveryMinutes).isBefore(LocalDateTime.now())) {
            // 恢复告警
            recoverAlert(alertKey, rule, currentValue);
        }
    }

    /**
     * 恢复告警
     */
    private void recoverAlert(String alertKey, AlertRule rule, double currentValue) {
        // 更新告警记录
        List<AlertRecord> alerts = alertRecordRepository.findByStatus("ALERTING");
        for (AlertRecord alert : alerts) {
            if (rule.getId().equals(alert.getRuleId())) {
                alert.setStatus("RESOLVED");
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolution("自动恢复");
                alertRecordRepository.save(alert);
            }
        }

        // 移除已触发记录
        triggeredAlerts.remove(alertKey);

        // 发送恢复通知
        notificationService.sendRecoveryNotification(rule.getName(), currentValue);

        log.info("Alert recovered: {}", rule.getName());
    }

    /**
     * 检查是否应该抑制
     */
    private boolean shouldSuppress(AlertRule rule) {
        String alertKey = rule.getId() + ":" + rule.getMetric();

        if (!triggeredAlerts.containsKey(alertKey)) {
            return false;
        }

        // 检查抑制时间（使用durationSeconds转换为分钟）
        LocalDateTime triggeredTime = triggeredAlerts.get(alertKey);
        int suppressMinutes = rule.getDurationSeconds() != null ? rule.getDurationSeconds() / 60 : 5; // 默认5分钟
        return triggeredTime.plusMinutes(suppressMinutes).isAfter(LocalDateTime.now());
    }

    /**
     * 手动触发告警
     */
    public AlertRecord manualTrigger(Long ruleId, String message) {
        AlertRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Rule not found"));

        AlertRecord alert = new AlertRecord();
        alert.setRuleId(rule.getId());
        alert.setRuleName(rule.getName());
        alert.setMetric(rule.getMetric());
        alert.setPriority(rule.getPriority());
        alert.setStatus("MANUAL");
        alert.setTitle(rule.getName());
        alert.setDetail(message);
        alert.setCreatedAt(LocalDateTime.now());

        AlertRecord saved = alertRecordRepository.save(alert);

        // 发送通知
        notificationService.sendAlertNotification(saved);

        log.info("Manual alert triggered: {}", rule.getName());

        return saved;
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态告警数
        List<Object[]> statusCounts = alertRecordRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 各优先级告警数
        List<Object[]> priorityCounts = alertRecordRepository.countByPriority();
        Map<String, Long> priorityMap = new HashMap<>();
        for (Object[] row : priorityCounts) {
            priorityMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("priorityCounts", priorityMap);

        // 活跃告警数
        long activeAlerts = statusMap.getOrDefault("ALERTING", 0L);
        stats.put("activeAlerts", activeAlerts);

        return stats;
    }
}
