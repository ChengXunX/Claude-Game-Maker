package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.IterationAdaptService;
import com.chengxun.gamemaker.web.entity.IterationAdaptRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 迭代适应控制器
 * 提供项目阶段判断、策略推荐和应用接口
 *
 * @author chengxun
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/iteration-adapt")
@Tag(name = "迭代适应", description = "迭代适应策略接口")
public class IterationAdaptController {

    @Autowired
    private IterationAdaptService adaptService;

    @GetMapping("/recommendation/{projectId}")
    @Operation(summary = "获取迭代建议", description = "根据项目阶段获取推荐的迭代策略")
    @PreAuthorize("hasAnyAuthority('PERM_iteration:view', 'PERM_iteration:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getRecommendation(@PathVariable String projectId) {
        return ResponseEntity.ok(adaptService.getRecommendation(projectId));
    }

    @PostMapping("/apply/{projectId}")
    @Operation(summary = "应用迭代建议", description = "将推荐的迭代策略应用到项目")
    @PreAuthorize("hasAnyAuthority('PERM_iteration:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<IterationAdaptRecord> applyRecommendation(@PathVariable String projectId) {
        IterationAdaptRecord record = adaptService.applyRecommendation(projectId);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/history/{projectId}")
    @Operation(summary = "调整历史", description = "获取项目的迭代策略调整历史")
    @PreAuthorize("hasAnyAuthority('PERM_iteration:view', 'PERM_iteration:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<IterationAdaptRecord>> getHistory(@PathVariable String projectId) {
        return ResponseEntity.ok(adaptService.getHistory(projectId));
    }
}
