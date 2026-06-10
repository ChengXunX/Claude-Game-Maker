package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 操作日志服务
 * 提供异步批量写入、查询等功能
 *
 * 采用内存队列 + 定时刷盘策略：
 * - 日志先写入 ConcurrentLinkedQueue 内存队列
 * - 队列达到 50 条或每 3 秒触发一次批量 saveAll()
 * - 应用关闭时 flush 剩余日志
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    /** 批量写入阈值：队列积攒到此数量立即触发写入 */
    private static final int BATCH_THRESHOLD = 50;

    /** 内存日志队列 */
    private final ConcurrentLinkedQueue<OperationLog> logQueue = new ConcurrentLinkedQueue<>();

    private final OperationLogRepository logRepository;

    public OperationLogService(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 异步记录操作日志（基础版）
     * 日志入队后立即返回，不阻塞调用线程
     *
     * @param userId 用户ID
     * @param action 操作类型
     * @param target 目标
     * @param detail 详情
     * @param ipAddress IP地址
     */
    @Async("taskExecutor")
    public void logAsync(Long userId, String action, String target, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setAction(action);
        operationLog.setTargetType(target);
        operationLog.setTargetName(target);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    /**
     * 异步记录操作日志（带用户名）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param action 操作类型
     * @param target 目标
     * @param detail 详情
     * @param ipAddress IP地址
     */
    @Async("taskExecutor")
    public void logAsync(Long userId, String username, String action, String target, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(target);
        operationLog.setTargetName(target);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    /**
     * 异步记录完整操作日志
     */
    @Async("taskExecutor")
    public void logFullAsync(Long userId, String action, String targetType, String targetId,
                             String targetName, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setTargetName(targetName);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    /**
     * 直接入队（由 AOP 切面调用，切面已在异步上下文中）
     */
    public void enqueue(OperationLog operationLog) {
        logQueue.offer(operationLog);
        // 队列积攒到阈值时立即刷盘
        if (logQueue.size() >= BATCH_THRESHOLD) {
            flushQueue();
        }
    }

    /**
     * 每 3 秒定时刷盘，确保低流量时日志也能及时写入
     */
    @Scheduled(fixedRate = 3000)
    public void scheduledFlush() {
        if (!logQueue.isEmpty()) {
            flushQueue();
        }
    }

    /**
     * 应用关闭时 flush 剩余日志
     */
    @PreDestroy
    public void onDestroy() {
        log.info("应用关闭，flush 剩余 {} 条日志", logQueue.size());
        flushQueue();
    }

    /**
     * 批量写入队列中的日志到数据库
     */
    private void flushQueue() {
        List<OperationLog> batch = new ArrayList<>();
        OperationLog item;
        while ((item = logQueue.poll()) != null) {
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            try {
                logRepository.saveAll(batch);
                log.debug("批量写入 {} 条操作日志", batch.size());
            } catch (Exception e) {
                log.error("批量写入操作日志失败: {}", e.getMessage());
            }
        }
    }

    // ========== 以下保留同步方法用于兼容 ==========

    /**
     * 记录操作日志（同步，兼容旧调用）
     */
    public void log(Long userId, String action, String target, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setAction(action);
        operationLog.setTargetType(target);
        operationLog.setTargetName(target);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    /**
     * 记录操作日志（带用户名，同步兼容）
     */
    public void log(Long userId, String username, String action, String target, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(target);
        operationLog.setTargetName(target);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    /**
     * 记录完整操作日志（同步兼容）
     */
    public void logFull(Long userId, String action, String targetType, String targetId,
                        String targetName, String detail, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setTargetName(targetName);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        enqueue(operationLog);
    }

    public List<OperationLog> getRecentLogs() {
        return logRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<OperationLog> getUserLogs(Long userId) {
        return logRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 记录操作日志（带耗时）
     */
    public void log(Long userId, String action, String detail, String params, long durationMs) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setAction(action);
        operationLog.setTargetType("SYSTEM");
        operationLog.setDetail(detail + " [耗时: " + durationMs + "ms]");
        enqueue(operationLog);
    }

    /**
     * 记录完整审计日志（含请求参数和响应数据）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param action 操作类型
     * @param targetType 目标类型
     * @param targetName 目标名称
     * @param detail 操作详情
     * @param requestParams 请求参数（JSON格式）
     * @param responseData 响应数据（JSON格式）
     * @param durationMs 耗时（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    public void logAudit(Long userId, String username, String action, String targetType,
                         String targetName, String detail, String requestParams,
                         String responseData, long durationMs, boolean success, String errorMessage) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetName(targetName);
        operationLog.setDetail(detail);
        operationLog.setRequestParams(requestParams);
        operationLog.setResponseData(responseData);
        operationLog.setDurationMs(durationMs);
        operationLog.setStatus(success ? "SUCCESS" : "FAILURE");
        if (!success && errorMessage != null) {
            operationLog.setErrorMessage(errorMessage);
        }
        enqueue(operationLog);
    }
}
