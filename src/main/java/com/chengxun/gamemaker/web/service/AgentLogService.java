package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.AgentLogRepository;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent日志服务
 * 提供日志记录、查询和导出功能
 */
@Service
public class AgentLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentLogService.class);

    private final AgentLogRepository logRepository;
    private final OperationLogRepository operationLogRepository;

    public AgentLogService(AgentLogRepository logRepository, OperationLogRepository operationLogRepository) {
        this.logRepository = logRepository;
        this.operationLogRepository = operationLogRepository;
    }

    @Async
    public void logAsync(String agentId, String agentName, String action, String level,
                         String summary, String detail, String projectId, String taskId,
                         String decision, Long durationMs) {
        try {
            AgentLog agentLog = new AgentLog();
            agentLog.setAgentId(agentId);
            agentLog.setAgentName(agentName);
            agentLog.setAction(action);
            agentLog.setLevel(level);
            agentLog.setSummary(summary);
            agentLog.setDetail(detail);
            agentLog.setProjectId(projectId);
            agentLog.setTaskId(taskId);
            agentLog.setDecision(decision);
            agentLog.setDurationMs(durationMs);
            logRepository.save(agentLog);
        } catch (Exception e) {
            log.error("Failed to save agent log: {}", e.getMessage());
        }
    }

    // 便捷方法
    public void info(String agentId, String agentName, String action, String summary) {
        logAsync(agentId, agentName, action, "INFO", summary, null, null, null, null, null);
    }

    public void info(String agentId, String agentName, String action, String summary, String detail) {
        logAsync(agentId, agentName, action, "INFO", summary, detail, null, null, null, null);
    }

    public void warn(String agentId, String agentName, String action, String summary, String detail) {
        logAsync(agentId, agentName, action, "WARN", summary, detail, null, null, null, null);
    }

    public void error(String agentId, String agentName, String action, String summary, String detail) {
        logAsync(agentId, agentName, action, "ERROR", summary, detail, null, null, null, null);
    }

    public void decision(String agentId, String agentName, String summary, String decision) {
        logAsync(agentId, agentName, "DECISION", "INFO", summary, null, null, null, decision, null);
    }

    public void taskLog(String agentId, String agentName, String action, String summary,
                        String taskId, String projectId) {
        logAsync(agentId, agentName, action, "INFO", summary, null, projectId, taskId, null, null);
    }

    public void aiCall(String agentId, String agentName, String summary, long durationMs) {
        logAsync(agentId, agentName, "AI_CALL", "INFO", summary, null, null, null, null, durationMs);
    }

    // 查询方法
    public Page<AgentLog> searchLogs(String agentId, String action, String level,
                                     String keyword, LocalDateTime startTime, LocalDateTime endTime,
                                     int page, int size) {
        return logRepository.searchLogs(agentId, action, level, keyword, startTime, endTime,
            PageRequest.of(page, size));
    }

    public Page<AgentLog> getRecentLogs(int page, int size) {
        return logRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public List<AgentLog> getRecent50() {
        return logRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public List<Object[]> getStatsByAction() {
        return logRepository.countByAction();
    }

    public List<Object[]> getStatsByAgent() {
        return logRepository.countByAgent();
    }

    public long getTotalCount() {
        return logRepository.count();
    }

    /**
     * 搜索Agent日志（返回List，用于导出）
     */
    public List<AgentLog> searchLogs(String agentId, String action, String level,
                                     String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        return logRepository.searchLogs(agentId, action, level, keyword, startTime, endTime);
    }

    /**
     * 搜索操作日志（用于导出）
     */
    public List<OperationLog> searchOperationLogs(Long userId, String action,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        Specification<OperationLog> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (action != null && !action.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (startTime != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
        }
        if (endTime != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
        }

        return operationLogRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
