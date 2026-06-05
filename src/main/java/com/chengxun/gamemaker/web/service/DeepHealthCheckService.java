package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.manager.AgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 深度健康检查服务
 * 检查系统各组件的健康状态
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class DeepHealthCheckService implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DeepHealthCheckService.class);

    /** 缓存有效期（5 秒） */
    private static final long CACHE_TTL_MS = 5000;

    private final DataSource dataSource;
    private final AgentManager agentManager;

    /** 健康检查结果缓存 */
    private volatile Map<String, Object> cachedDetailedHealth;
    private volatile long lastDetailedHealthTime = 0;

    /** Actuator 健康结果缓存 */
    private volatile Health cachedHealth;
    private volatile long lastHealthTime = 0;

    public DeepHealthCheckService(DataSource dataSource, AgentManager agentManager) {
        this.dataSource = dataSource;
        this.agentManager = agentManager;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean healthy = true;

        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            details.put("database", "UP");
        } catch (Exception e) {
            details.put("database", "DOWN: " + e.getMessage());
            healthy = false;
        }

        // 检查 Agent 状态
        try {
            int totalAgents = agentManager.getAllAgents().size();
            int aliveAgents = (int) agentManager.getAllAgents().stream()
                .filter(a -> a.isAlive()).count();
            details.put("agents.total", totalAgents);
            details.put("agents.alive", aliveAgents);
            details.put("agents.status", aliveAgents + "/" + totalAgents);
        } catch (Exception e) {
            details.put("agents", "ERROR: " + e.getMessage());
            healthy = false;
        }

        // 检查磁盘空间
        try {
            File dataDir = new File("data");
            if (dataDir.exists()) {
                long freeSpace = dataDir.getFreeSpace();
                long totalSpace = dataDir.getTotalSpace();
                double usagePercent = 100.0 - (freeSpace * 100.0 / totalSpace);
                details.put("disk.free", formatSize(freeSpace));
                details.put("disk.total", formatSize(totalSpace));
                details.put("disk.usage", String.format("%.1f%%", usagePercent));

                if (usagePercent > 90) {
                    details.put("disk.warning", "磁盘使用率超过 90%");
                    healthy = false;
                }
            }
        } catch (Exception e) {
            details.put("disk", "ERROR: " + e.getMessage());
        }

        // 检查内存
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usagePercent = usedMemory * 100.0 / maxMemory;

            details.put("memory.max", formatSize(maxMemory));
            details.put("memory.used", formatSize(usedMemory));
            details.put("memory.usage", String.format("%.1f%%", usagePercent));

            if (usagePercent > 85) {
                details.put("memory.warning", "内存使用率超过 85%");
            }
        } catch (Exception e) {
            details.put("memory", "ERROR: " + e.getMessage());
        }

        if (healthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    /**
     * 获取详细健康状态（带缓存）
     */
    public Map<String, Object> getDetailedHealth() {
        long now = System.currentTimeMillis();
        if (cachedDetailedHealth != null && (now - lastDetailedHealthTime) < CACHE_TTL_MS) {
            return cachedDetailedHealth;
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 数据库
        try (Connection conn = dataSource.getConnection()) {
            result.put("database", Map.of("status", "UP", "product", conn.getMetaData().getDatabaseProductName()));
        } catch (Exception e) {
            result.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // Agent
        result.put("agents", Map.of(
            "total", agentManager.getAllAgents().size(),
            "alive", agentManager.getAllAgents().stream().filter(a -> a.isAlive()).count(),
            "projects", agentManager.getRegisteredProjectIds().size()
        ));

        // JVM
        Runtime runtime = Runtime.getRuntime();
        result.put("jvm", Map.of(
            "maxMemory", formatSize(runtime.maxMemory()),
            "totalMemory", formatSize(runtime.totalMemory()),
            "freeMemory", formatSize(runtime.freeMemory()),
            "availableProcessors", runtime.availableProcessors()
        ));

        // 磁盘
        File dataDir = new File("data");
        if (dataDir.exists()) {
            result.put("disk", Map.of(
                "free", formatSize(dataDir.getFreeSpace()),
                "total", formatSize(dataDir.getTotalSpace())
            ));
        }

        // 更新缓存
        cachedDetailedHealth = result;
        lastDetailedHealthTime = System.currentTimeMillis();

        return result;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
