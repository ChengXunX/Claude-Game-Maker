package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {
    
    private final AgentManager agentManager;
    private final FeishuBotService feishuService;
    
    public StatusController(AgentManager agentManager, FeishuBotService feishuService) {
        this.agentManager = agentManager;
        this.feishuService = feishuService;
    }
    
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("feishu-enabled", feishuService.isEnabled());
        status.put("agent-count", agentManager.getAllAgents().size());
        
        List<Map<String, Object>> agents = agentManager.getAllAgents().stream()
            .map(agent -> {
                Map<String, Object> agentStatus = new HashMap<>();
                agentStatus.put("id", agent.getId());
                agentStatus.put("name", agent.getName());
                agentStatus.put("role", agent.getRole());
                agentStatus.put("busy", agent.isBusy());
                agentStatus.put("alive", agent.isAlive());
                return agentStatus;
            })
            .toList();
        
        status.put("agents", agents);
        
        return status;
    }
    
    @GetMapping("/agents")
    public List<Map<String, Object>> getAgents() {
        return agentManager.getAllAgents().stream()
            .map(agent -> {
                Map<String, Object> agentInfo = new HashMap<>();
                agentInfo.put("id", agent.getId());
                agentInfo.put("name", agent.getName());
                agentInfo.put("role", agent.getRole());
                agentInfo.put("busy", agent.isBusy());
                agentInfo.put("alive", agent.isAlive());
                agentInfo.put("tasks", agent.getTasks().size());
                return agentInfo;
            })
            .toList();
    }
}
