package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AlertRecord;
import com.chengxun.gamemaker.web.entity.AlertRule;
import com.chengxun.gamemaker.web.repository.AlertRecordRepository;
import com.chengxun.gamemaker.web.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警服务
 * 负责告警规则管理、告警触发、告警处理
 *
 * 主要功能：
 * - 管理告警规则（CRUD）
 * - 监控指标并触发告警
 * - 处理告警（确认、解决、忽略）
 * - 发送告警通知
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private NotificationService notificationService;

    // ===== 告警规则管理 =====

    /**
     * 创建告警规则
     */
    public AlertRule createRule(AlertRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * 更新告警规则
     */
    public AlertRule updateRule(AlertRule rule) {
        rule.setUpdatedAt(LocalDateTime.now());
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Updated alert rule: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * 删除告警规则
     */
    public void deleteRule(Long ruleId) {
        alertRuleRepository.deleteById(ruleId);
        log.info("Deleted alert rule: {}", ruleId);
    }

    /**
     * 获取告警规则
     */
    public AlertRule getRule(Long ruleId) {
        return alertRuleRepository.findById(ruleId).orElse(null);
    }

    /**
     * 获取所有告警规则
     */
    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }

    /**
     * 获取启用的告警规则
     */
    public List<AlertRule> getEnabledRules() {
        return alertRuleRepository.findByEnabledTrue();
    }

    /**
     * 启用/禁用规则
     */
    public void toggleRule(Long ruleId, boolean enabled) {
        alertRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setEnabled(enabled);
            rule.setUpdatedAt(LocalDateTime.now());
            alertRuleRepository.save(rule);
            log.info("Alert rule {} {}", ruleId, enabled ? "enabled" : "disabled");
        });
    }

    // ===== 告警触发 =====

    /**
     * 检查指标并触发告警
     * @param metric 指标名称
     * @param value 当前值
     * @param agentId 相关Agent ID（可选）
     * @param agentName 相关Agent名称（可选）
     * @param projectId 相关项目ID（可选）
     */
    public void checkAndTrigger(String metric, double value, String agentId, String agentName, String projectId) {
        List<AlertRule> rules = alertRuleRepository.findByRuleTypeAndEnabled(metric, true);

        for (AlertRule rule : rules) {
            if (shouldTrigger(rule, value)) {
                triggerAlert(rule, value, agentId, agentName, projectId);
            }
        }
    }

    /**
     * 判断是否应该触发告警
     */
    private boolean shouldTrigger(AlertRule rule, double value) {
        return switch (rule.getOperator()) {
            case "GT" -> value > rule.getThreshold();
            case "GTE" -> value >= rule.getThreshold();
            case "LT" -> value < rule.getThreshold();
            case "LTE" -> value <= rule.getThreshold();
            case "EQ" -> Math.abs(value - rule.getThreshold()) < 0.001;
            case "NEQ" -> Math.abs(value - rule.getThreshold()) >= 0.001;
            default -> false;
        };
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, double value, String agentId, String agentName, String projectId) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getName());
        record.setPriority(rule.getPriority());
        record.setTitle(generateAlertTitle(rule, value));
        record.setDetail(generateAlertDetail(rule, value, agentId, agentName));
        record.setTriggerValue(value);
        record.setThresholdValue(rule.getThreshold());
        record.setMetric(rule.getMetric());
        record.setAgentId(agentId);
        record.setAgentName(agentName);
        record.setProjectId(projectId);
        record.setStatus(AlertRecord.Status.PENDING.name());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        AlertRecord saved = alertRecordRepository.save(record);
        log.warn("Alert triggered: {} (Priority: {}, Value: {}, Threshold: {})",
            rule.getName(), rule.getPriority(), value, rule.getThreshold());

        // 发送通知
        sendNotification(saved, rule);
    }

    /**
     * 生成告警标题
     */
    private String generateAlertTitle(AlertRule rule, double value) {
        return String.format("[%s] %s: %.2f %s %.2f",
            rule.getPriority(),
            rule.getName(),
            value,
            getOperatorSymbol(rule.getOperator()),
            rule.getThreshold());
    }

    /**
     * 生成告警详情
     */
    private String generateAlertDetail(AlertRule rule, double value, String agentId, String agentName) {
        StringBuilder sb = new StringBuilder();
        sb.append("告警规则: ").append(rule.getName()).append("\n");
        sb.append("规则描述: ").append(rule.getDescription()).append("\n");
        sb.append("监控指标: ").append(rule.getMetric()).append("\n");
        sb.append("当前值: ").append(String.format("%.2f", value)).append("\n");
        sb.append("阈值: ").append(String.format("%.2f", rule.getThreshold())).append("\n");
        sb.append("触发时间: ").append(LocalDateTime.now()).append("\n");

        if (agentId != null) {
            sb.append("相关Agent: ").append(agentName).append(" (").append(agentId).append(")\n");
        }

        return sb.toString();
    }

    /**
     * 获取运算符符号
     */
    private String getOperatorSymbol(String operator) {
        return switch (operator) {
            case "GT" -> ">";
            case "GTE" -> ">=";
            case "LT" -> "<";
            case "LTE" -> "<=";
            case "EQ" -> "==";
            case "NEQ" -> "!=";
            default -> operator;
        };
    }

    /**
     * 发送告警通知
     */
    private void sendNotification(AlertRecord record, AlertRule rule) {
        try {
            String method = rule.getNotifyMethod();

            if ("SYSTEM".equals(method) || "MULTI".equals(method)) {
                // 发送系统通知（广播给所有管理员）
                // 这里简化处理，实际应该查询管理员用户列表
                log.info("System alert: {}", record.getTitle());
            }

            if ("FEISHU".equals(method) || "MULTI".equals(method)) {
                // 发送飞书通知
                notificationService.sendNotification(
                    null,
                    "告警: " + record.getTitle(),
                    record.getDetail(),
                    null,
                    com.chengxun.gamemaker.web.entity.Notification.NotificationChannel.FEISHU,
                    null,
                    null
                );
            }

            record.markNotified();
            alertRecordRepository.save(record);

        } catch (Exception e) {
            log.error("Failed to send alert notification", e);
            record.markNotifyFailed();
            alertRecordRepository.save(record);
        }
    }

    // ===== 告警处理 =====

    /**
     * 确认告警
     */
    public void acknowledgeAlert(Long alertId, String userId) {
        alertRecordRepository.findById(alertId).ifPresent(record -> {
            record.acknowledge(userId);
            alertRecordRepository.save(record);
            log.info("Alert {} acknowledged by {}", alertId, userId);
        });
    }

    /**
     * 开始处理告警
     */
    public void startProgress(Long alertId, String userId) {
        alertRecordRepository.findById(alertId).ifPresent(record -> {
            record.startProgress(userId);
            alertRecordRepository.save(record);
            log.info("Alert {} in progress by {}", alertId, userId);
        });
    }

    /**
     * 解决告警
     */
    public void resolveAlert(Long alertId, String userId, String resolution) {
        alertRecordRepository.findById(alertId).ifPresent(record -> {
            record.resolve(userId, resolution);
            alertRecordRepository.save(record);
            log.info("Alert {} resolved by {}", alertId, userId);
        });
    }

    /**
     * 忽略告警
     */
    public void ignoreAlert(Long alertId, String userId) {
        alertRecordRepository.findById(alertId).ifPresent(record -> {
            record.ignore(userId);
            alertRecordRepository.save(record);
            log.info("Alert {} ignored by {}", alertId, userId);
        });
    }

    /**
     * 批量确认所有待处理告警
     *
     * @param userId 操作用户ID
     * @return 确认的告警数量
     */
    public int acknowledgeAllPending(String userId) {
        List<AlertRecord> pendingAlerts = getPendingAlerts();
        int count = 0;
        for (AlertRecord alert : pendingAlerts) {
            try {
                alert.acknowledge(userId);
                alertRecordRepository.save(alert);
                count++;
            } catch (Exception e) {
                log.error("Failed to acknowledge alert {}: {}", alert.getId(), e.getMessage());
            }
        }
        log.info("Batch acknowledged {} alerts by {}", count, userId);
        return count;
    }

    /**
     * 导出告警记录为CSV格式
     *
     * @param status 告警状态过滤（可选）
     * @param priority 优先级过滤（可选）
     * @param days 最近N天
     * @return CSV内容
     */
    public String exportAlertsToCsv(String status, String priority, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AlertRecord> alerts;

        if (status != null && !status.isEmpty()) {
            alerts = alertRecordRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (priority != null && !priority.isEmpty()) {
            alerts = alertRecordRepository.findByPriority(priority);
        } else {
            alerts = alertRecordRepository.findByTimeRange(since, LocalDateTime.now());
        }

        StringBuilder csv = new StringBuilder();
        // CSV头
        csv.append("ID,规则名称,标题,优先级,状态,触发值,阈值,指标,Agent,创建时间,解决时间\n");

        // CSV数据
        for (AlertRecord alert : alerts) {
            csv.append(alert.getId()).append(",");
            csv.append(escapeCsv(alert.getRuleName())).append(",");
            csv.append(escapeCsv(alert.getTitle())).append(",");
            csv.append(alert.getPriority()).append(",");
            csv.append(alert.getStatus()).append(",");
            csv.append(alert.getTriggerValue() != null ? alert.getTriggerValue() : "").append(",");
            csv.append(alert.getThresholdValue() != null ? alert.getThresholdValue() : "").append(",");
            csv.append(escapeCsv(alert.getMetric())).append(",");
            csv.append(escapeCsv(alert.getAgentName())).append(",");
            csv.append(alert.getCreatedAt()).append(",");
            csv.append(alert.getResolvedAt() != null ? alert.getResolvedAt() : "").append("\n");
        }

        return csv.toString();
    }

    /**
     * CSV字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ===== 告警查询 =====

    /**
     * 获取告警记录
     */
    public AlertRecord getAlert(Long alertId) {
        return alertRecordRepository.findById(alertId).orElse(null);
    }

    /**
     * 获取所有告警记录（分页）
     */
    public Page<AlertRecord> getAllAlerts(int page, int size) {
        return alertRecordRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 根据状态获取告警
     */
    public List<AlertRecord> getAlertsByStatus(String status) {
        return alertRecordRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 根据优先级获取告警
     */
    public List<AlertRecord> getAlertsByPriority(String priority) {
        return alertRecordRepository.findByPriority(priority);
    }

    /**
     * 根据Agent获取告警
     */
    public List<AlertRecord> getAlertsByAgent(String agentId) {
        return alertRecordRepository.findByAgentId(agentId);
    }

    /**
     * 获取待处理的告警
     */
    public List<AlertRecord> getPendingAlerts() {
        return alertRecordRepository.findByStatusOrderByCreatedAtDesc(AlertRecord.Status.PENDING.name());
    }

    /**
     * 获取高优先级告警
     */
    public List<AlertRecord> getHighPriorityAlerts() {
        List<AlertRecord> highPriority = new ArrayList<>(alertRecordRepository.findByPriority("HIGH"));
        List<AlertRecord> critical = alertRecordRepository.findByPriority("CRITICAL");
        highPriority.addAll(critical);
        return highPriority.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态统计
        List<Object[]> statusCounts = alertRecordRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 各优先级统计
        List<Object[]> priorityCounts = alertRecordRepository.countByPriority();
        Map<String, Long> priorityMap = new HashMap<>();
        for (Object[] row : priorityCounts) {
            priorityMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("priorityCounts", priorityMap);

        // 最近24小时告警数
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Long recentCount = alertRecordRepository.countSince(since);
        stats.put("last24Hours", recentCount);

        // 待处理告警数
        Long pendingCount = statusMap.getOrDefault("PENDING", 0L);
        stats.put("pending", pendingCount);

        return stats;
    }

    /**
     * 获取告警趋势（按天）
     */
    public Map<String, Long> getAlertTrend(int days) {
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<AlertRecord> dayAlerts = alertRecordRepository.findByTimeRange(dayStart, dayEnd);
            String dateKey = dayStart.toLocalDate().toString();
            trend.put(dateKey, (long) dayAlerts.size());
        }

        return trend;
    }
}
