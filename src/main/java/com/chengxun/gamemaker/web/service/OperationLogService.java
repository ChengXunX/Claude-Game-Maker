package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogRepository logRepository;

    public OperationLogService(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 记录操作日志
     * @param userId 用户ID
     * @param action 操作类型
     * @param target 目标（兼容旧接口，会同时设置targetType和targetName）
     * @param detail 详情
     * @param ipAddress IP地址
     */
    public void log(Long userId, String action, String target, String detail, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(target);
        log.setTargetName(target);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        logRepository.save(log);
    }

    /**
     * 记录操作日志（完整参数）
     */
    public void logFull(Long userId, String action, String targetType, String targetId, String targetName, String detail, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        logRepository.save(log);
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
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType("SYSTEM");
        log.setDetail(detail + " [耗时: " + durationMs + "ms]");
        logRepository.save(log);
    }
}
