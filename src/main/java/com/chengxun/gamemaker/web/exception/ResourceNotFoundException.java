package com.chengxun.gamemaker.web.exception;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出此异常
 *
 * @author chengxun
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s 未找到，ID: %d", resourceName, id));
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(String.format("%s 未找到，标识: %s", resourceName, identifier));
    }
}
