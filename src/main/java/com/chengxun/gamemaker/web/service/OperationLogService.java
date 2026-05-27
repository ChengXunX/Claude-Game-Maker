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

    public void log(Long userId, String action, String target, String detail, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTarget(target);
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
}
