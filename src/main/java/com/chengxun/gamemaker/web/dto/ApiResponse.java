package com.chengxun.gamemaker.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一 API 响应格式
 * 所有 REST API 返回此格式
 *
 * @author chengxun
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /** 时间戳 */
    private LocalDateTime timestamp;

    /** 请求追踪 ID */
    private String traceId;

    /** 分页信息 */
    private Pagination pagination;

    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
        // 自动设置 traceId
        String traceId = MDC.get("traceId");
        this.traceId = traceId != null ? traceId : UUID.randomUUID().toString().substring(0, 8);
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 200;
        response.message = "success";
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 200;
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error(403, message);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public ApiResponse<T> withPagination(int page, int size, long total) {
        this.pagination = new Pagination(page, size, total);
        return this;
    }

    // Getters & Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Pagination getPagination() { return pagination; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    /**
     * 分页信息
     */
    public static class Pagination {
        private int page;
        private int size;
        private long total;
        private int totalPages;

        public Pagination(int page, int size, long total) {
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / size);
        }

        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
}
