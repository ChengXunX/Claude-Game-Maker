package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.dingtalk.DingTalkService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final AgentManager agentManager;
    private final FeishuBotService feishuService;
    private final DingTalkService dingTalkService;
    private final SkillManager skillManager;
    private final ProjectManager projectManager;

    public StatusController(AgentManager agentManager, FeishuBotService feishuService,
                           DingTalkService dingTalkService,
                           SkillManager skillManager, ProjectManager projectManager) {
        this.agentManager = agentManager;
        this.feishuService = feishuService;
        this.dingTalkService = dingTalkService;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("feishu-enabled", feishuService.isEnabled());
        status.put("dingtalk-enabled", dingTalkService.isEnabled());
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

    // Agent相关API已迁移到AgentApiController，此处仅保留状态查询
    // 项目相关API已迁移到ProjectApiController，此处仅保留状态查询
}
