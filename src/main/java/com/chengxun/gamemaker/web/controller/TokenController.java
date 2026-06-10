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
                             @RequestParam(required = false) Integer contextWindow,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String assignAgentId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.createToken(name, apiKey, apiUrl, model,
                maxTokens != null ? maxTokens : 4096,
                contextWindow != null ? contextWindow : 200000,
                description, authentication.getName());

            // 如果指定了 agent，立即分配
            if (assignAgentId != null && !assignAgentId.isEmpty()) {
                tokenService.assignToken(token.getId(), assignAgentId);
            }

            logService.log(getUserId(authentication), "CREATE_TOKEN", name,
                "Created API token (key: " + maskApiKey(apiKey) + ")", null);
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
                             @RequestParam(required = false) Integer contextWindow,
                             @RequestParam(required = false) String description,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            tokenService.updateToken(id, name, apiKey, apiUrl, model,
                maxTokens != null ? maxTokens : 4096,
                contextWindow != null ? contextWindow : 200000,
                description);
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

    /**
     * T22: 测试 Token 连接
     */
    @PostMapping("/{id}/test")
    public String testToken(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.getTokenById(id);
            if (token == null) {
                redirectAttributes.addFlashAttribute("error", "Token 不存在");
                return "redirect:/tokens";
            }

            // 简单测试：检查 API Key 格式
            if (token.getApiKey() == null || token.getApiKey().isBlank()) {
                redirectAttributes.addFlashAttribute("error", "API Key 为空");
            } else if (token.getApiKey().length() < 10) {
                redirectAttributes.addFlashAttribute("warning", "API Key 格式可能不正确（长度过短）");
            } else {
                redirectAttributes.addFlashAttribute("success", "Token 格式检查通过");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "测试失败: " + e.getMessage());
        }
        return "redirect:/tokens";
    }

    @PostMapping("/{id}/assign")
    public String assignToken(@PathVariable Long id,
                             @RequestParam String agentId,
                             @RequestParam(defaultValue = "immediate") String activation,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            ApiToken token = tokenService.assignToken(id, agentId, activation);
            String activationLabel = "pending".equals(activation) ? "等待任务完成" : "立即";
            logService.log(getUserId(authentication), "ASSIGN_TOKEN",
                token.getName(), "Assigned to agent: " + agentId + " (" + activationLabel + ")", null);
            redirectAttributes.addFlashAttribute("success",
                "Token 已分配给 Agent（" + activationLabel + "生效）");
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

    /**
     * S03: API Key 脱敏
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
