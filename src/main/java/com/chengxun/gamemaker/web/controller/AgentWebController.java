package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.Skill;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agents")
public class AgentWebController {

    private final AgentManager agentManager;
    private final SkillManager skillManager;

    public AgentWebController(AgentManager agentManager, SkillManager skillManager) {
        this.agentManager = agentManager;
        this.skillManager = skillManager;
    }

    @GetMapping
    public String listAgents(Model model, Authentication authentication) {
        List<Agent> agents = agentManager.getAllAgents();
        model.addAttribute("agents", agents);
        model.addAttribute("username", authentication.getName());
        return "agents";
    }

    @GetMapping("/{agentId}")
    public String agentDetail(@PathVariable String agentId, Model model, Authentication authentication) {
        Map<String, Object> status = agentManager.getAgentStatus(agentId);
        if (status == null) {
            return "redirect:/agents";
        }

        model.addAttribute("status", status);
        model.addAttribute("agentId", agentId);

        // 获取 SKILL 列表
        String projectId = (String) status.get("projectId");
        List<Skill> skills = skillManager.getAllSkills(projectId);
        model.addAttribute("skills", skills);

        model.addAttribute("username", authentication.getName());
        return "agent-detail";
    }

    @PostMapping("/{agentId}/task")
    public String sendTask(@PathVariable String agentId,
                          @RequestParam String taskContent,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent not found");
            return "redirect:/agents";
        }

        com.chengxun.gamemaker.model.AgentMessage taskMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
            authentication.getName(), agentId, taskContent);
        agent.receiveMessage(taskMsg);

        redirectAttributes.addFlashAttribute("success", "任务已发送");
        return "redirect:/agents/" + agentId;
    }

    @PostMapping("/{agentId}/query")
    public String sendQuery(@PathVariable String agentId,
                           @RequestParam String queryContent,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent not found");
            return "redirect:/agents";
        }

        com.chengxun.gamemaker.model.AgentMessage queryMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
            .fromAgentId(authentication.getName())
            .toAgentId(agentId)
            .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.QUERY)
            .content(queryContent)
            .build();
        agent.receiveMessage(queryMsg);

        redirectAttributes.addFlashAttribute("success", "查询已发送");
        return "redirect:/agents/" + agentId;
    }
}
