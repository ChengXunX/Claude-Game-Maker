package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.NotificationTemplate;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Category;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Channel;
import com.chengxun.gamemaker.web.service.NotificationTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 通知模板Web控制器
 * 处理通知模板页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/notification-templates")
public class NotificationTemplateWebController {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateWebController.class);

    private final NotificationTemplateService templateService;

    public NotificationTemplateWebController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 模板列表页面
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String templateList(Model model) {
        List<NotificationTemplate> templates = templateService.getAllTemplates();
        model.addAttribute("templates", templates);
        model.addAttribute("channels", Channel.values());
        model.addAttribute("categories", Category.values());
        return "notification-templates/list";
    }

    /**
     * 创建模板页面
     */
    @GetMapping("/create")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String createTemplatePage(Model model) {
        model.addAttribute("template", new NotificationTemplate());
        model.addAttribute("channels", Channel.values());
        model.addAttribute("categories", Category.values());
        model.addAttribute("variableDescriptions", templateService.getVariableDescriptions());
        return "notification-templates/form";
    }

    /**
     * 编辑模板页面
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String editTemplatePage(@PathVariable Long id, Model model) {
        NotificationTemplate template = templateService.getTemplate(id);
        if (template == null) {
            return "redirect:/notification-templates";
        }
        model.addAttribute("template", template);
        model.addAttribute("channels", Channel.values());
        model.addAttribute("categories", Category.values());
        model.addAttribute("variableDescriptions", templateService.getVariableDescriptions());
        return "notification-templates/form";
    }

    /**
     * 保存模板
     */
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String saveTemplate(@ModelAttribute NotificationTemplate template,
                                RedirectAttributes redirectAttributes) {
        try {
            if (template.getId() != null) {
                templateService.updateTemplate(template);
                redirectAttributes.addFlashAttribute("success", "模板更新成功");
            } else {
                templateService.createTemplate(template);
                redirectAttributes.addFlashAttribute("success", "模板创建成功");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "操作失败: " + e.getMessage());
        }
        return "redirect:/notification-templates";
    }

    /**
     * 删除模板
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String deleteTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            templateService.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("success", "模板删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败: " + e.getMessage());
        }
        return "redirect:/notification-templates";
    }

    /**
     * 切换启用状态
     */
    @PostMapping("/toggle/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public String toggleTemplate(@PathVariable Long id,
                                  @RequestParam boolean enabled,
                                  RedirectAttributes redirectAttributes) {
        try {
            templateService.toggleTemplate(id, enabled);
            redirectAttributes.addFlashAttribute("success", enabled ? "模板已启用" : "模板已禁用");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "操作失败: " + e.getMessage());
        }
        return "redirect:/notification-templates";
    }
}
