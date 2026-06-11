package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.Notification.NotificationChannel;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import com.chengxun.gamemaker.web.entity.NotificationPreference;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.NotificationRepository;
import com.chengxun.gamemaker.web.repository.NotificationPreferenceRepository;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一通知服务
 * 支持站内信、邮件和飞书三种通知渠道
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final com.chengxun.gamemaker.feishu.FeishuBotService feishuService;
    private final com.chengxun.gamemaker.dingtalk.DingTalkService dingTalkService;
    private final UserRepository userRepository;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService,
                               com.chengxun.gamemaker.feishu.FeishuBotService feishuService,
                               com.chengxun.gamemaker.dingtalk.DingTalkService dingTalkService,
                               UserRepository userRepository,
                               NotificationTemplateService templateService,
                               NotificationPreferenceService preferenceService,
                               NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.feishuService = feishuService;
        this.dingTalkService = dingTalkService;
        this.userRepository = userRepository;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * 发送通知（站内信）
     */
    public void sendSystemNotification(Long userId, String title, String content, NotificationType type) {
        sendNotification(userId, title, content, type, NotificationChannel.SYSTEM, null, null, null);
    }

    /**
     * 发送通知（站内信，带链接）
     */
    public void sendSystemNotification(Long userId, String title, String content, NotificationType type, String link) {
        sendNotification(userId, title, content, type, NotificationChannel.SYSTEM, null, null, link);
    }

    /**
     * 发送通知（指定渠道）
     */
    public void sendNotification(Long userId, String title, String content, NotificationType type,
                                 NotificationChannel channel, String referenceId, String referenceType) {
        sendNotification(userId, title, content, type, channel, referenceId, referenceType, null);
    }

    /**
     * 发送通知（指定渠道，带链接）
     */
    public void sendNotification(Long userId, String title, String content, NotificationType type,
                                 NotificationChannel channel, String referenceId, String referenceType, String link) {
        try {
            // 根据通知类型映射到偏好 key
            String prefKey = mapToPreferenceKey(type);

            // 站内信：始终保存（站内信不过滤偏好，用户可以在列表中看到）
            if (channel == NotificationChannel.SYSTEM || channel == null) {
                // 检查偏好（userId 为 null 时跳过偏好检查，广播场景）
                if (userId != null && !isPreferenceEnabled(userId, prefKey, NotificationPreference.Channel.IN_APP)) {
                    log.debug("User {} disabled IN_APP notifications for {}", userId, prefKey);
                    return;
                }
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType(type);
                notification.setChannel(NotificationChannel.SYSTEM);
                notification.setReferenceId(referenceId);
                notification.setReferenceType(referenceType);
                notification.setLink(link);
                notificationRepository.save(notification);
                log.debug("System notification sent to user {}: {}", userId, title);
            }

            // 发送邮件通知
            if (channel == NotificationChannel.EMAIL && userId != null) {
                if (isPreferenceEnabled(userId, prefKey, NotificationPreference.Channel.EMAIL)) {
                    sendEmailNotification(userId, title, content);
                }
            }

            // 发送飞书通知
            if (channel == NotificationChannel.FEISHU) {
                sendFeishuNotification(title, content);
            }

            // 发送钉钉通知
            if (channel == NotificationChannel.DINGTALK) {
                sendDingTalkNotification(title, content);
            }
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }

    /**
     * 将通知类型枚举映射到偏好 key
     */
    private String mapToPreferenceKey(NotificationType type) {
        if (type == null) return "system";
        return switch (type) {
            case WARNING, ERROR -> "alert";
            case TASK -> "task";
            case APPROVAL -> "approval";
            case AGENT -> "agent_status";
            case SUCCESS -> "performance";
            case INFO -> "project";
            case SYSTEM -> "system";
        };
    }

    /**
     * 发送邮件通知
     * 从用户表获取邮箱地址并发送邮件
     *
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     */
    private void sendEmailNotification(Long userId, String title, String content) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                // 使用通用邮件功能发送通知
                emailService.sendGeneralEmail(email, title, content);
                log.debug("Email notification sent to user {}: {}", userId, title);
            } else {
                log.warn("User {} has no email address configured", userId);
            }
        } else {
            log.warn("User {} not found for email notification", userId);
        }
    }

    /**
     * 发送飞书通知
     */
    private void sendFeishuNotification(String title, String content) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(String.format("**%s**\n\n%s", title, content));
        }
    }

    /**
     * 发送钉钉通知
     */
    private void sendDingTalkNotification(String title, String content) {
        if (dingTalkService.isEnabled()) {
            dingTalkService.sendMarkdown(title, String.format("### %s\n\n%s", title, content));
        }
    }

    /**
     * 获取用户通知列表
     */
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * 获取用户未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * 获取用户未读通知列表
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此通知");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    /**
     * 标记用户所有通知为已读
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此通知");
        }

        notificationRepository.delete(notification);
    }

    /**
     * 定时清理过期通知（保留30天）
     */
    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点执行
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(30);
        // 需要按用户分别清理，这里简化处理
        log.info("Notification cleanup task executed");
    }

    /**
     * 发送任务通知
     */
    public void sendTaskNotification(Long userId, String taskTitle, String message) {
        sendSystemNotification(userId, "任务通知: " + taskTitle, message, NotificationType.TASK);
    }

    /**
     * 发送Agent通知
     */
    public void sendAgentNotification(Long userId, String agentName, String message) {
        sendSystemNotification(userId, "Agent通知: " + agentName, message, NotificationType.AGENT);
    }

    /**
     * 发送系统通知
     */
    public void sendSystemAlert(Long userId, String title, String message) {
        sendSystemNotification(userId, title, message, NotificationType.SYSTEM);
    }

    /**
     * 使用模板发送通知
     *
     * @param userId 用户ID
     * @param templateCode 模板编码
     * @param variables 变量映射
     * @param type 通知类型
     */
    public void sendNotificationByTemplate(Long userId, String templateCode, Map<String, String> variables, NotificationType type) {
        try {
            Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
            String title = rendered.get("subject");
            String content = rendered.get("content");
            sendSystemNotification(userId, title, content, type);
        } catch (Exception e) {
            log.error("Failed to send notification by template {}: {}", templateCode, e.getMessage());
        }
    }

    /**
     * 使用模板发送任务通知
     *
     * @param userId 用户ID
     * @param taskTitle 任务标题
     * @param taskResult 任务结果
     */
    public void sendTaskNotificationByTemplate(Long userId, String taskTitle, String taskResult) {
        Map<String, String> variables = Map.of(
            "taskTitle", taskTitle,
            "taskResult", taskResult != null ? taskResult : "",
            "content", taskResult != null ? taskResult : ""
        );
        sendNotificationByTemplate(userId, "TASK_SYSTEM", variables, NotificationType.TASK);
    }

    /**
     * 使用模板发送Agent通知
     *
     * @param userId 用户ID
     * @param agentName Agent名称
     * @param message 通知内容
     */
    public void sendAgentNotificationByTemplate(Long userId, String agentName, String message) {
        Map<String, String> variables = Map.of(
            "agentName", agentName,
            "content", message
        );
        sendNotificationByTemplate(userId, "AGENT_SYSTEM", variables, NotificationType.AGENT);
    }

    /**
     * 检查用户是否启用了指定通知类型和渠道
     * 同时检查免打扰时间段：如果当前时间在免打扰窗口内，返回 false
     *
     * @param userId 用户ID
     * @param notificationType 通知类型
     * @param channel 渠道
     * @return 是否启用
     */
    private boolean isPreferenceEnabled(Long userId, String notificationType,
                                         NotificationPreference.Channel channel) {
        if (userId == null) return true;
        try {
            // 先检查是否启用
            if (!preferenceService.isNotificationEnabled(userId, notificationType, channel)) {
                return false;
            }
            // 再检查免打扰时间段
            return !isInQuietPeriod(userId, notificationType, channel);
        } catch (Exception e) {
            return true; // 出错时默认启用
        }
    }

    /**
     * 检查当前时间是否在用户的免打扰时间段内
     *
     * @param userId 用户ID
     * @param notificationType 通知类型
     * @param channel 渠道
     * @return true 表示当前在免打扰时间段内，不应发送通知
     */
    private boolean isInQuietPeriod(Long userId, String notificationType,
                                     NotificationPreference.Channel channel) {
        try {
            var prefOpt = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(
                userId, notificationType, channel);
            if (prefOpt.isEmpty()) return false;

            NotificationPreference pref = prefOpt.get();
            if (!pref.isDoNotDisturb()) return false;

            String quietStart = pref.getQuietStart();
            String quietEnd = pref.getQuietEnd();
            if (quietStart == null || quietEnd == null || quietStart.isEmpty() || quietEnd.isEmpty()) {
                return false;
            }

            // 解析免打扰时间段（HH:mm 格式）
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = java.time.LocalTime.parse(quietStart);
            java.time.LocalTime end = java.time.LocalTime.parse(quietEnd);

            // 支持跨午夜（如 22:00 - 08:00）
            if (start.isBefore(end) || start.equals(end)) {
                // 正常时段：start <= now < end
                return !now.isBefore(start) && now.isBefore(end);
            } else {
                // 跨午夜：now >= start || now < end
                return !now.isBefore(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            log.debug("检查免打扰时间段失败: userId={}, type={}, channel={}, error={}",
                userId, notificationType, channel, e.getMessage());
            return false;
        }
    }

    /**
     * 向所有管理员发送通知（自动检查偏好）
     *
     * @param notificationType 通知类型标识
     * @param templateCode 模板编码
     * @param variables 变量映射
     * @param type 通知类型
     */
    public void notifyAdmins(String notificationType, String templateCode,
                              Map<String, String> variables, NotificationType type) {
        notifyAdmins(notificationType, templateCode, variables, type, null);
    }

    /**
     * 向所有管理员发送通知（检查偏好，支持多渠道，支持跳转链接）
     *
     * @param notificationType 通知类型标识（用于偏好检查）
     * @param templateCode 模板编码基础名
     * @param variables 变量映射
     * @param type 通知类型
     * @param link 跳转链接（可选）
     */
    public void notifyAdmins(String notificationType, String templateCode,
                              Map<String, String> variables, NotificationType type, String link) {
        List<User> admins = userRepository.findByRoleName("ADMIN");
        log.info("notifyAdmins called: type={}, templateCode={}, adminCount={}", notificationType, templateCode, admins.size());

        for (User admin : admins) {
            try {
                // 站内信 - 系统级通知始终发送，不检查用户偏好
                String title = variables.getOrDefault("title", "通知");
                String content = variables.getOrDefault("content", "");
                try {
                    Map<String, String> rendered = templateService.renderTemplate(templateCode + "_SYSTEM", variables);
                    title = rendered.getOrDefault("subject", title);
                    content = rendered.getOrDefault("content", content);
                    log.debug("Rendered SYSTEM template for admin {}: subject={}", admin.getUsername(), title);
                } catch (Exception te) {
                    try {
                        Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
                        title = rendered.getOrDefault("subject", title);
                        content = rendered.getOrDefault("content", content);
                        log.debug("Rendered template {} for admin {}: subject={}", templateCode, admin.getUsername(), title);
                    } catch (Exception te2) {
                        log.debug("Template {} not found, using variables", templateCode);
                    }
                }
                sendSystemNotification(admin.getId(), title, content, type, link);
                log.info("站内信通知已发送给管理员: {}", admin.getUsername());

                // 邮件
                if (emailService.isEmailEnabled() &&
                    isPreferenceEnabled(admin.getId(), notificationType, NotificationPreference.Channel.EMAIL)) {
                    String email = admin.getEmail();
                    if (email != null && !email.isEmpty()) {
                        String emailSubject = variables.getOrDefault("title", "通知");
                        String emailContent = variables.getOrDefault("content", "");
                        try {
                            Map<String, String> rendered = templateService.renderTemplate(templateCode + "_EMAIL", variables);
                            emailSubject = rendered.getOrDefault("subject", emailSubject);
                            emailContent = rendered.getOrDefault("content", emailContent);
                        } catch (Exception te) {
                            try {
                                Map<String, String> rendered = templateService.renderTemplate(templateCode, variables);
                                emailSubject = rendered.getOrDefault("subject", emailSubject);
                                emailContent = rendered.getOrDefault("content", emailContent);
                            } catch (Exception te2) {
                                // 模板不存在，使用变量中的内容
                            }
                        }
                        emailService.sendGeneralEmail(email, emailSubject, emailContent);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to send notification to admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // 飞书（全局，不按用户偏好）
        try {
            if (feishuService.isEnabled()) {
                Map<String, String> rendered = templateService.renderTemplate(templateCode + "_FEISHU", variables);
                if (!rendered.isEmpty() && rendered.get("content") != null && !rendered.get("content").isEmpty()) {
                    feishuService.sendMessage(rendered.get("content"));
                }
            }
        } catch (Exception e) {
            log.debug("Feishu notification failed: {}", e.getMessage());
        }

        // 钉钉（全局，不按用户偏好）
        try {
            if (dingTalkService.isEnabled()) {
                Map<String, String> rendered = templateService.renderTemplate(templateCode + "_DINGTALK", variables);
                if (!rendered.isEmpty() && rendered.get("content") != null && !rendered.get("content").isEmpty()) {
                    dingTalkService.sendMarkdown(rendered.get("subject"), rendered.get("content"));
                }
            }
        } catch (Exception e) {
            log.debug("DingTalk notification failed: {}", e.getMessage());
        }
    }

    /**
     * 向指定用户发送通知（检查偏好，支持多渠道）
     *
     * @param userId 用户ID
     * @param notificationType 通知类型标识
     * @param templateCode 模板编码基础名
     * @param variables 变量映射
     * @param type 通知类型
     */
    public void notifyUser(Long userId, String notificationType, String templateCode,
                            Map<String, String> variables, NotificationType type) {
        notifyUser(userId, notificationType, templateCode, variables, type, null);
    }

    /**
     * 向指定用户发送通知（检查偏好，支持多渠道，支持跳转链接）
     *
     * @param userId 用户ID
     * @param notificationType 通知类型标识
     * @param templateCode 模板编码基础名
     * @param variables 变量映射
     * @param type 通知类型
     * @param link 跳转链接（可选）
     */
    public void notifyUser(Long userId, String notificationType, String templateCode,
                            Map<String, String> variables, NotificationType type, String link) {
        try {
            // 站内信
            if (isPreferenceEnabled(userId, notificationType, NotificationPreference.Channel.IN_APP)) {
                Map<String, String> rendered = templateService.renderTemplate(templateCode + "_SYSTEM", variables);
                if (rendered.isEmpty() || rendered.get("content").isEmpty()) {
                    rendered = templateService.renderTemplate(templateCode, variables);
                }
                String title = rendered.getOrDefault("subject", variables.getOrDefault("title", "通知"));
                String content = rendered.getOrDefault("content", "");
                sendSystemNotification(userId, title, content, type, link);
            }

            // 邮件
            if (emailService.isEmailEnabled() &&
                isPreferenceEnabled(userId, notificationType, NotificationPreference.Channel.EMAIL)) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    String email = userOpt.get().getEmail();
                    if (email != null && !email.isEmpty()) {
                        Map<String, String> rendered = templateService.renderTemplate(templateCode + "_EMAIL", variables);
                        if (rendered.isEmpty() || rendered.get("content").isEmpty()) {
                            rendered = templateService.renderTemplate(templateCode, variables);
                        }
                        emailService.sendGeneralEmail(email, rendered.get("subject"), rendered.get("content"));
                    }
                }
            }

            // 飞书
            if (feishuService.isEnabled() &&
                isPreferenceEnabled(userId, notificationType, NotificationPreference.Channel.FEISHU)) {
                Map<String, String> rendered = templateService.renderTemplate(templateCode + "_FEISHU", variables);
                if (rendered.isEmpty() || rendered.get("content").isEmpty()) {
                    rendered = templateService.renderTemplate(templateCode, variables);
                }
                String content = rendered.getOrDefault("content", "");
                if (content != null && !content.isEmpty()) {
                    feishuService.sendMessage(content);
                }
            }

            // 钉钉
            if (dingTalkService.isEnabled() &&
                isPreferenceEnabled(userId, notificationType, NotificationPreference.Channel.DINGTALK)) {
                Map<String, String> rendered = templateService.renderTemplate(templateCode + "_DINGTALK", variables);
                if (rendered.isEmpty() || rendered.get("content").isEmpty()) {
                    rendered = templateService.renderTemplate(templateCode, variables);
                }
                String subject = rendered.getOrDefault("subject", "通知");
                String content = rendered.getOrDefault("content", "");
                if (content != null && !content.isEmpty()) {
                    dingTalkService.sendMarkdown(subject, content);
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify user {}: {}", userId, e.getMessage());
        }
    }
}
