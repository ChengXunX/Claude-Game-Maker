package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AlertRule;
import com.chengxun.gamemaker.web.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 告警规则初始化服务
 * 在系统启动时预置默认的告警规则
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AlertRuleInitService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleInitService.class);

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultRules() {
        if (alertRuleRepository.count() > 0) {
            log.info("Alert rules already initialized ({} rules), skipping", alertRuleRepository.count());
            return;
        }

        log.info("Initializing default alert rules...");

        // Agent 响应时间告警
        createRule("Agent响应时间过长", "Agent响应时间超过30秒",
            "AGENT_RESPONSE_TIME", "GT", 30000.0, "HIGH", "SYSTEM");

        createRule("Agent响应时间超限", "Agent响应时间超过60秒",
            "AGENT_RESPONSE_TIME", "GT", 60000.0, "CRITICAL", "MULTI");

        // Token 消耗告警
        createRule("Token消耗过高", "单次请求Token消耗超过10000",
            "TOKEN_USAGE", "GT", 10000.0, "MEDIUM", "SYSTEM");

        createRule("Token消耗超限", "单次请求Token消耗超过50000",
            "TOKEN_USAGE", "GT", 50000.0, "HIGH", "MULTI");

        // 系统资源告警
        createRule("内存使用率过高", "JVM堆内存使用率超过80%",
            "SYSTEM_RESOURCE", "GT", 80.0, "HIGH", "SYSTEM");

        createRule("内存使用率超限", "JVM堆内存使用率超过95%",
            "SYSTEM_RESOURCE", "GT", 95.0, "CRITICAL", "MULTI");

        // API响应时间告警
        createRule("API响应时间过长", "API响应时间超过5秒",
            "API_RESPONSE_TIME", "GT", 5000.0, "MEDIUM", "SYSTEM");

        createRule("API响应时间超限", "API响应时间超过10秒",
            "API_RESPONSE_TIME", "GT", 10000.0, "HIGH", "MULTI");

        // 任务失败率告警
        createRule("任务失败率过高", "任务失败率超过20%",
            "TASK_FAILURE_RATE", "GT", 20.0, "HIGH", "SYSTEM");

        createRule("任务失败率超限", "任务失败率超过50%",
            "TASK_FAILURE_RATE", "GT", 50.0, "CRITICAL", "MULTI");

        log.info("Default alert rules initialized successfully");
    }

    private void createRule(String name, String description, String metric,
                           String operator, Double threshold, String priority, String notifyMethod) {
        AlertRule rule = new AlertRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(metric);
        rule.setMetric(metric);
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setPriority(priority);
        rule.setNotifyMethod(notifyMethod);
        rule.setEnabled(true);

        alertRuleRepository.save(rule);
        log.debug("Created alert rule: {} ({})", name, metric);
    }
}
