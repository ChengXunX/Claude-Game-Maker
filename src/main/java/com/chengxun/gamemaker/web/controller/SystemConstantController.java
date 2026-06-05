package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.ErrorResponse;
import com.chengxun.gamemaker.web.entity.SystemConstant;
import com.chengxun.gamemaker.web.service.SystemConstantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统常量管理控制器
 */
@Controller
@RequestMapping({"/constants", "/api/constants"})
@PreAuthorize("hasAuthority('PERM_admin:manage')")
public class SystemConstantController {

    private static final Logger log = LoggerFactory.getLogger(SystemConstantController.class);

    private final SystemConstantService constantService;

    public SystemConstantController(SystemConstantService constantService) {
        this.constantService = constantService;
    }

    @GetMapping
    public String constantsPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("constants", constantService.getAll());
        model.addAttribute("groups", constantService.getGroups());
        return "constants/index";
    }

    @GetMapping("/api/all")
    @ResponseBody
    public List<SystemConstant> getAll() {
        return constantService.getAll();
    }

    @GetMapping("/api/group/{group}")
    @ResponseBody
    public List<SystemConstant> getByGroup(@PathVariable String group) {
        return constantService.getByGroup(group);
    }

    @GetMapping("/api/groups")
    @ResponseBody
    public Set<String> getGroups() {
        return constantService.getGroups();
    }

    @PostMapping("/api/update")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody Map<String, String> body) {
        try {
            String key = body.get("key");
            String value = body.get("value");
            SystemConstant updated = constantService.update(key, value);
            return ResponseEntity.ok(Map.of("success", true, "constant", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/api/batch-update")
    @ResponseBody
    public ResponseEntity<?> batchUpdate(@RequestBody Map<String, String> updates) {
        List<SystemConstant> results = constantService.batchUpdate(updates);
        return ResponseEntity.ok(Map.of("success", true, "updated", results.size()));
    }

    @PostMapping("/api/reset/{key}")
    @ResponseBody
    public ResponseEntity<?> resetToDefault(@PathVariable String key) {
        SystemConstant reset = constantService.resetToDefault(key);
        return ResponseEntity.ok(Map.of("success", true, "constant", reset));
    }

    @PostMapping("/api/reset-all")
    @ResponseBody
    public ResponseEntity<?> resetAll() {
        constantService.resetAllToDefault();
        return ResponseEntity.ok(Map.of("success", true, "message", "所有常量已恢复默认值"));
    }

    @PostMapping("/api/reload")
    @ResponseBody
    public ResponseEntity<?> reload() {
        constantService.reloadCache();
        return ResponseEntity.ok(Map.of("success", true, "message", "常量缓存已重新加载"));
    }
}
