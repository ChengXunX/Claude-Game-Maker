package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.repository.CapabilityInvocationLogRepository;
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

    private final OperationLogRepository operationLogRepository;
    private final CapabilityInvocationLogRepository invocationLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataArchivalService(OperationLogRepository operationLogRepository,
                                CapabilityInvocationLogRepository invocationLogRepository) {
        this.operationLogRepository = operationLogRepository;
        this.invocationLogRepository = invocationLogRepository;
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
        } catch (Exception e) {
            log.error("Data archival failed", e);
        }
        log.info("Data archival completed");
    }

    @Transactional
    public void archiveOperationLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(OPERATION_LOG_RETENTION_DAYS);
        int deleted = entityManager.createQuery("DELETE FROM OperationLog o WHERE o.createdAt < :cutoff")
            .setParameter("cutoff", cutoff)
            .executeUpdate();
        if (deleted > 0) {
            log.info("Archived {} operation logs older than {}", deleted, cutoff);
        }
    }

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

    @Transactional
    public void cleanupExpiredPermissions() {
        int disabled = entityManager.createQuery(
            "UPDATE UserPermission p SET p.enabled = false WHERE p.expiresAt < CURRENT_TIMESTAMP AND p.enabled = true")
            .executeUpdate();
        if (disabled > 0) {
            log.info("Disabled {} expired permissions", disabled);
        }
    }
}
