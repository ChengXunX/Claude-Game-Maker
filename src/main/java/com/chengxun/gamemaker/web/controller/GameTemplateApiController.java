package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.GameTemplateService;
import com.chengxun.gamemaker.service.GameTemplateService.GameTemplate;
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
 * 游戏模板 API 控制器
 * 提供游戏模板的查询、匹配、创建、更新和删除接口
 *
 * 操作维度：系统级
 * 权限要求：
 * - 查询模板：登录用户即可
 * - 创建/更新/删除模板：需要 projects:manage 权限
 *
 * @author chengxun
 * @since 1.0.0
 */

/**
 * 游戏模板 API 控制器
 * 提供游戏模板的查询和匹配接口
 *
 * 操作维度：系统级
 * 权限要求：登录用户即可
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/game-templates")
@Tag(name = "游戏模板", description = "游戏模板相关接口")
public class GameTemplateApiController {

    private static final Logger log = LoggerFactory.getLogger(GameTemplateApiController.class);

    private final GameTemplateService gameTemplateService;

    public GameTemplateApiController(GameTemplateService gameTemplateService) {
        this.gameTemplateService = gameTemplateService;
    }

    /**
     * 获取所有游戏模板
     *
     * @return 模板列表
     */
    @GetMapping
    @Operation(summary = "获取所有模板")
    public ResponseEntity<List<Map<String, Object>>> getAllTemplates() {
        List<GameTemplate> templates = gameTemplateService.getAllTemplates();

        List<Map<String, Object>> result = templates.stream()
            .map(this::toMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<?> getTemplate(@PathVariable String id) {
        GameTemplate template = gameTemplateService.getTemplate(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toMap(template));
    }

    /**
     * 根据描述匹配模板
     *
     * @param description 游戏描述
     * @return 匹配的模板列表
     */
    @GetMapping("/match")
    @Operation(summary = "根据描述匹配模板")
    public ResponseEntity<List<Map<String, Object>>> matchTemplates(
            @RequestParam String description) {

        List<GameTemplate> matched = gameTemplateService.matchTemplates(description);

        List<Map<String, Object>> result = matched.stream()
            .map(this::toMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取最佳匹配模板
     *
     * @param description 游戏描述
     * @return 最佳匹配模板
     */
    @GetMapping("/best-match")
    @Operation(summary = "获取最佳匹配模板")
    public ResponseEntity<?> getBestMatch(@RequestParam String description) {
        GameTemplate bestMatch = gameTemplateService.getBestMatch(description);
        if (bestMatch == null) {
            return ResponseEntity.ok(Map.of("message", "未找到匹配的模板"));
        }
        return ResponseEntity.ok(toMap(bestMatch));
    }

    /**
     * 创建游戏模板
     * 需要 projects:manage 权限
     *
     * @param request 模板信息
     * @return 创建的模板
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    @Operation(summary = "创建游戏模板")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> request) {
        try {
            String id = (String) request.get("id");
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String skillName = (String) request.get("skillName");
            String content = (String) request.get("content");

            @SuppressWarnings("unchecked")
            List<String> keywords = request.get("keywords") != null
                ? (List<String>) request.get("keywords")
                : List.of();

            // 验证必填字段
            if (id == null || id.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "模板ID不能为空"));
            }
            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "模板名称不能为空"));
            }

            // 检查ID是否已存在
            if (gameTemplateService.templateExists(id)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "模板ID已存在: " + id));
            }

            GameTemplate template = gameTemplateService.createTemplate(id, name, description, keywords, skillName, content);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "模板创建成功");
            result.put("template", toMap(template));

            log.info("游戏模板创建成功: {} - {}", id, name);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建游戏模板失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新游戏模板
     * 需要 projects:manage 权限
     *
     * @param id 模板ID
     * @param request 模板信息
     * @return 更新的模板
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    @Operation(summary = "更新游戏模板")
    public ResponseEntity<?> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            // 检查模板是否存在
            if (!gameTemplateService.templateExists(id)) {
                return ResponseEntity.notFound().build();
            }

            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String skillName = (String) request.get("skillName");
            String content = (String) request.get("content");

            @SuppressWarnings("unchecked")
            List<String> keywords = request.get("keywords") != null
                ? (List<String>) request.get("keywords")
                : null;

            // 获取现有模板
            GameTemplate existing = gameTemplateService.getTemplate(id);

            // 使用新值或保留旧值
            String newName = name != null ? name : existing.getName();
            String newDescription = description != null ? description : existing.getDescription();
            String newSkillName = skillName != null ? skillName : existing.getSkillName();
            String newContent = content != null ? content : existing.getContent();
            List<String> newKeywords = keywords != null ? keywords : existing.getKeywords();

            GameTemplate template = gameTemplateService.updateTemplate(id, newName, newDescription, newKeywords, newSkillName, newContent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "模板更新成功");
            result.put("template", toMap(template));

            log.info("游戏模板更新成功: {} - {}", id, newName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("更新游戏模板失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除游戏模板
     * 需要 projects:manage 权限
     *
     * @param id 模板ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    @Operation(summary = "删除游戏模板")
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        try {
            // 检查模板是否存在
            if (!gameTemplateService.templateExists(id)) {
                return ResponseEntity.notFound().build();
            }

            boolean deleted = gameTemplateService.deleteTemplate(id);
            if (deleted) {
                log.info("游戏模板删除成功: {}", id);
                return ResponseEntity.ok(Map.of("success", true, "message", "模板删除成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "删除失败"));
            }
        } catch (Exception e) {
            log.error("删除游戏模板失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 将模板转换为 Map
     */
    private Map<String, Object> toMap(GameTemplate template) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", template.getId());
        map.put("name", template.getName());
        map.put("description", template.getDescription());
        map.put("keywords", template.getKeywords());
        map.put("skillName", template.getSkillName());
        if (template.getContent() != null) {
            map.put("content", template.getContent());
        }
        return map;
    }
}
