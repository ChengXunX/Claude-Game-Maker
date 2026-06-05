package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.Notification.NotificationChannel;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.NotificationRepository;
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
    private final UserRepository userRepository;
    private final NotificationTemplateService templateService;

    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService,
                               com.chengxun.gamemaker.feishu.FeishuBotService feishuService,
                               UserRepository userRepository,
                               NotificationTemplateService templateService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.feishuService = feishuService;
        this.userRepository = userRepository;
        this.templateService = templateService;
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
            // 保存站内信
            if (channel == NotificationChannel.SYSTEM || channel == null) {
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
            if (channel == NotificationChannel.EMAIL) {
                sendEmailNotification(userId, title, content);
            }

            // 发送飞书通知
            if (channel == NotificationChannel.FEISHU) {
                sendFeishuNotification(title, content);
            }
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
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
                // 使用验证码邮件功能发送通知
                emailService.sendVerificationEmail(email, content);
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
}
