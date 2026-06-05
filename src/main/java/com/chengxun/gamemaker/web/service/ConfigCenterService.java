package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 配置中心服务
 * 负责系统配置的管理、热更新和版本控制
 *
 * 主要功能：
 * - 配置的增删改查
 * - 配置热更新（无需重启）
 * - 配置版本管理
 * - 配置变更通知
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class ConfigCenterService {

    private static final Logger log = LoggerFactory.getLogger(ConfigCenterService.class);

    @Autowired
    private SystemConfigRepository configRepository;

    /** 配置缓存 */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /** 配置变更监听器 */
    private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();

    /**
     * 配置变更监听器接口
     */
    public interface ConfigChangeListener {
        void onConfigChanged(String key, String oldValue, String newValue);
    }

    /**
     * 初始化配置缓存
     */
    public void initCache() {
        log.info("Initializing config cache...");
        List<SystemConfig> configs = configRepository.findAll();
        for (SystemConfig config : configs) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
        log.info("Config cache initialized with {} entries", configs.size());
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回null
     */
    public String getConfig(String key) {
        // 先从缓存获取
        String value = configCache.get(key);
        if (value != null) {
            return value;
        }

        // 缓存中没有，从数据库获取
        SystemConfig config = configRepository.findByConfigKey(key).orElse(null);
        if (config != null) {
            configCache.put(key, config.getConfigValue());
            return config.getConfigValue();
        }

        return null;
    }

    /**
     * 获取配置值，带默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在返回默认值
     */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置配置值
     *
     * @param key 配置键
     * @param value 配置值
     * @param description 配置描述
     * @param group 配置分组
     */
    public void setConfig(String key, String value, String description, String group) {
        String oldValue = configCache.get(key);

        SystemConfig config = configRepository.findByConfigKey(key).orElse(null);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setConfigValue(value);
        config.setDescription(description);
        config.setGroup(group);
        config.setUpdatedAt(LocalDateTime.now());

        configRepository.save(config);
        configCache.put(key, value);

        // 通知监听器
        notifyListeners(key, oldValue, value);

        log.info("Config updated: {} = {}", key, value);
    }

    /**
     * 删除配置
     *
     * @param key 配置键
     */
    public void deleteConfig(String key) {
        // 先获取旧值，再删除
        String oldValue = configCache.get(key);

        configRepository.deleteByConfigKey(key);
        configCache.remove(key);

        // 通知监听器（使用之前保存的旧值）
        notifyListeners(key, oldValue, null);

        log.info("Config deleted: {}", key);
    }

    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    public List<SystemConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 根据分组获取配置
     *
     * @param group 配置分组
     * @return 配置列表
     */
    public List<SystemConfig> getConfigsByGroup(String group) {
        return configRepository.findByGroup(group);
    }

    /**
     * 搜索配置
     *
     * @param keyword 关键词
     * @return 配置列表
     */
    public List<SystemConfig> searchConfigs(String keyword) {
        // 简单实现：获取所有配置后过滤
        return configRepository.findAll().stream()
            .filter(config -> config.getConfigKey().contains(keyword) ||
                             (config.getDescription() != null && config.getDescription().contains(keyword)))
            .toList();
    }

    /**
     * 批量更新配置
     *
     * @param configs 配置映射
     */
    public void batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String oldValue = configCache.get(entry.getKey());

            // 持久化到数据库
            SystemConfig config = configRepository.findByConfigKey(entry.getKey()).orElse(null);
            if (config != null) {
                config.setConfigValue(entry.getValue());
                config.setUpdatedAt(LocalDateTime.now());
                configRepository.save(config);
            } else {
                log.warn("Config key not found in database, skipping: {}", entry.getKey());
                continue;
            }

            configCache.put(entry.getKey(), entry.getValue());

            // 通知监听器
            notifyListeners(entry.getKey(), oldValue, entry.getValue());
        }

        log.info("Batch updated {} configs", configs.size());
    }

    /**
     * 注册配置变更监听器
     *
     * @param key 配置键
     * @param listener 监听器
     */
    public void addListener(String key, ConfigChangeListener listener) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(String key, String oldValue, String newValue) {
        List<ConfigChangeListener> keyListeners = listeners.get(key);
        if (keyListeners != null) {
            for (ConfigChangeListener listener : keyListeners) {
                try {
                    listener.onConfigChanged(key, oldValue, newValue);
                } catch (Exception e) {
                    log.error("Error notifying config listener for key {}: {}", key, e.getMessage());
                }
            }
        }
    }

    /**
     * 获取配置统计
     *
     * @return 统计数据
     */
    public Map<String, Object> getConfigStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<SystemConfig> allConfigs = configRepository.findAll();
        stats.put("totalConfigs", allConfigs.size());

        // 按分组统计
        Map<String, Long> groupCounts = new HashMap<>();
        for (SystemConfig config : allConfigs) {
            String group = config.getGroup() != null ? config.getGroup() : "default";
            groupCounts.merge(group, 1L, Long::sum);
        }
        stats.put("groupCounts", groupCounts);

        // 缓存统计
        stats.put("cachedConfigs", configCache.size());

        return stats;
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        configCache.clear();
        initCache();
        log.info("Config cache refreshed");
    }
}
