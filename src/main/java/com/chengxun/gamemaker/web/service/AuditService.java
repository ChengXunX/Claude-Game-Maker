package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审计服务
 * 负责操作日志的记录、查询和分析
 *
 * 主要功能：
 * - 记录所有重要操作
 * - 查询操作历史
 * - 生成审计报告
 * - 支持操作回溯
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private OperationLogRepository logRepository;

    /**
     * 记录操作日志
     * @param userId 用户ID
     * @param username 用户名
     * @param action 操作类型
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param targetName 目标名称
     * @param detail 操作详情
     * @param ipAddress IP地址
     * @return 操作日志
     */
    public OperationLog logOperation(Long userId, String username, String action,
                                      String targetType, String targetId, String targetName,
                                      String detail, String ipAddress) {
        return logOperation(userId, username, null, action, targetType, targetId, targetName,
            detail, ipAddress, null, null, null, null, null);
    }

    /**
     * 记录操作日志（完整参数）
     */
    public OperationLog logOperation(Long userId, String username, String agentId,
                                      String action, String targetType, String targetId,
                                      String targetName, String detail, String ipAddress,
                                      String userAgent, String requestParams, String responseData,
                                      Long durationMs, String projectId) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAgentId(agentId);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setTargetName(targetName);
        operationLog.setDetail(detail);
        operationLog.setIpAddress(ipAddress);
        operationLog.setUserAgent(userAgent);
        operationLog.setRequestParams(requestParams);
        operationLog.setResponseData(responseData);
        operationLog.setDurationMs(durationMs);
        operationLog.setProjectId(projectId);
        operationLog.setStatus(OperationLog.Status.SUCCESS.name());
        operationLog.setLevel(OperationLog.Level.INFO.name());

        OperationLog saved = logRepository.save(operationLog);
        log.debug("Operation logged: {} by user {} on {}", action, username, targetType);

        return saved;
    }

    /**
     * 记录用户登录
     */
    public void logUserLogin(Long userId, String username, String ipAddress, boolean success) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction("USER_LOGIN");
        operationLog.setTargetType("USER");
        operationLog.setTargetId(userId.toString());
        operationLog.setTargetName(username);
        operationLog.setIpAddress(ipAddress);
        operationLog.setStatus(success ? OperationLog.Status.SUCCESS.name() : OperationLog.Status.FAILURE.name());
        operationLog.setLevel(success ? OperationLog.Level.INFO.name() : OperationLog.Level.WARN.name());

        logRepository.save(operationLog);
    }

    /**
     * 记录用户登出
     */
    public void logUserLogout(Long userId, String username, String ipAddress) {
        logOperation(userId, username, "USER_LOGOUT", "USER", userId.toString(), username,
            "用户登出", ipAddress);
    }

    /**
     * 记录Agent操作
     */
    public void logAgentOperation(Long userId, String username, String agentId, String agentName,
                                   String action, String detail, String ipAddress) {
        logOperation(userId, username, agentId, action, "AGENT", agentId, agentName,
            detail, ipAddress, null, null, null, null, null);
    }

    /**
     * 记录项目操作
     */
    public void logProjectOperation(Long userId, String username, String projectId, String projectName,
                                     String action, String detail, String ipAddress) {
        logOperation(userId, username, null, action, "PROJECT", projectId, projectName,
            detail, ipAddress, null, null, null, null, projectId);
    }

    /**
     * 记录任务操作
     */
    public void logTaskOperation(Long userId, String username, String taskId, String taskName,
                                  String action, String detail, String ipAddress, String projectId) {
        logOperation(userId, username, null, action, "TASK", taskId, taskName,
            detail, ipAddress, null, null, null, null, projectId);
    }

    /**
     * 记录系统事件
     */
    public void logSystemEvent(String action, String detail) {
        OperationLog operationLog = new OperationLog();
        operationLog.setAction(action);
        operationLog.setTargetType("SYSTEM");
        operationLog.setDetail(detail);
        operationLog.setLevel(OperationLog.Level.INFO.name());
        operationLog.setStatus(OperationLog.Status.SUCCESS.name());

        logRepository.save(operationLog);
    }

    /**
     * 记录错误操作
     */
    public void logError(Long userId, String username, String action, String targetType,
                          String targetId, String errorMessage, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setErrorMessage(errorMessage);
        operationLog.setIpAddress(ipAddress);
        operationLog.setStatus(OperationLog.Status.FAILURE.name());
        operationLog.setLevel(OperationLog.Level.ERROR.name());

        logRepository.save(operationLog);
    }

    // ===== 查询方法 =====

    /**
     * 获取所有操作日志（分页）
     */
    public Page<OperationLog> getAllLogs(int page, int size) {
        return logRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 根据用户获取操作日志
     */
    public List<OperationLog> getUserLogs(Long userId) {
        return logRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 根据操作类型获取日志
     */
    public List<OperationLog> getLogsByAction(String action) {
        return logRepository.findByActionOrderByCreatedAtDesc(action);
    }

    /**
     * 根据目标类型获取日志
     */
    public List<OperationLog> getLogsByTargetType(String targetType) {
        return logRepository.findByTargetTypeOrderByCreatedAtDesc(targetType);
    }

    /**
     * 获取指定时间范围内的日志
     */
    public List<OperationLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return logRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    /**
     * 获取错误日志
     */
    public List<OperationLog> getErrorLogs() {
        return logRepository.findByLevelOrderByCreatedAtDesc("ERROR");
    }

    /**
     * 获取审计统计
     */
    public Map<String, Object> getAuditStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 总操作数
        long totalLogs = logRepository.count();
        stats.put("totalLogs", totalLogs);

        // 今日操作数
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long todayLogs = logRepository.countSince(todayStart);
        stats.put("todayLogs", todayLogs);

        // 各操作类型统计
        List<Object[]> actionCounts = logRepository.countByAction();
        Map<String, Long> actionMap = new HashMap<>();
        for (Object[] row : actionCounts) {
            actionMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("actionCounts", actionMap);

        // 各目标类型统计
        List<Object[]> targetCounts = logRepository.countByTargetType();
        Map<String, Long> targetMap = new HashMap<>();
        for (Object[] row : targetCounts) {
            targetMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("targetCounts", targetMap);

        // 错误日志数量
        Long errorCount = logRepository.countByLevel("ERROR");
        stats.put("errorCount", errorCount);

        return stats;
    }

    /**
     * 获取操作趋势（按小时）
     */
    public Map<String, Long> getOperationTrend(int hours) {
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = hours - 1; i >= 0; i--) {
            LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            Long count = logRepository.countByCreatedAtBetween(hourStart, hourEnd);
            String hourKey = hourStart.getHour() + ":00";
            trend.put(hourKey, count != null ? count : 0);
        }

        return trend;
    }

    /**
     * 搜索操作日志
     */
    public List<OperationLog> searchLogs(String keyword) {
        return logRepository.searchByKeyword(keyword);
    }

    /**
     * 删除过期日志
     * @param daysToKeep 保留天数
     */
    @Transactional
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        logRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up operation logs older than {} days", daysToKeep);
    }
}
