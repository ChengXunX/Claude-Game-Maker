package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.SystemConstantDef;
import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.entity.SystemConstant;
import com.chengxun.gamemaker.web.repository.SystemConstantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统常量服务
 * 统一管理所有可配置常量，支持热更新
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class SystemConstantService {

    private static final Logger log = LoggerFactory.getLogger(SystemConstantService.class);

    private final SystemConstantRepository constantRepository;

    /** 内存缓存 */
    private final ConcurrentHashMap<String, SystemConstant> cache = new ConcurrentHashMap<>();

    public SystemConstantService(SystemConstantRepository constantRepository) {
        this.constantRepository = constantRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        syncFromAnnotations();
        reloadCache();
    }

    /**
     * 扫描 SystemConstants 类中的 @SystemConstantDef 注解，同步到数据库
     * - 数据库中不存在的常量 → 自动创建
     * - 数据库中已存在的常量 → 保留用户修改的值，更新元数据
     * - 代码中已删除的常量 → 保留在数据库中（不自动删除）
     */
    private void syncFromAnnotations() {
        int created = 0;
        int updated = 0;

        for (Field field : SystemConstants.class.getDeclaredFields()) {
            SystemConstantDef def = field.getAnnotation(SystemConstantDef.class);
            if (def == null) continue;

            Optional<SystemConstant> existing = constantRepository.findByConstantKey(def.key());

            if (existing.isPresent()) {
                // 已存在：更新元数据，保留用户修改的值
                SystemConstant constant = existing.get();
                constant.setDisplayName(def.name());
                constant.setDescription(def.description());
                constant.setValueType(def.valueType());
                constant.setGroupName(def.group());
                constant.setUnit(def.unit());
                constant.setMinValue(def.min() != Long.MIN_VALUE ? def.min() : null);
                constant.setMaxValue(def.max() != Long.MAX_VALUE ? def.max() : null);
                constant.setRequireRestart(def.requireRestart());
                constant.setDefaultValue(def.defaultValue());
                constant.setSystemBuiltin(true);
                constantRepository.save(constant);
                updated++;
            } else {
                // 不存在：创建新常量
                SystemConstant constant = new SystemConstant();
                constant.setConstantKey(def.key());
                constant.setDisplayName(def.name());
                constant.setDescription(def.description());
                constant.setValue(def.defaultValue());
                constant.setDefaultValue(def.defaultValue());
                constant.setValueType(def.valueType());
                constant.setGroupName(def.group());
                constant.setUnit(def.unit());
                constant.setMinValue(def.min() != Long.MIN_VALUE ? def.min() : null);
                constant.setMaxValue(def.max() != Long.MAX_VALUE ? def.max() : null);
                constant.setRequireRestart(def.requireRestart());
                constant.setSystemBuiltin(true);
                constantRepository.save(constant);
                created++;
            }
        }

        if (created > 0 || updated > 0) {
            log.info("System constants synced: {} created, {} updated", created, updated);
        }
    }

    // ===== 查询 =====

    /** 获取常量值（字符串） */
    public String getString(String key, String defaultValue) {
        SystemConstant constant = cache.get(key);
        if (constant != null) return constant.getValue();
        return defaultValue;
    }

    /** 获取常量值（int） */
    public int getInt(String key, int defaultValue) {
        SystemConstant constant = cache.get(key);
        if (constant != null) return constant.getIntValue();
        return defaultValue;
    }

    /** 获取常量值（long） */
    public long getLong(String key, long defaultValue) {
        SystemConstant constant = cache.get(key);
        if (constant != null) return constant.getLongValue();
        return defaultValue;
    }

    /** 获取常量值（boolean） */
    public boolean getBoolean(String key, boolean defaultValue) {
        SystemConstant constant = cache.get(key);
        if (constant != null) return constant.getBooleanValue();
        return defaultValue;
    }

    /** 获取所有常量 */
    public List<SystemConstant> getAll() {
        return constantRepository.findAllByOrderByGroupNameAscDisplayNameAsc();
    }

    /** 按分组获取 */
    public List<SystemConstant> getByGroup(String group) {
        return constantRepository.findByGroupNameOrderByDisplayNameAsc(group);
    }

    /** 获取所有分组 */
    public Set<String> getGroups() {
        Set<String> groups = new LinkedHashSet<>();
        for (SystemConstant c : constantRepository.findAllByOrderByGroupNameAscDisplayNameAsc()) {
            groups.add(c.getGroupName());
        }
        return groups;
    }

    // ===== 更新 =====

    /** 更新常量值 */
    public SystemConstant update(String key, String value) {
        SystemConstant constant = constantRepository.findByConstantKey(key)
            .orElseThrow(() -> new RuntimeException("常量不存在: " + key));

        // 数值范围校验
        if ("int".equals(constant.getValueType()) || "long".equals(constant.getValueType())) {
            try {
                long numVal = Long.parseLong(value);
                if (constant.getMinValue() != null && numVal < constant.getMinValue()) {
                    throw new RuntimeException("值不能小于 " + constant.getMinValue());
                }
                if (constant.getMaxValue() != null && numVal > constant.getMaxValue()) {
                    throw new RuntimeException("值不能大于 " + constant.getMaxValue());
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("值必须是数字");
            }
        }

        constant.setValue(value);
        SystemConstant saved = constantRepository.save(constant);
        cache.put(key, saved);

        log.info("System constant updated: {} = {}", key, value);
        return saved;
    }

    /** 批量更新 */
    public List<SystemConstant> batchUpdate(Map<String, String> updates) {
        List<SystemConstant> results = new ArrayList<>();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            try {
                results.add(update(entry.getKey(), entry.getValue()));
            } catch (Exception e) {
                log.warn("Failed to update constant {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return results;
    }

    /** 恢复默认值 */
    public SystemConstant resetToDefault(String key) {
        SystemConstant constant = constantRepository.findByConstantKey(key)
            .orElseThrow(() -> new RuntimeException("常量不存在: " + key));
        constant.setValue(constant.getDefaultValue());
        SystemConstant saved = constantRepository.save(constant);
        cache.put(key, saved);
        return saved;
    }

    /** 全部恢复默认 */
    public void resetAllToDefault() {
        List<SystemConstant> all = constantRepository.findAll();
        for (SystemConstant c : all) {
            c.setValue(c.getDefaultValue());
        }
        constantRepository.saveAll(all);
        reloadCache();
        log.info("All system constants reset to defaults");
    }

    // ===== 缓存 =====

    public void reloadCache() {
        cache.clear();
        for (SystemConstant c : constantRepository.findAll()) {
            cache.put(c.getConstantKey(), c);
        }
        log.info("System constants cache loaded: {} entries", cache.size());
    }

    // ===== 初始化默认常量 =====

    private void initDefaults() {
        log.info("Initializing default system constants...");

        // Agent 相关
        save("agent.max-message-backlog", "最大消息积压", "Agent 消息队列最大积压数量",
            "50", "int", "agent", "条", 1L, 1000L, true);
        save("agent.max-idle-minutes", "最大空闲时间", "Agent 最大无响应时间",
            "30", "int", "agent", "分钟", 1L, 1440L, false);
        save("agent.max-recovery-attempts", "最大恢复尝试", "上下文恢复最大尝试次数",
            "3", "int", "agent", "次", 1L, 10L, false);
        save("agent.task-queue-size", "任务队列大小", "Agent 任务队列最大容量",
            "500", "int", "agent", "条", 10L, 5000L, true);
        save("agent.message-queue-size", "消息队列大小", "Agent 消息队列最大容量",
            "1000", "int", "agent", "条", 10L, 10000L, true);
        save("agent.max-retry-count", "最大重试次数", "任务/消息最大重试次数",
            "3", "int", "agent", "次", 0L, 10L, false);
        save("agent.history-size", "历史记录大小", "任务/消息历史保留数量",
            "1000", "int", "agent", "条", 100L, 10000L, false);
        save("agent.scheduler-interval-ms", "调度间隔", "Agent 调度器执行间隔",
            "300000", "long", "agent", "毫秒", 10000L, 3600000L, false);
        save("agent.max-warnings", "最大警告次数", "绩效管理最大警告次数",
            "3", "int", "agent", "次", 1L, 10L, false);

        // 文件相关
        save("file.max-size-mb", "单文件大小限制", "单个文件最大上传大小",
            "50", "int", "file", "MB", 1L, 500L, false);
        save("file.quota-mb", "Agent 存储配额", "每个 Agent 的文件存储配额",
            "500", "int", "file", "MB", 10L, 5000L, false);
        save("file.storage-dir", "存储目录", "文件存储根目录",
            "data/files", "string", "file", null, null, null, true);

        // 安全相关
        save("security.max-login-attempts", "最大登录尝试", "登录失败最大尝试次数",
            "5", "int", "security", "次", 1L, 20L, false);
        save("security.jwt-expiration-ms", "JWT 过期时间", "JWT Token 有效期",
            "86400000", "long", "security", "毫秒", 3600000L, 604800000L, false);
        save("security.session-timeout-minutes", "会话超时", "Web 会话超时时间",
            "30", "int", "security", "分钟", 5L, 480L, false);

        // 限流相关
        save("rate-limit.global", "全局限流", "全局 API 每分钟请求限制",
            "120", "int", "rate-limit", "次/分钟", 10L, 1000L, false);
        save("rate-limit.auth", "认证限流", "登录接口每分钟请求限制",
            "10", "int", "rate-limit", "次/分钟", 1L, 100L, false);
        save("rate-limit.write", "写操作限流", "写操作每分钟请求限制",
            "60", "int", "rate-limit", "次/分钟", 5L, 500L, false);

        // 性能相关
        save("performance.max-backups", "最大备份数", "文件备份最大保留数量",
            "10", "int", "performance", "份", 1L, 100L, false);
        save("performance.max-output-size", "最大输出大小", "沙箱执行最大输出大小",
            "1048576", "long", "performance", "字节", 1024L, 10485760L, false);
        save("performance.ws-session-timeout", "WebSocket 超时", "终端 WebSocket 会话超时",
            "1800000", "long", "performance", "毫秒", 60000L, 86400000L, false);
        save("performance.context-compact-threshold", "上下文压缩阈值", "触发自动压缩的消息数",
            "50", "int", "performance", "条", 10L, 500L, false);

        // 通知相关
        save("notification.batch-size", "批量通知大小", "批量发送通知的批次大小",
            "100", "int", "notification", "条", 10L, 1000L, false);

        // Agent 知识
        save("agent.max-learned-knowledge", "最大知识条数", "Agent 最大学习知识数量",
            "100", "int", "agent", "条", 10L, 1000L, false);

        log.info("Default system constants initialized: 25 entries");
    }

    private void save(String key, String displayName, String description,
                      String defaultValue, String valueType, String group,
                      String unit, Long min, Long max, boolean requireRestart) {
        SystemConstant constant = new SystemConstant();
        constant.setConstantKey(key);
        constant.setDisplayName(displayName);
        constant.setDescription(description);
        constant.setValue(defaultValue);
        constant.setDefaultValue(defaultValue);
        constant.setValueType(valueType);
        constant.setGroupName(group);
        constant.setUnit(unit);
        constant.setMinValue(min);
        constant.setMaxValue(max);
        constant.setRequireRestart(requireRestart);
        constant.setSystemBuiltin(true);
        constantRepository.save(constant);
    }
}
