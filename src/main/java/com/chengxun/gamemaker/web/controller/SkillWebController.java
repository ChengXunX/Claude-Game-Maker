package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/skills")
public class SkillWebController {

    private final SkillManager skillManager;
    private final UserService userService;
    private final OperationLogService logService;

    public SkillWebController(SkillManager skillManager, UserService userService, OperationLogService logService) {
        this.skillManager = skillManager;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public String listSkills(Model model, Authentication authentication) {
        List<Skill> globalSkills = skillManager.getAllGlobalSkills();
        model.addAttribute("globalSkills", globalSkills);
        model.addAttribute("globalCount", globalSkills.size());
        model.addAttribute("username", authentication.getName());
        return "skills";
    }

    /**
     * 获取所有技能（JSON API，供能力管理页面使用）
     */
    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_skills:view', 'PERM_admin:manage')")
    public List<Map<String, Object>> getAllSkillsJson() {
        List<Skill> skills = skillManager.getAllGlobalSkills();
        return skills.stream().map(skill -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", skill.getId());
            map.put("name", skill.getName());
            map.put("skillName", skill.getName());
            map.put("description", skill.getDescription());
            map.put("category", skill.getCategory());
            map.put("triggerPattern", skill.getTriggerPattern());
            map.put("trigger", skill.getTriggerPattern());
            map.put("usageCount", skill.getUsageCount());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{skillId}")
    public String skillDetail(@PathVariable String skillId, Model model, Authentication authentication) {
        Skill skill = skillManager.getGlobalSkill(skillId);
        if (skill == null) {
            return "redirect:/skills";
        }
        model.addAttribute("skill", skill);
        model.addAttribute("username", authentication.getName());
        return "skill-detail";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAuthority('PERM_skills:manage')")
    public String createSkillPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "skill-form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_skills:manage')")
    public String createSkill(@RequestParam String name,
                              @RequestParam String description,
                              @RequestParam String triggerPattern,
                              @RequestParam String prompt,
                              @RequestParam(required = false) String examples,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String id = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            if (skillManager.getGlobalSkill(id) != null) {
                redirectAttributes.addFlashAttribute("error", "技能 ID 已存在: " + id);
                return "redirect:/skills/create";
            }

            List<String> exampleList = parseExamples(examples);

            Skill skill = Skill.builder()
                .id(id)
                .name(name)
                .description(description)
                .category("custom")
                .triggerPattern(triggerPattern)
                .prompt(prompt)
                .examples(exampleList)
                .build();

            skillManager.registerGlobalSkill(skill);
            skillManager.saveGlobalSkillToFile(skill);

            logService.log(getUserId(authentication), "CREATE_SKILL", name, "Created skill: " + name, null);
            redirectAttributes.addFlashAttribute("success", "技能 " + name + " 创建成功");
            return "redirect:/skills/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/skills/create";
        }
    }

    @GetMapping("/{skillId}/edit")
    @PreAuthorize("hasAuthority('PERM_skills:manage')")
    public String editSkillPage(@PathVariable String skillId, Model model, Authentication authentication) {
        Skill skill = skillManager.getGlobalSkill(skillId);
        if (skill == null) {
            return "redirect:/skills";
        }
        model.addAttribute("skill", skill);
        model.addAttribute("username", authentication.getName());
        return "skill-form";
    }

    @PostMapping("/{skillId}/edit")
    @PreAuthorize("hasAuthority('PERM_skills:manage')")
    public String updateSkill(@PathVariable String skillId,
                              @RequestParam String name,
                              @RequestParam String description,
                              @RequestParam String triggerPattern,
                              @RequestParam String prompt,
                              @RequestParam(required = false) String examples,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Skill skill = skillManager.getGlobalSkill(skillId);
            if (skill == null) {
                redirectAttributes.addFlashAttribute("error", "技能不存在");
                return "redirect:/skills";
            }

            if ("builtin".equals(skill.getCategory())) {
                redirectAttributes.addFlashAttribute("error", "内置技能不可编辑");
                return "redirect:/skills/" + skillId;
            }

            skill.setName(name);
            skill.setDescription(description);
            skill.setTriggerPattern(triggerPattern);
            skill.setPrompt(prompt);
            skill.setExamples(parseExamples(examples));

            skillManager.registerGlobalSkill(skill);
            skillManager.saveGlobalSkillToFile(skill);

            logService.log(getUserId(authentication), "UPDATE_SKILL", name, "Updated skill: " + name, null);
            redirectAttributes.addFlashAttribute("success", "技能已更新");
            return "redirect:/skills/" + skillId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/skills/" + skillId + "/edit";
        }
    }

    @PostMapping("/{skillId}/delete")
    @PreAuthorize("hasAuthority('PERM_skills:manage')")
    public String deleteSkill(@PathVariable String skillId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Skill skill = skillManager.getGlobalSkill(skillId);
        if (skill == null) {
            redirectAttributes.addFlashAttribute("error", "技能不存在");
            return "redirect:/skills";
        }

        if ("builtin".equals(skill.getCategory())) {
            redirectAttributes.addFlashAttribute("error", "内置技能不可删除");
            return "redirect:/skills";
        }

        skillManager.removeGlobalSkill(skillId);
        skillManager.deleteGlobalSkillFile(skillId);

        logService.log(getUserId(authentication), "DELETE_SKILL", skill.getName(), "Deleted skill", null);
        redirectAttributes.addFlashAttribute("success", "技能已删除");
        return "redirect:/skills";
    }

    private List<String> parseExamples(String examples) {
        if (examples == null || examples.isBlank()) return List.of();
        return Arrays.stream(examples.split("\\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
