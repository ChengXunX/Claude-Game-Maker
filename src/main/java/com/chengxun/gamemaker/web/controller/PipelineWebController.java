package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.Pipeline;
import com.chengxun.gamemaker.web.service.PipelineService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 流水线Web控制器
 * 处理流水线页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/pipelines")
public class PipelineWebController {

    private static final Logger log = LoggerFactory.getLogger(PipelineWebController.class);

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private UserService userService;

    /**
     * 流水线列表页面
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public String pipelineList(Model model, Authentication authentication) {
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        model.addAttribute("pipelines", pipelines);
        model.addAttribute("stats", pipelineService.getPipelineStatistics());

        return "pipeline/list";
    }

    /**
     * 项目流水线页面
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public String projectPipelines(@PathVariable String projectId, Model model) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return "redirect:/projects";
        List<Pipeline> pipelines = pipelineService.getProjectPipelines(projectId);

        model.addAttribute("project", project);
        model.addAttribute("pipelines", pipelines);

        return "pipeline/project";
    }

    /**
     * 流水线详情页面
     */
    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public String pipelineDetail(@PathVariable Long pipelineId, Model model) {
        Pipeline pipeline = pipelineService.getPipeline(pipelineId);
        if (pipeline == null) {
            return "redirect:/pipelines";
        }

        model.addAttribute("pipeline", pipeline);
        model.addAttribute("stages", pipelineService.getPipelineStages(pipelineId));

        return "pipeline/detail";
    }

    /**
     * 创建流水线页面
     */
    @GetMapping("/create")
    @PreAuthorize("hasAuthority('PERM_pipeline:manage')")
    public String createPipelinePage(Model model) {
        List<GameProject> projects = projectManager.getAllProjects();
        model.addAttribute("projects", projects);

        return "pipeline/create";
    }

    /**
     * 创建流水线
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_pipeline:manage')")
    public String createPipeline(@RequestParam String name,
                                  @RequestParam String description,
                                  @RequestParam String projectId,
                                  @RequestParam String pipelineType,
                                  @RequestParam(required = false) String config,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Pipeline pipeline = pipelineService.createPipeline(name, description, projectId, pipelineType, config);

            log.info("Pipeline created: {} by {}", pipeline.getPipelineNo(), authentication.getName());

            redirectAttributes.addFlashAttribute("success", "流水线创建成功: " + pipeline.getPipelineNo());
            return "redirect:/pipelines/" + pipeline.getId();

        } catch (Exception e) {
            log.error("Failed to create pipeline", e);
            redirectAttributes.addFlashAttribute("error", "创建失败: " + e.getMessage());
            return "redirect:/pipelines/create";
        }
    }

    /**
     * 触发流水线执行
     */
    @PostMapping("/{pipelineId}/trigger")
    @PreAuthorize("hasAuthority('PERM_pipeline:execute')")
    public String triggerPipeline(@PathVariable Long pipelineId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtil.getCurrentUserId(userService);
            String username = authentication.getName();

            Pipeline pipeline = pipelineService.triggerPipeline(pipelineId, userId, username, "MANUAL");

            redirectAttributes.addFlashAttribute("success", "流水线已触发执行");
            return "redirect:/pipelines/" + pipelineId;

        } catch (Exception e) {
            log.error("Failed to trigger pipeline", e);
            redirectAttributes.addFlashAttribute("error", "触发失败: " + e.getMessage());
            return "redirect:/pipelines/" + pipelineId;
        }
    }

    /**
     * 取消流水线执行
     */
    @PostMapping("/{pipelineId}/cancel")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public String cancelPipeline(@PathVariable Long pipelineId,
                                  RedirectAttributes redirectAttributes) {
        try {
            pipelineService.cancelPipeline(pipelineId);
            redirectAttributes.addFlashAttribute("success", "流水线已取消");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "取消失败: " + e.getMessage());
        }
        return "redirect:/pipelines/" + pipelineId;
    }

    /**
     * 暂停流水线
     */
    @PostMapping("/{pipelineId}/pause")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public String pausePipeline(@PathVariable Long pipelineId,
                                 RedirectAttributes redirectAttributes) {
        try {
            pipelineService.pausePipeline(pipelineId);
            redirectAttributes.addFlashAttribute("success", "流水线已暂停");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "暂停失败: " + e.getMessage());
        }
        return "redirect:/pipelines/" + pipelineId;
    }

    /**
     * 恢复流水线
     */
    @PostMapping("/{pipelineId}/resume")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public String resumePipeline(@PathVariable Long pipelineId,
                                  RedirectAttributes redirectAttributes) {
        try {
            pipelineService.resumePipeline(pipelineId);
            redirectAttributes.addFlashAttribute("success", "流水线已恢复");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "恢复失败: " + e.getMessage());
        }
        return "redirect:/pipelines/" + pipelineId;
    }

    /**
     * 提交审批请求
     */
    @PostMapping("/{pipelineId}/submit-approval")
    @PreAuthorize("hasAuthority('PERM_pipeline:execute')")
    public String submitApproval(@PathVariable Long pipelineId,
                                  RedirectAttributes redirectAttributes) {
        try {
            Pipeline pipeline = pipelineService.submitApproval(pipelineId);
            redirectAttributes.addFlashAttribute("success", "审批请求已提交，等待管理员审批");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "提交审批失败: " + e.getMessage());
        }
        return "redirect:/pipelines/" + pipelineId;
    }

    /**
     * 审批流水线
     */
    @PostMapping("/{pipelineId}/approve")
    @PreAuthorize("hasAuthority('PERM_pipeline:approve')")
    public String approvePipeline(@PathVariable Long pipelineId,
                                   @RequestParam boolean approved,
                                   @RequestParam(required = false) String comment,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtil.getCurrentUserId(userService);
            String username = authentication.getName();

            pipelineService.approvePipeline(pipelineId, userId, username, approved, comment);

            if (approved) {
                redirectAttributes.addFlashAttribute("success", "流水线已批准，正在执行");
            } else {
                redirectAttributes.addFlashAttribute("success", "流水线已拒绝");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "审批失败: " + e.getMessage());
        }
        return "redirect:/pipelines/" + pipelineId;
    }
}
