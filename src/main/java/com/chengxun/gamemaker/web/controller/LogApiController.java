package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.repository.OperationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志 API 控制器
 * 提供操作日志的查询和导出接口
 *
 * 操作维度：系统级
 * 权限要求：logs:view
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "操作日志", description = "操作日志相关接口")
public class LogApiController {

    private static final Logger log = LoggerFactory.getLogger(LogApiController.class);

    private final OperationLogRepository logRepository;

    public LogApiController(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 获取操作日志列表
     *
     * @param keyword 搜索关键词（可选）
     * @param action 操作类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 日志列表
     */
    @GetMapping
    @Operation(summary = "获取日志列表")
    @PreAuthorize("hasAuthority('PERM_logs:view') or hasRole('ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 构建动态查询条件
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate keywordPredicate = cb.or(
                    cb.like(root.get("username"), pattern),
                    cb.like(root.get("action"), pattern),
                    cb.like(root.get("targetName"), pattern),
                    cb.like(root.get("detail"), pattern),
                    cb.like(root.get("ipAddress"), pattern)
                );
                predicates.add(keywordPredicate);
            }

            // 操作类型筛选
            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }

            // 开始日期筛选
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDateTime start = LocalDateTime.parse(startDate.trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                } catch (Exception e) {
                    log.warn("Invalid startDate format: {}", startDate);
                }
            }

            // 结束日期筛选
            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDateTime end = LocalDateTime.parse(endDate.trim());
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                } catch (Exception e) {
                    log.warn("Invalid endDate format: {}", endDate);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OperationLog> logs = logRepository.findAll(spec, pageRequest);

        return ResponseEntity.ok(logs);
    }

    /**
     * 获取日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取日志详情")
    @PreAuthorize("hasAuthority('PERM_logs:view') or hasRole('ADMIN')")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        OperationLog operationLog = logRepository.findById(id).orElse(null);
        if (operationLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(operationLog);
    }

    /**
     * 导出日志为 CSV 格式
     *
     * @param keyword 搜索关键词（可选）
     * @param action 操作类型（可选）
     * @return CSV 文件
     */
    @GetMapping("/export")
    @Operation(summary = "导出日志")
    @PreAuthorize("hasAuthority('PERM_logs:view') or hasRole('ADMIN')")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action) {

        var logs = logRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        StringBuilder csv = new StringBuilder();
        csv.append("ID,时间,用户,操作,目标,详情,IP地址,状态\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (OperationLog operationLog : logs) {
            csv.append(operationLog.getId()).append(",");
            csv.append(operationLog.getCreatedAt() != null ? operationLog.getCreatedAt().format(formatter) : "").append(",");
            csv.append(escapeCsv(operationLog.getUsername())).append(",");
            csv.append(escapeCsv(operationLog.getAction())).append(",");
            csv.append(escapeCsv(operationLog.getTargetName())).append(",");
            csv.append(escapeCsv(operationLog.getDetail())).append(",");
            csv.append(escapeCsv(operationLog.getIpAddress())).append(",");
            csv.append(operationLog.getStatus()).append("\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "操作日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");

        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }

    /**
     * CSV 值转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
