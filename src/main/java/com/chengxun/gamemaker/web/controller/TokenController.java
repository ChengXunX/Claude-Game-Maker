package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApiTokenService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tokens")
@PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task')")
public class TokenController {

    private final ApiTokenService tokenService;
    private final AgentManager agentManager;
    private final UserService userService;
    private final OperationLogService logService;

    public TokenController(ApiTokenService tokenService, AgentManager agentManager,
                          UserService userService, OperationLogService logService) {
        this.tokenService = tokenService;
        this.agentManager = agentManager;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public String listTokens(Model model, Authentication authentication) {
        List<ApiToken> tokens = tokenService.getAllTokens();
        List<Agent> agents = agentManager.getAllAgents();

        model.addAttribute("tokens", tokens);
        model.addAttribute("agents", agents);
        model.addAttribute("activeCount", tokenService.getActiveTokenCount());
        model.addAttribute("assignedCount", tokenService.getAssignedTokenCount());
        model.addAttribute("username", authentication.getName());
        return "tokens";
    }

    @GetMapping("/create")
    public String createTokenPage(Model model, Authentication authentication) {
        List<Agent> agents = agentManager.getAllAgents();
        model.addAttribute("agents", agents);
        model.addAttribute("username", authentication.getName());
        return "token-form";
    }

    @PostMapping("/create")
    public String createToken(@RequestParam String name,
                             @RequestParam String apiKey,
                             @RequestParam(required = false) String apiUrl,
                             @RequestParam(required = false) String model,
                             @RequestParam(required = false) Integer maxTokens,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String assignAgentId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.createToken(name, apiKey, apiUrl, model,
                maxTokens != null ? maxTokens : 4096, description, authentication.getName());

            // 如果指定了 agent，立即分配
            if (assignAgentId != null && !assignAgentId.isEmpty()) {
                tokenService.assignToken(token.getId(), assignAgentId);
            }

            logService.log(getUserId(authentication), "CREATE_TOKEN", name, "Created API token", null);
            redirectAttributes.addFlashAttribute("success", "Token 创建成功");
            return "redirect:/tokens";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tokens/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editTokenPage(@PathVariable Long id, Model model, Authentication authentication) {
        ApiToken token = tokenService.getTokenById(id);
        if (token == null) {
            return "redirect:/tokens";
        }

        List<Agent> agents = agentManager.getAllAgents();
        model.addAttribute("token", token);
        model.addAttribute("agents", agents);
        model.addAttribute("username", authentication.getName());
        return "token-form";
    }

    @PostMapping("/{id}/edit")
    public String updateToken(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String apiKey,
                             @RequestParam(required = false) String apiUrl,
                             @RequestParam(required = false) String model,
                             @RequestParam(required = false) Integer maxTokens,
                             @RequestParam(required = false) String description,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            tokenService.updateToken(id, name, apiKey, apiUrl, model,
                maxTokens != null ? maxTokens : 4096, description);
            logService.log(getUserId(authentication), "UPDATE_TOKEN", name, "Updated API token", null);
            redirectAttributes.addFlashAttribute("success", "Token 已更新");
            return "redirect:/tokens";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tokens/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteToken(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.getTokenById(id);
            String tokenName = token != null ? token.getName() : "Unknown";
            tokenService.deleteToken(id);
            logService.log(getUserId(authentication), "DELETE_TOKEN", tokenName, "Deleted API token", null);
            redirectAttributes.addFlashAttribute("success", "Token 已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tokens";
    }

    @PostMapping("/{id}/assign")
    public String assignToken(@PathVariable Long id,
                             @RequestParam String agentId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.assignToken(id, agentId);
            logService.log(getUserId(authentication), "ASSIGN_TOKEN",
                token.getName(), "Assigned to agent: " + agentId, null);
            redirectAttributes.addFlashAttribute("success", "Token 已分配给 Agent");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tokens";
    }

    @PostMapping("/{id}/unassign")
    public String unassignToken(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.unassignToken(id);
            logService.log(getUserId(authentication), "UNASSIGN_TOKEN",
                token.getName(), "Unassigned from agent", null);
            redirectAttributes.addFlashAttribute("success", "Token 已取消分配");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tokens";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam ApiToken.TokenStatus status,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            tokenService.updateTokenStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Token 状态已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tokens";
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
