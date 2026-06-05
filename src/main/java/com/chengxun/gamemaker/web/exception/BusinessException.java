package com.chengxun.gamemaker.web.exception;

/**
 * 业务逻辑异常
 * 当业务规则验证失败时抛出此异常
 *
 * @author chengxun
 * @since 1.0.0
 */
public class BusinessException extends RuntimeException {

    /** 错误代码 */
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 创建请求参数错误异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException("INVALID_ARGUMENT", message);
    }

    /**
     * 创建资源未找到异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message);
    }

    /**
     * 创建权限不足异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message);
    }

    /**
     * 创建并发冲突异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException conflict(String message) {
        return new BusinessException("CONFLICT", message);
    }
}
