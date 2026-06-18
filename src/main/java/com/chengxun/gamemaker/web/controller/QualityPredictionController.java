package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.QualityPredictionService;
import com.chengxun.gamemaker.web.entity.QualityPrediction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量预测控制器
 * 提供版本质量预测和历史查询接口
 *
 * @author chengxun
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/quality-prediction")
@Tag(name = "质量预测", description = "版本质量预测接口")
public class QualityPredictionController {

    @Autowired
    private QualityPredictionService predictionService;

    @GetMapping("/{projectId}")
    @Operation(summary = "获取最新预测", description = "获取项目当前版本的质量预测")
    @PreAuthorize("hasAnyAuthority('PERM_quality:view', 'PERM_quality:predict', 'PERM_projects:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<QualityPrediction> getLatest(@PathVariable String projectId) {
        QualityPrediction prediction = predictionService.getLatest(projectId);
        if (prediction == null) {
            prediction = predictionService.predict(projectId);
        }
        return ResponseEntity.ok(prediction);
    }

    @PostMapping("/{projectId}/predict")
    @Operation(summary = "执行预测", description = "对项目当前版本执行质量预测")
    @PreAuthorize("hasAnyAuthority('PERM_quality:predict', 'PERM_projects:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<QualityPrediction> predict(@PathVariable String projectId) {
        return ResponseEntity.ok(predictionService.predict(projectId));
    }

    @GetMapping("/{projectId}/history")
    @Operation(summary = "预测历史", description = "获取项目的质量预测历史")
    @PreAuthorize("hasAnyAuthority('PERM_quality:view', 'PERM_quality:predict', 'PERM_projects:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<QualityPrediction>> getHistory(@PathVariable String projectId) {
        return ResponseEntity.ok(predictionService.getHistory(projectId));
    }
}
