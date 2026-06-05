package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AlertRecord;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.UserRepository;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.dingtalk.DingTalkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警通知服务
 * 负责发送告警通知
 *
 * 主要功能：
 * - 使用模板发送告警通知
 * - 发送恢复通知
 * - 支持多种通知渠道（站内信、邮件、飞书）
 *
 * 模板编码：
 * - ALERT_EMAIL: 邮件告警模板
 * - ALERT_FEISHU: 飞书告警模板
 * - ALERT_SYSTEM: 站内告警通知模板
 * - RECOVERY_EMAIL: 恢复通知邮件模板
 * - RECOVERY_FEISHU: 恢复通知飞书模板
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeishuBotService feishuService;

    @Autowired
    private DingTalkService dingTalkService;

    @Autowired
    private NotificationTemplateService templateService;

    /**
     * 发送告警通知
     *
     * @param alert 告警记录
     */
    public void sendAlertNotification(AlertRecord alert) {
        log.info("Sending alert notification: {}", alert.getRuleName());

        // 构建变量映射
        Map<String, String> variables = buildAlertVariables(alert);

        // 发送站内通知给所有管理员
        sendInAppNotification("ALERT_SYSTEM", variables, alert.getPriority());

        // 发送邮件通知（如果配置了）
        if (emailService.isEmailEnabled()) {
            sendEmailNotification("ALERT_EMAIL", variables);
        }

        // 发送飞书通知
        sendFeishuNotification("ALERT_FEISHU", variables);

        // 发送钉钉通知
        sendDingTalkNotification("ALERT_DINGTALK", variables);
    }

    /**
     * 发送恢复通知
     *
     * @param ruleName 规则名称
     * @param currentValue 当前值
     */
    public void sendRecoveryNotification(String ruleName, double currentValue) {
        log.info("Sending recovery notification: {}", ruleName);

        // 构建变量映射
        Map<String, String> variables = new HashMap<>();
        variables.put("ruleName", ruleName);
        variables.put("triggerValue", String.format("%.2f", currentValue));
        variables.put("content", "告警已恢复，系统运行正常");

        // 发送站内通知给所有管理员
        sendInAppNotification("ALERT_SYSTEM", variables, "INFO");

        // 发送邮件通知（如果配置了）
        if (emailService.isEmailEnabled()) {
            sendEmailNotification("RECOVERY_EMAIL", variables);
        }

        // 发送飞书通知
        sendFeishuNotification("RECOVERY_FEISHU", variables);

        // 发送钉钉通知
        sendDingTalkNotification("RECOVERY_DINGTALK", variables);
    }

    /**
     * 构建告警变量映射
     *
     * @param alert 告警记录
     * @return 变量映射
     */
    private Map<String, String> buildAlertVariables(AlertRecord alert) {
        Map<String, String> variables = new HashMap<>();
        variables.put("title", alert.getTitle());
        variables.put("content", alert.getDetail());
        variables.put("priority", alert.getPriority());
        variables.put("priorityDesc", alert.getPriorityDescription());
        variables.put("ruleName", alert.getRuleName());
        variables.put("metric", alert.getMetric() != null ? alert.getMetric() : "");
        variables.put("triggerValue", alert.getTriggerValue() != null ? String.format("%.2f", alert.getTriggerValue()) : "N/A");
        variables.put("thresholdValue", alert.getThresholdValue() != null ? String.format("%.2f", alert.getThresholdValue()) : "N/A");
        variables.put("agentId", alert.getAgentId() != null ? alert.getAgentId() : "");
        variables.put("agentName", alert.getAgentName() != null ? alert.getAgentName() : "");
        return variables;
    }

    /**
     * 发送站内通知给所有管理员用户
     *
     * @param templateCode 模板编码
     * @param variables 变量映射
     * @param severity 严重级别
     */
    private void sendInAppNotification(String templateCode, Map<String, String> variables, String severity) {
        try {
            // 渲染模板
            Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
            String title = rendered.get("subject");
            String content = rendered.get("content");

            // 获取所有管理员用户并发送通知
            List<User> admins = userRepository.findByRoleName("ADMIN");
            NotificationType type = mapSeverityToType(severity);

            for (User admin : admins) {
                try {
                    notificationService.sendSystemNotification(admin.getId(), title, content, type);
                } catch (Exception e) {
                    log.error("Failed to send notification to admin {}: {}", admin.getUsername(), e.getMessage());
                }
            }
            log.info("In-app notification sent to {} admins: title={}, severity={}", admins.size(), title, severity);
        } catch (Exception e) {
            log.error("Failed to send in-app notification: {}", e.getMessage());
            // 降级处理：使用默认内容发送
            sendFallbackInAppNotification(variables, severity);
        }
    }

    /**
     * 发送邮件通知给所有管理员
     *
     * @param templateCode 模板编码
     * @param variables 变量映射
     */
    private void sendEmailNotification(String templateCode, Map<String, String> variables) {
        try {
            // 渲染模板
            Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
            String subject = rendered.get("subject");
            String content = rendered.get("content");

            List<User> admins = userRepository.findByRoleName("ADMIN");
            for (User admin : admins) {
                String email = admin.getEmail();
                if (email != null && !email.isEmpty()) {
                    emailService.sendGeneralEmail(email, subject, content);
                    log.debug("Email notification sent to admin: {}", admin.getUsername());
                }
            }
            log.info("Email notification sent to {} admins: subject={}", admins.size(), subject);
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    /**
     * 发送飞书通知
     *
     * @param templateCode 模板编码
     * @param variables 变量映射
     */
    private void sendFeishuNotification(String templateCode, Map<String, String> variables) {
        try {
            if (feishuService.isEnabled()) {
                // 渲染模板
                Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
                String content = rendered.get("content");

                feishuService.sendMessage(content);
                log.info("Feishu notification sent: template={}", templateCode);
            } else {
                log.debug("Feishu notification skipped (not enabled): template={}", templateCode);
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu notification: {}", e.getMessage());
        }
    }

    /**
     * 发送钉钉通知
     *
     * @param templateCode 模板编码
     * @param variables 变量映射
     */
    private void sendDingTalkNotification(String templateCode, Map<String, String> variables) {
        try {
            if (dingTalkService.isEnabled()) {
                // 渲染模板
                Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
                String title = rendered.get("subject");
                String content = rendered.get("content");

                dingTalkService.sendMarkdown(title, content);
                log.info("DingTalk notification sent: template={}", templateCode);
            } else {
                log.debug("DingTalk notification skipped (not enabled): template={}", templateCode);
            }
        } catch (Exception e) {
            log.error("Failed to send DingTalk notification: {}", e.getMessage());
        }
    }

    /**
     * 降级处理：发送默认格式的站内通知
     *
     * @param variables 变量映射
     * @param severity 严重级别
     */
    private void sendFallbackInAppNotification(Map<String, String> variables, String severity) {
        try {
            String ruleName = variables.getOrDefault("ruleName", "未知规则");
            String priorityDesc = variables.getOrDefault("priorityDesc", severity);
            String title = String.format("[%s] %s", priorityDesc, ruleName);
            String content = String.format("告警详情：规则[%s]，级别[%s]，当前值[%s]",
                ruleName, priorityDesc, variables.getOrDefault("triggerValue", "N/A"));

            List<User> admins = userRepository.findByRoleName("ADMIN");
            NotificationType type = mapSeverityToType(severity);

            for (User admin : admins) {
                notificationService.sendSystemNotification(admin.getId(), title, content, type);
            }
        } catch (Exception e) {
            log.error("Failed to send fallback notification: {}", e.getMessage());
        }
    }

    /**
     * 将告警严重级别映射为通知类型
     *
     * @param severity 严重级别
     * @return 通知类型
     */
    private NotificationType mapSeverityToType(String severity) {
        if (severity == null) {
            return NotificationType.SYSTEM;
        }
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "HIGH" -> NotificationType.ERROR;
            case "MEDIUM", "WARNING" -> NotificationType.WARNING;
            default -> NotificationType.INFO;
        };
    }

    /**
     * 检查邮件服务是否启用
     *
     * @return 是否启用
     */
    public boolean isEmailEnabled() {
        return emailService.isEmailEnabled();
    }
}
