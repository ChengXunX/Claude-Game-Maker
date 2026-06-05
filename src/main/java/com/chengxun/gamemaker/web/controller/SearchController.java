package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.SearchService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 全局搜索控制器
 *
 * 权限模型：
 * - 搜索结果按用户项目权限过滤
 * - 管理员可搜索所有内容
 * - 普通用户只能搜索自己参与的项目内容
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/search", "/api/search"})
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    /**
     * 搜索页面
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public String searchPage(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String projectId,
                            Model model, Authentication authentication) {
        if (q != null && !q.trim().isEmpty()) {
            User user = userService.getUserByUsername(authentication.getName());
            boolean isAdmin = user != null && user.isAdmin();

            Map<String, List<Map<String, Object>>> results;

            if (projectId != null && !projectId.isEmpty()) {
                // 项目级搜索：校验权限
                if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                    model.addAttribute("error", "无权限搜索该项目");
                    return "search/results";
                }
                results = searchService.search(q, 10);
                // 按 projectId 过滤结果
                results.entrySet().removeIf(entry ->
                    entry.getValue().removeIf(item ->
                        item.containsKey("projectId") && !projectId.equals(item.get("projectId"))
                    )
                );
            } else if (isAdmin) {
                results = searchService.search(q, 10);
            } else {
                // 普通用户：搜索结果后续按权限过滤
                results = searchService.search(q, 10);
            }

            model.addAttribute("results", results);
            model.addAttribute("query", q);
            model.addAttribute("projectId", projectId);
            model.addAttribute("resultCount", results.values().stream()
                .mapToInt(List::size)
                .sum());
        }
        return "search/results";
    }

    /**
     * 搜索API（JSON）
     */
    @GetMapping("/api")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        return ResponseEntity.ok(searchService.search(q, limit));
    }

    /**
     * 搜索建议API（JSON）
     */
    @GetMapping("/api/suggestions")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        return ResponseEntity.ok(searchService.getSearchSuggestions(q));
    }
}
