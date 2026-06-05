package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.Skill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能管理 API 控制器
 * 提供技能的 CRUD 接口
 *
 * 操作维度：系统级
 * 权限要求：skills:view / skills:manage
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/skills")
@Tag(name = "技能管理", description = "技能相关接口")
public class SkillApiController {

    private static final Logger log = LoggerFactory.getLogger(SkillApiController.class);

    private final SkillManager skillManager;

    public SkillApiController(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    /**
     * 获取所有技能列表
     *
     * @param keyword 搜索关键词（可选）
     * @return 技能列表
     */
    @GetMapping
    @Operation(summary = "获取技能列表")
    @PreAuthorize("hasAuthority('PERM_skills:view') or hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectId) {

        try {
            List<Skill> skills = skillManager.getAllSkills(projectId);

            List<Map<String, Object>> result = skills.stream()
                .filter(skill -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    String lowerKeyword = keyword.toLowerCase();
                    return (skill.getName() != null && skill.getName().toLowerCase().contains(lowerKeyword))
                        || (skill.getId() != null && skill.getId().toLowerCase().contains(lowerKeyword))
                        || (skill.getDescription() != null && skill.getDescription().toLowerCase().contains(lowerKeyword));
                })
                .map(this::toMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取技能列表失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 获取技能详情
     * 同时查找全局技能和项目技能
     *
     * @param id 技能ID
     * @return 技能详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取技能详情")
    @PreAuthorize("hasAuthority('PERM_skills:view') or hasRole('ADMIN')")
    public ResponseEntity<?> detail(@PathVariable String id) {
        // 先查找全局技能
        Skill skill = skillManager.getGlobalSkill(id);

        // 如果全局技能中没有找到，查找项目技能
        if (skill == null) {
            for (Map.Entry<String, Map<String, Skill>> entry : skillManager.getAllProjectSkills().entrySet()) {
                Skill projectSkill = entry.getValue().get(id);
                if (projectSkill != null) {
                    skill = projectSkill;
                    break;
                }
            }
        }

        if (skill == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toMap(skill));
    }

    /**
     * 获取项目技能列表
     *
     * @param projectId 项目ID
     * @return 技能列表
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目技能")
    @PreAuthorize("hasAuthority('PERM_skills:view') or hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getByProject(@PathVariable String projectId) {
        List<Skill> skills = skillManager.getAllSkills(projectId);

        List<Map<String, Object>> result = skills.stream()
            .map(this::toMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 创建技能
     *
     * @param skillData 技能数据
     * @return 创建的技能
     */
    @PostMapping
    @Operation(summary = "创建技能")
    @PreAuthorize("hasAuthority('PERM_skills:manage') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> skillData) {
        try {
            String name = skillData.get("name");
            String description = skillData.get("description");
            String content = skillData.get("content");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("技能名称不能为空"));
            }

            // 生成技能ID
            String id = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

            Skill skill = new Skill();
            skill.setId(id);
            skill.setName(name);
            skill.setDescription(description != null ? description : "");
            skill.setCategory("custom");

            skillManager.registerGlobalSkill(skill);
            skillManager.saveGlobalSkillToFile(skill);

            log.info("技能创建成功: {}", id);
            return ResponseEntity.ok(toMap(skill));
        } catch (Exception e) {
            log.error("创建技能失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("创建失败: " + e.getMessage()));
        }
    }

    /**
     * AI 辅助生成技能
     * 根据用户描述自动生成技能定义
     *
     * @param request 包含 description（技能描述）和可选的 category（分类）
     * @return 生成的技能建议
     */
    @PostMapping("/generate")
    @Operation(summary = "AI生成技能", description = "根据描述自动生成技能定义")
    @PreAuthorize("hasAuthority('PERM_skills:manage') or hasRole('ADMIN')")
    public ResponseEntity<?> generateSkill(@RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            if (description == null || description.isEmpty()) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("请提供技能描述"));
            }

            String category = request.getOrDefault("category", "custom");

            // 根据描述生成技能定义
            Map<String, Object> generated = new java.util.HashMap<>();
            String id = description.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-|-$)", "").toLowerCase();
            if (id.length() > 30) id = id.substring(0, 30);

            generated.put("id", id);
            generated.put("name", extractName(description));
            generated.put("description", description);
            generated.put("category", category);
            generated.put("triggerPattern", generateTriggerPattern(description));
            generated.put("content", generateSkillContent(description));
            generated.put("suggestions", generateSuggestions(description));

            return ResponseEntity.ok(generated);
        } catch (Exception e) {
            log.error("生成技能失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("生成失败: " + e.getMessage()));
        }
    }

    /** 从描述中提取技能名称 */
    private String extractName(String description) {
        // 取前20个字符作为名称
        String name = description.replaceAll("[，。！？、；：]", "").trim();
        if (name.length() > 20) name = name.substring(0, 20);
        return name;
    }

    /** 根据描述生成触发模式 */
    private String generateTriggerPattern(String description) {
        List<String> patterns = new ArrayList<>();
        // 提取关键词作为触发模式
        String[] keywords = description.split("[，,、\\s]+");
        for (String kw : keywords) {
            kw = kw.trim();
            if (kw.length() >= 2 && kw.length() <= 10) {
                patterns.add(kw);
            }
        }
        if (patterns.isEmpty()) {
            patterns.add(description.substring(0, Math.min(5, description.length())));
        }
        return String.join(",", patterns.subList(0, Math.min(5, patterns.size())));
    }

    /** 根据描述生成技能内容模板 */
    private String generateSkillContent(String description) {
        return String.format("""
            # %s

            ## 技能描述
            %s

            ## 触发条件
            当用户请求与该技能相关的任务时自动触发

            ## 执行步骤
            1. 分析用户需求
            2. 准备必要的资源和上下文
            3. 执行核心逻辑
            4. 验证结果
            5. 输出结果

            ## 注意事项
            - 确保输入参数的有效性
            - 处理异常情况
            - 记录执行日志
            """, extractName(description), description);
    }

    /** 生成优化建议 */
    private List<String> generateSuggestions(String description) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议添加详细的输入参数说明");
        suggestions.add("建议定义明确的输出格式");
        suggestions.add("建议添加错误处理逻辑");
        suggestions.add("建议设置合理的超时时间");
        return suggestions;
    }

    /**
     * 删除技能
     *
     * @param id 技能ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除技能")
    @PreAuthorize("hasAuthority('PERM_skills:manage') or hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Skill existing = skillManager.getGlobalSkill(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            skillManager.removeGlobalSkill(id);
            skillManager.deleteGlobalSkillFile(id);

            log.info("技能删除成功: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("删除技能失败: {}", id, e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 将 Skill 转换为 Map
     */
    private Map<String, Object> toMap(Skill skill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", skill.getId());
        map.put("name", skill.getName());
        map.put("description", skill.getDescription());
        map.put("category", skill.getCategory());
        map.put("triggerPattern", skill.getTriggerPattern());
        map.put("content", skill.getPrompt()); // 技能内容（prompt模板）
        map.put("usageCount", skill.getUsageCount());
        return map;
    }
}
