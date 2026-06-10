package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.NotificationPreference;
import com.chengxun.gamemaker.web.entity.NotificationPreference.Channel;
import com.chengxun.gamemaker.web.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知偏好服务
 * 管理用户的通知接收偏好配置
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    /**
     * 通知类型定义
     * key: 通知类型标识
     * value: 通知类型描述
     * requiredPermission: 查看/配置该通知类型所需的权限（null 表示所有用户可见）
     */
    public static class NotificationType {
        private final String key;
        private final String label;
        private final String description;
        private final String category;
        private final String requiredPermission;
        private final boolean defaultEnabled;

        public NotificationType(String key, String label, String description,
                               String category, String requiredPermission, boolean defaultEnabled) {
            this.key = key;
            this.label = label;
            this.description = description;
            this.category = category;
            this.requiredPermission = requiredPermission;
            this.defaultEnabled = defaultEnabled;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getRequiredPermission() { return requiredPermission; }
        public boolean isDefaultEnabled() { return defaultEnabled; }
    }

    /** 所有通知类型定义（仅保留系统实际会发送的类型） */
    private static final List<NotificationType> NOTIFICATION_TYPES = List.of(
        // 告警（AlertService / AlertNotificationService 发送）
        new NotificationType("alert", "监控告警", "监控告警触发或解除通知", "告警", "PERM_system:monitor", true),

        // 审批（ApprovalService / RecruitmentApprovalService 发送）
        new NotificationType("approval", "审批通知", "审批请求、通过、拒绝通知", "审批", null, true),

        // Agent（Agent 生命周期事件）
        new NotificationType("agent_status", "Agent 状态", "Agent 启动、停止、异常等状态变化", "Agent", "PERM_agents:view", true),

        // 绩效（PerformanceManagementService 发送）
        new NotificationType("performance", "绩效通知", "绩效考核、离职审批等通知", "绩效", null, true),

        // 项目（ProjectImportService / GameEvaluationService 发送）
        new NotificationType("project", "项目通知", "项目导入、评估完成等通知", "项目", "PERM_projects:view", true),

        // 系统（系统维护、Token 耗尽等）
        new NotificationType("system", "系统通知", "系统维护、Token 耗尽等通知", "系统", null, true)
    );

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    // ===== 查询 =====

    /**
     * 获取用户的所有通知偏好
     */
    public List<NotificationPreference> getUserPreferences(Long userId) {
        return preferenceRepository.findByUserIdOrderByNotificationTypeAscChannelAsc(userId);
    }

    /**
     * 获取用户可见的通知类型（根据权限过滤）
     */
    public List<NotificationType> getVisibleTypes(Set<String> userPermissions) {
        return NOTIFICATION_TYPES.stream()
            .filter(type -> type.getRequiredPermission() == null
                || userPermissions.contains(type.getRequiredPermission()))
            .collect(Collectors.toList());
    }

    /**
     * 获取所有通知类型（管理员用）
     */
    public List<NotificationType> getAllTypes() {
        return NOTIFICATION_TYPES;
    }

    /**
     * 获取所有可用渠道
     */
    public Channel[] getAvailableChannels() {
        return Channel.values();
    }

    /**
     * 检查用户是否启用了某通知
     */
    public boolean isNotificationEnabled(Long userId, String notificationType, Channel channel) {
        return preferenceRepository.isEnabled(userId, notificationType, channel);
    }

    /**
     * 获取用户启用了某渠道的通知类型列表
     */
    public List<String> getEnabledTypes(Long userId, Channel channel) {
        return preferenceRepository.findByUserIdAndChannel(userId, channel).stream()
            .filter(NotificationPreference::isEnabled)
            .map(NotificationPreference::getNotificationType)
            .collect(Collectors.toList());
    }

    // ===== 设置 =====

    /**
     * 更新用户通知偏好
     */
    @Transactional(readOnly = false)
    public NotificationPreference updatePreference(Long userId, String notificationType,
                                                     Channel channel, boolean enabled) {
        NotificationPreference pref = preferenceRepository
            .findByUserIdAndNotificationTypeAndChannel(userId, notificationType, channel)
            .orElseGet(() -> {
                NotificationPreference p = new NotificationPreference();
                p.setUserId(userId);
                p.setNotificationType(notificationType);
                p.setChannel(channel);
                return p;
            });

        pref.setEnabled(enabled);
        NotificationPreference saved = preferenceRepository.save(pref);
        log.debug("Updated notification preference: user={}, type={}, channel={}, enabled={}",
            userId, notificationType, channel, enabled);
        return saved;
    }

    /**
     * 批量更新用户通知偏好
     */
    @Transactional(readOnly = false)
    public List<NotificationPreference> batchUpdate(Long userId, List<Map<String, Object>> preferences) {
        List<NotificationPreference> results = new ArrayList<>();
        for (Map<String, Object> pref : preferences) {
            String type = (String) pref.get("notificationType");
            Channel channel = Channel.valueOf((String) pref.get("channel"));
            boolean enabled = (Boolean) pref.get("enabled");
            results.add(updatePreference(userId, type, channel, enabled));
        }
        return results;
    }

    /**
     * 设置免打扰
     */
    @Transactional(readOnly = false)
    public void setDoNotDisturb(Long userId, String notificationType, Channel channel,
                                 boolean doNotDisturb, String quietStart, String quietEnd) {
        NotificationPreference pref = preferenceRepository
            .findByUserIdAndNotificationTypeAndChannel(userId, notificationType, channel)
            .orElseThrow(() -> new RuntimeException("通知偏好不存在"));

        pref.setDoNotDisturb(doNotDisturb);
        pref.setQuietStart(quietStart);
        pref.setQuietEnd(quietEnd);
        preferenceRepository.save(pref);
    }

    /**
     * 初始化用户默认通知偏好
     * 新用户注册时调用，根据通知类型的 defaultEnabled 设置默认值
     */
    public void initDefaultPreferences(Long userId) {
        for (NotificationType type : NOTIFICATION_TYPES) {
            for (Channel channel : Channel.values()) {
                // 所有渠道默认全部启用（用户可手动关闭不需要的）
                updatePreference(userId, type.getKey(), channel, type.isDefaultEnabled());
            }
        }
        log.info("Default notification preferences initialized for user: {}", userId);
    }

    /**
     * 重置为默认值
     */
    @Transactional(readOnly = false)
    public void resetToDefaults(Long userId) {
        preferenceRepository.deleteByUserId(userId);
        initDefaultPreferences(userId);
    }
}
