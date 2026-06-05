package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.SystemDiagnosticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统自检 API 控制器
 * 提供系统级别的健康检查和诊断接口
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/diagnostic")
@Tag(name = "系统自检", description = "系统健康检查和诊断接口")
public class DiagnosticApiController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticApiController.class);

    private final SystemDiagnosticService diagnosticService;

    public DiagnosticApiController(SystemDiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    /**
     * 执行完整系统自检
     *
     * @return 诊断结果
     */
    @PostMapping("/run")
    @Operation(summary = "执行自检", description = "执行完整系统自检并返回结果")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> runDiagnostic() {
        log.info("手动触发系统自检");
        Map<String, Object> result = diagnosticService.runFullDiagnostic();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最近一次自检结果
     *
     * @return 诊断结果
     */
    @GetMapping("/result")
    @Operation(summary = "获取自检结果", description = "获取最近一次系统自检结果")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDiagnosticResult() {
        Map<String, Object> result = diagnosticService.getLastDiagnosticResult();
        return ResponseEntity.ok(result);
    }

    /**
     * 快速健康检查
     *
     * @return 简化的健康状态
     */
    @GetMapping("/quick")
    @Operation(summary = "快速健康检查", description = "返回简化的系统健康状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> quickCheck() {
        Map<String, Object> fullResult = diagnosticService.getLastDiagnosticResult();

        Map<String, Object> quickResult = new java.util.LinkedHashMap<>();
        quickResult.put("overallStatus", fullResult.get("overallStatus"));
        quickResult.put("timestamp", fullResult.get("timestamp"));

        // 提取各组件状态
        Map<String, Object> checks = (Map<String, Object>) fullResult.get("checks");
        if (checks != null) {
            Map<String, String> componentStatus = new java.util.LinkedHashMap<>();
            checks.forEach((key, value) -> {
                if (value instanceof Map) {
                    componentStatus.put(key, (String) ((Map<?, ?>) value).get("status"));
                }
            });
            quickResult.put("components", componentStatus);
        }

        return ResponseEntity.ok(quickResult);
    }

    /**
     * 获取 CPU 详细信息
     */
    @GetMapping("/details/cpu")
    @Operation(summary = "CPU 详情", description = "获取 CPU 详细信息，包括系统属性、JVM 参数等")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCpuDetails() {
        return ResponseEntity.ok(diagnosticService.getCpuDetails());
    }

    /**
     * 获取内存详细信息
     */
    @GetMapping("/details/memory")
    @Operation(summary = "内存详情", description = "获取内存池、GC 等详细信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMemoryDetails() {
        return ResponseEntity.ok(diagnosticService.getMemoryDetails());
    }

    /**
     * 获取线程详细信息
     */
    @GetMapping("/details/threads")
    @Operation(summary = "线程详情", description = "获取线程列表、状态分布、堆栈信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getThreadDetails() {
        return ResponseEntity.ok(diagnosticService.getThreadDetails());
    }

    /**
     * 获取磁盘详细信息
     */
    @GetMapping("/details/disk")
    @Operation(summary = "磁盘详情", description = "获取各磁盘分区使用情况")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDiskDetails() {
        return ResponseEntity.ok(diagnosticService.getDiskDetails());
    }
}
