package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final AgentManager agentManager;
    private final FeishuBotService feishuService;
    private final SkillManager skillManager;
    private final ProjectManager projectManager;

    public StatusController(AgentManager agentManager, FeishuBotService feishuService,
                           SkillManager skillManager, ProjectManager projectManager) {
        this.agentManager = agentManager;
        this.feishuService = feishuService;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("feishu-enabled", feishuService.isEnabled());
        status.put("agent-count", agentManager.getAllAgents().size());
        status.put("global-skill-count", skillManager.getAllGlobalSkills().size());
        status.put("project-count", projectManager.getAllProjects().size());

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

    // ===== 项目相关 API =====

    @GetMapping("/projects")
    public List<Map<String, Object>> getProjects() {
        return projectManager.getAllProjects().stream()
            .map(project -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", project.getId());
                info.put("name", project.getName());
                info.put("description", project.getDescription());
                info.put("workDir", project.getWorkDir());
                info.put("status", project.getStatus());
                info.put("agentCount", project.getAgentIds().size());
                info.put("createdAt", project.getCreatedAt());
                info.put("lastActiveAt", project.getLastActiveAt());
                return info;
            })
            .toList();
    }

    @GetMapping("/projects/{projectId}")
    public Map<String, Object> getProjectDetail(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Project not found");
            return error;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", project.getId());
        info.put("name", project.getName());
        info.put("description", project.getDescription());
        info.put("workDir", project.getWorkDir());
        info.put("status", project.getStatus());
        info.put("agentIds", project.getAgentIds());
        info.put("metadata", project.getMetadata());
        info.put("createdAt", project.getCreatedAt());
        info.put("lastActiveAt", project.getLastActiveAt());

        // 项目级别的 SKILL
        Map<String, Skill> projectSkills = skillManager.getProjectSkills(projectId);
        info.put("skillCount", projectSkills.size());

        return info;
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        String workDir = request.get("workDir");

        if (name == null || workDir == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "name and workDir are required");
            return error;
        }

        GameProject project = projectManager.createProject(name, description, workDir);

        Map<String, Object> result = new HashMap<>();
        result.put("id", project.getId());
        result.put("name", project.getName());
        result.put("workDir", project.getWorkDir());
        return result;
    }

    @PostMapping("/projects/{projectId}/rules")
    public Map<String, Object> setProjectRules(@PathVariable String projectId, @RequestBody Map<String, String> request) {
        String rules = request.get("rules");
        if (rules == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "rules is required");
            return error;
        }

        projectManager.saveProjectRules(projectId, rules);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("projectId", projectId);
        return result;
    }

    // ===== SKILL 相关 API =====

    @GetMapping("/skills")
    public List<Map<String, Object>> getSkills(@RequestParam(required = false) String projectId) {
        List<Skill> skills;
        if (projectId != null) {
            skills = skillManager.getAllSkills(projectId);
        } else {
            skills = skillManager.getAllGlobalSkills();
        }

        return skills.stream()
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
    public Map<String, Object> getSkillDetail(@PathVariable String skillId,
                                               @RequestParam(required = false) String projectId) {
        Skill skill = null;
        if (projectId != null) {
            Map<String, Skill> projectSkills = skillManager.getProjectSkills(projectId);
            skill = projectSkills.get(skillId);
        }
        if (skill == null) {
            skill = skillManager.getGlobalSkill(skillId);
        }

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
