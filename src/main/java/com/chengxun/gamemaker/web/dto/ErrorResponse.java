package com.chengxun.gamemaker.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 错误响应 DTO
 * 统一的 API 错误响应格式
 *
 * @author chengxun
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** 是否成功（固定为 false） */
    private boolean success = false;

    /** HTTP 状态码 */
    private int status;

    /** 错误代码 */
    private String code;

    /** 错误消息 */
    private String message;

    /** 请求路径 */
    private String path;

    /** 时间戳 */
    private LocalDateTime timestamp;

    /** 字段验证错误（可选） */
    private Map<String, String> fieldErrors;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String code, String message) {
        this.success = false;
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String code, String message, String path, LocalDateTime timestamp) {
        this.success = false;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }

    // ===== 静态工厂方法 =====

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(400, "BAD_REQUEST", message);
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "NOT_FOUND", message);
    }

    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse(403, "FORBIDDEN", message);
    }

    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse(401, "UNAUTHORIZED", message);
    }

    public static ErrorResponse internal(String message) {
        return new ErrorResponse(500, "INTERNAL_ERROR", message);
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "CONFLICT", message);
    }

    public static ErrorResponse gone(String message) {
        return new ErrorResponse(410, "GONE", message);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}
