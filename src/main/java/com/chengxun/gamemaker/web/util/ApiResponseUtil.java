package com.chengxun.gamemaker.web.util;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * API响应工具类
 * 提供统一的HTTP响应构建方法，减少控制器中的重复代码
 *
 * 主要功能：
 * - 构建成功响应
 * - 构建错误响应
 * - 构建带数据的响应
 *
 * 使用示例：
 * ```java
 * // 简单成功响应
 * return ApiResponseUtil.success("操作成功");
 *
 * // 带数据的成功响应
 * return ApiResponseUtil.success("查询成功", data);
 *
 * // 错误响应
 * return ApiResponseUtil.error("操作失败");
 * ```
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class ApiResponseUtil {

    private ApiResponseUtil() {
        // 工具类不允许实例化
    }

    /**
     * 构建成功响应
     *
     * @param message 成功消息
     * @return 包含status和message的响应
     */
    public static ResponseEntity<Map<String, Object>> success(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", message);
        return ResponseEntity.ok(body);
    }

    /**
     * 构建带数据的成功响应
     *
     * @param message 成功消息
     * @param data 响应数据
     * @return 包含status、message和data的响应
     */
    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    /**
     * 构建带自定义字段的成功响应
     *
     * @param fields 自定义字段键值对
     * @return 包含自定义字段的响应
     */
    public static ResponseEntity<Map<String, Object>> success(Map<String, Object> fields) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.putAll(fields);
        return ResponseEntity.ok(body);
    }

    /**
     * 构建错误响应
     *
     * @param message 错误消息
     * @return 包含status和message的错误响应
     */
    public static ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 构建带状态码的错误响应
     *
     * @param statusCode HTTP状态码
     * @param message 错误消息
     * @return 包含status、code和message的错误响应
     */
    public static ResponseEntity<Map<String, Object>> error(int statusCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("code", statusCode);
        body.put("message", message);
        return ResponseEntity.status(statusCode).body(body);
    }

    /**
     * 构建带ID的成功响应
     *
     * @param message 成功消息
     * @param id 资源ID
     * @return 包含status、message和id的响应
     */
    public static ResponseEntity<Map<String, Object>> successWithId(String message, Long id) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", message);
        body.put("id", id);
        return ResponseEntity.ok(body);
    }

    /**
     * 构建带多个自定义字段的成功响应
     *
     * @param message 成功消息
     * @param keyValuePairs 键值对，必须是偶数个参数
     * @return 包含自定义字段的响应
     */
    public static ResponseEntity<Map<String, Object>> successWithFields(String message, Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("键值对参数必须是偶数个");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", message);

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            body.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
        }

        return ResponseEntity.ok(body);
    }
}
