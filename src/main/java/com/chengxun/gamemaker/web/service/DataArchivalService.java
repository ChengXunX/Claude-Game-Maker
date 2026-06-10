package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.repository.AlertRecordRepository;
import com.chengxun.gamemaker.web.repository.CapabilityInvocationLogRepository;
import com.chengxun.gamemaker.web.repository.OperationLogArchiveRepository;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 数据归档服务
 * 定期归档旧数据，保持主表性能
 *
 * 归档策略：
 * - 操作日志：先迁移到 operation_logs_archive 表，再从主表删除
 * - 调用日志：直接删除（保留 30 天）
 * - 过期权限：自动禁用
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class DataArchivalService {

    private static final Logger log = LoggerFactory.getLogger(DataArchivalService.class);

    /** 操作日志保留天数 */
    private static final int OPERATION_LOG_RETENTION_DAYS = 90;

    /** 调用日志保留天数 */
    private static final int INVOCATION_LOG_RETENTION_DAYS = 30;

    /** 每批归档的数量，避免长事务 */
    private static final int ARCHIVE_BATCH_SIZE = 5000;

    /** 告警记录保留天数 */
    private static final int ALERT_RECORD_RETENTION_DAYS = 90;

    private final OperationLogRepository operationLogRepository;
    private final CapabilityInvocationLogRepository invocationLogRepository;
    private final OperationLogArchiveRepository archiveRepository;
    private final AlertRecordRepository alertRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataArchivalService(OperationLogRepository operationLogRepository,
                                CapabilityInvocationLogRepository invocationLogRepository,
                                OperationLogArchiveRepository archiveRepository,
                                AlertRecordRepository alertRecordRepository) {
        this.operationLogRepository = operationLogRepository;
        this.invocationLogRepository = invocationLogRepository;
        this.archiveRepository = archiveRepository;
        this.alertRecordRepository = alertRecordRepository;
    }

    /**
     * 每天凌晨 3 点执行数据归档
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveOldData() {
        log.info("Starting data archival...");
        try {
            archiveOperationLogs();
            archiveInvocationLogs();
            cleanupExpiredPermissions();
            cleanupResolvedAlerts();
        } catch (Exception e) {
            log.error("Data archival failed", e);
        }
        log.info("Data archival completed");
    }

    /**
     * 归档操作日志：先迁移到归档表，再从主表删除
     * 分批处理，每批 ARCHIVE_BATCH_SIZE 条，避免长事务锁表
     */
    @Transactional
    public void archiveOperationLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(OPERATION_LOG_RETENTION_DAYS);

        // 分批归档，避免一次性迁移过多数据导致长事务
        boolean hasMore = true;
        int totalArchived = 0;

        while (hasMore) {
            // 1. 将旧数据插入归档表（分批）
            int inserted = entityManager.createNativeQuery(
                "INSERT INTO operation_logs_archive " +
                "(user_id, username, agent_id, operation, target_type, target_id, target_name, " +
                "detail, level, status, error_message, ip_address, user_agent, " +
                "request_params, response_data, duration_ms, project_id, created_at) " +
                "SELECT user_id, username, agent_id, operation, target_type, target_id, target_name, " +
                "detail, level, status, error_message, ip_address, user_agent, " +
                "request_params, response_data, duration_ms, project_id, created_at " +
                "FROM operation_logs WHERE created_at < :cutoff LIMIT :batchSize")
                .setParameter("cutoff", cutoff)
                .setParameter("batchSize", ARCHIVE_BATCH_SIZE)
                .executeUpdate();

            if (inserted > 0) {
                totalArchived += inserted;

                // 2. 删除已归档的数据
                entityManager.createNativeQuery(
                    "DELETE FROM operation_logs WHERE created_at < :cutoff LIMIT :batchSize")
                    .setParameter("cutoff", cutoff)
                    .setParameter("batchSize", ARCHIVE_BATCH_SIZE)
                    .executeUpdate();

                log.info("Archived batch of {} operation logs (total: {})", inserted, totalArchived);
            }

            hasMore = (inserted >= ARCHIVE_BATCH_SIZE);
        }

        if (totalArchived > 0) {
            log.info("Total archived {} operation logs older than {}", totalArchived, cutoff);
        }
    }

    /**
     * 清理调用日志（直接删除，保留 30 天）
     */
    @Transactional
    public void archiveInvocationLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(INVOCATION_LOG_RETENTION_DAYS);
        int deleted = entityManager.createQuery(
            "DELETE FROM CapabilityInvocationLog c WHERE c.createdAt < :cutoff")
            .setParameter("cutoff", cutoff)
            .executeUpdate();
        if (deleted > 0) {
            log.info("Archived {} invocation logs older than {}", deleted, cutoff);
        }
    }

    /**
     * 禁用过期权限
     */
    @Transactional
    public void cleanupExpiredPermissions() {
        int disabled = entityManager.createQuery(
            "UPDATE UserPermission p SET p.enabled = false WHERE p.expiresAt < CURRENT_TIMESTAMP AND p.enabled = true")
            .executeUpdate();
        if (disabled > 0) {
            log.info("Disabled {} expired permissions", disabled);
        }
    }

    /**
     * 清理已解决/已忽略的告警记录（保留 90 天）
     * 分批删除，避免长事务
     */
    @Transactional
    public void cleanupResolvedAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ALERT_RECORD_RETENTION_DAYS);
        int totalDeleted = 0;

        while (true) {
            int deleted = alertRecordRepository.deleteResolvedBefore(cutoff, ARCHIVE_BATCH_SIZE);
            totalDeleted += deleted;
            if (deleted < ARCHIVE_BATCH_SIZE) break;
        }

        if (totalDeleted > 0) {
            log.info("Cleaned up {} resolved alert records older than {}", totalDeleted, cutoff);
        }
    }
}
