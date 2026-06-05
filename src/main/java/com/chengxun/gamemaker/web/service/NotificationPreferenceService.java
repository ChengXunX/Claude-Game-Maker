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

    /** 所有通知类型定义（缓存） */
    private static final List<NotificationType> NOTIFICATION_TYPES = List.of(
        // 审批相关
        new NotificationType("approval_created", "新审批请求", "有新的审批请求需要处理", "审批", "PERM_approval:view", true),
        new NotificationType("approval_approved", "审批通过", "您的审批请求已通过", "审批", null, true),
        new NotificationType("approval_rejected", "审批被拒", "您的审批请求被拒绝", "审批", null, true),
        new NotificationType("approval_expired", "审批过期", "审批请求已过期", "审批", "PERM_approval:view", false),

        // 任务相关
        new NotificationType("task_assigned", "任务分配", "有新任务分配给您", "任务", null, true),
        new NotificationType("task_completed", "任务完成", "任务已完成", "任务", null, true),
        new NotificationType("task_failed", "任务失败", "任务执行失败", "任务", null, true),
        new NotificationType("task_progress", "任务进度", "任务进度更新", "任务", null, false),

        // Agent 相关
        new NotificationType("agent_created", "Agent 创建", "新 Agent 已创建", "Agent", "PERM_agents:view", true),
        new NotificationType("agent_started", "Agent 启动", "Agent 已启动", "Agent", "PERM_agents:view", false),
        new NotificationType("agent_stopped", "Agent 停止", "Agent 已停止", "Agent", "PERM_agents:view", true),
        new NotificationType("agent_error", "Agent 异常", "Agent 运行异常", "Agent", "PERM_agents:view", true),
        new NotificationType("agent_context_invalid", "上下文失效", "Agent 上下文失效需要恢复", "Agent", "PERM_agents:manage", true),

        // 告警相关
        new NotificationType("alert_triggered", "告警触发", "监控告警已触发", "告警", "PERM_system:monitor", true),
        new NotificationType("alert_resolved", "告警解除", "监控告警已解除", "告警", "PERM_system:monitor", false),

        // 权限相关
        new NotificationType("permission_request", "权限申请", "有新的权限申请需要审批", "权限", "PERM_admin:manage", true),
        new NotificationType("permission_approved", "权限批准", "您的权限申请已批准", "权限", null, true),
        new NotificationType("permission_rejected", "权限拒绝", "您的权限申请被拒绝", "权限", null, true),

        // 系统相关
        new NotificationType("system_maintenance", "系统维护", "系统维护通知", "系统", "PERM_system:manage", true),
        new NotificationType("system_update", "系统更新", "系统更新通知", "系统", "PERM_system:manage", false),
        new NotificationType("token_exhausted", "Token 耗尽", "API Token 配额不足", "系统", "PERM_tokens:manage", true),

        // 项目相关
        new NotificationType("project_created", "项目创建", "新项目已创建", "项目", "PERM_projects:view", false),
        new NotificationType("project_goal_completed", "目标完成", "项目目标已完成", "项目", "PERM_projects:view", true)
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
    @Transactional
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
    @Transactional
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
                // 站内信默认全部启用，其他渠道默认禁用
                boolean defaultEnabled = channel == Channel.IN_APP && type.isDefaultEnabled();
                updatePreference(userId, type.getKey(), channel, defaultEnabled);
            }
        }
        log.info("Default notification preferences initialized for user: {}", userId);
    }

    /**
     * 重置为默认值
     */
    public void resetToDefaults(Long userId) {
        preferenceRepository.deleteByUserId(userId);
        initDefaultPreferences(userId);
    }
}
