package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 系统配置管理控制器
 * 提供配置的查看、编辑、创建和删除功能
 */
@Controller
@RequestMapping({"/admin/configs", "/api/admin/configs"})
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService configService;
    private final UserService userService;
    private final OperationLogService logService;

    public SystemConfigController(SystemConfigService configService, UserService userService,
                                 OperationLogService logService) {
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;
    }

    /**
     * 配置列表页面
     */
    @GetMapping
    public String listConfigs(@RequestParam(required = false) String group,
                              Model model, Authentication authentication) {
        List<SystemConfig> configs;
        if (group != null && !group.isEmpty()) {
            configs = configService.getConfigsByGroup(group);
        } else {
            configs = configService.getAllConfigs();
        }

        model.addAttribute("configs", configs);
        model.addAttribute("groups", configService.getAllGroups());
        model.addAttribute("selectedGroup", group);
        model.addAttribute("username", authentication.getName());
        return "admin/configs";
    }

    /**
     * 编辑配置页面
     */
    @GetMapping("/{id}/edit")
    public String editConfigPage(@PathVariable Long id, Model model, Authentication authentication) {
        SystemConfig config = configService.getAllConfigs().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (config == null) {
            return "redirect:/admin/configs";
        }

        model.addAttribute("config", config);
        model.addAttribute("username", authentication.getName());
        return "admin/config-form";
    }

    /**
     * 更新配置
     */
    @PostMapping("/{id}/edit")
    public String updateConfig(@PathVariable Long id,
                               @RequestParam String configValue,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            SystemConfig config = configService.getAllConfigs().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);

            if (config == null) {
                redirectAttributes.addFlashAttribute("error", "配置不存在");
                return "redirect:/admin/configs";
            }

            configService.setConfig(config.getConfigKey(), configValue);

            User user = userService.getUserByUsername(authentication.getName());
            logService.log(user.getId(), "UPDATE_CONFIG", config.getConfigKey(),
                "Updated config: " + config.getConfigKey() + " = " + configValue, null);

            redirectAttributes.addFlashAttribute("success", "配置已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/configs";
    }

    /**
     * 创建配置页面
     */
    @GetMapping("/create")
    public String createConfigPage(Model model, Authentication authentication) {
        model.addAttribute("groups", configService.getAllGroups());
        model.addAttribute("username", authentication.getName());
        return "admin/config-form";
    }

    /**
     * 创建配置
     */
    @PostMapping("/create")
    public String createConfig(@RequestParam String configKey,
                               @RequestParam String configValue,
                               @RequestParam(required = false) String description,
                               @RequestParam String group,
                               @RequestParam(required = false, defaultValue = "string") String valueType,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setDescription(description);
            config.setGroup(group);
            config.setValueType(valueType);
            config.setSystemBuiltin(false);

            configService.createConfig(config);

            User user = userService.getUserByUsername(authentication.getName());
            logService.log(user.getId(), "CREATE_CONFIG", configKey,
                "Created config: " + configKey, null);

            redirectAttributes.addFlashAttribute("success", "配置已创建");
            return "redirect:/admin/configs";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/configs/create";
        }
    }

    /**
     * 删除配置
     */
    @PostMapping("/{id}/delete")
    public String deleteConfig(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            configService.deleteConfig(id);

            User user = userService.getUserByUsername(authentication.getName());
            logService.log(user.getId(), "DELETE_CONFIG", "Config " + id,
                "Deleted config: " + id, null);

            redirectAttributes.addFlashAttribute("success", "配置已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/configs";
    }

    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh")
    public String refreshCache(Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        configService.refreshCache();

        User user = userService.getUserByUsername(authentication.getName());
        logService.log(user.getId(), "REFRESH_CONFIG_CACHE", "Config cache",
            "Refreshed config cache", null);

        redirectAttributes.addFlashAttribute("success", "配置缓存已刷新");
        return "redirect:/admin/configs";
    }
}
