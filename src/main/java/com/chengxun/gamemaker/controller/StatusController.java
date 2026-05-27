package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.Skill;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final SkillManager skillManager;

    public StatusController(AgentManager agentManager, FeishuBotService feishuService, SkillManager skillManager) {
        this.agentManager = agentManager;
        this.feishuService = feishuService;
        this.skillManager = skillManager;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("feishu-enabled", feishuService.isEnabled());
        status.put("agent-count", agentManager.getAllAgents().size());
        status.put("skill-count", skillManager.getAllSkills().size());

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

    @GetMapping("/agents/{agentId}")
    public Map<String, Object> getAgentDetail(@PathVariable String agentId) {
        Map<String, Object> status = agentManager.getAgentStatus(agentId);
        if (status == null) {
            status = new HashMap<>();
            status.put("error", "Agent not found");
        }
        return status;
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> getSkills() {
        return skillManager.getAllSkills().stream()
            .map(skill -> {
                Map<String, Object> skillInfo = new HashMap<>();
                skillInfo.put("id", skill.getId());
                skillInfo.put("name", skill.getName());
                skillInfo.put("description", skill.getDescription());
                skillInfo.put("category", skill.getCategory());
                skillInfo.put("usageCount", skill.getUsageCount());
                return skillInfo;
            })
            .toList();
    }

    @GetMapping("/skills/{skillId}")
    public Map<String, Object> getSkillDetail(@PathVariable String skillId) {
        Skill skill = skillManager.getSkill(skillId);
        if (skill == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Skill not found");
            return error;
        }

        Map<String, Object> skillInfo = new HashMap<>();
        skillInfo.put("id", skill.getId());
        skillInfo.put("name", skill.getName());
        skillInfo.put("description", skill.getDescription());
        skillInfo.put("category", skill.getCategory());
        skillInfo.put("triggerPattern", skill.getTriggerPattern());
        skillInfo.put("prompt", skill.getPrompt());
        skillInfo.put("examples", skill.getExamples());
        skillInfo.put("usageCount", skill.getUsageCount());
        skillInfo.put("createdAt", skill.getCreatedAt());
        skillInfo.put("lastUsedAt", skill.getLastUsedAt());
        return skillInfo;
    }
}
