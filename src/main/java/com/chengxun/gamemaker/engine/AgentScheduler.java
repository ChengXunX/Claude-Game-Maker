package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.manager.AgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(AgentScheduler.class);
    
    private final AppConfig appConfig;
    private final AgentManager agentManager;
    
    public AgentScheduler(AppConfig appConfig, AgentManager agentManager) {
        this.appConfig = appConfig;
        this.agentManager = agentManager;
    }
    
    @Scheduled(fixedDelayString = "${game-maker.scheduler.agent-interval-ms:600000}")
    public void scheduleAgentWork() {
        if (!appConfig.getScheduler().isEnabled()) {
            return;
        }
        
        log.debug("Running agent scheduler...");
        
        for (Agent agent : agentManager.getAllAgents()) {
            if (agent.isAlive() && !agent.isBusy()) {
                try {
                    log.info("Triggering work for agent: {}", agent.getName());
                    agent.work();
                } catch (Exception e) {
                    log.error("Error in agent scheduler for {}: {}", agent.getName(), e.getMessage());
                }
            }
        }
    }
    
    @Scheduled(fixedDelayString = "${game-maker.scheduler.producer-interval-ms:300000}")
    public void scheduleProducerWork() {
        if (!appConfig.getScheduler().isEnabled()) {
            return;
        }
        
        log.debug("Running producer scheduler...");
        
        Agent producer = agentManager.getAgent("producer");
        if (producer != null && producer.isAlive() && !producer.isBusy()) {
            try {
                log.info("Triggering producer work");
                producer.work();
            } catch (Exception e) {
                log.error("Error in producer scheduler: {}", e.getMessage());
            }
        }
    }
}
