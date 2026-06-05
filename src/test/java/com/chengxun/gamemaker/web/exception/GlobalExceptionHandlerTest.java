package com.chengxun.gamemaker.web.exception;

import com.chengxun.gamemaker.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 全局异常处理器单元测试
 * 测试GlobalExceptionHandler的各种异常处理
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Test
    void handleBusinessException_shouldReturnBadRequest() {
        // Given
        BusinessException ex = BusinessException.badRequest("参数错误");
        when(request.getRequestURI()).thenReturn("/api/test");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex, request);

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getCode());
        assertEquals("参数错误", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_shouldReturnNotFound() {
        // Given
        BusinessException ex = BusinessException.notFound("资源不存在");
        when(request.getRequestURI()).thenReturn("/api/test");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex, request);

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Test
    void handleResourceNotFound_shouldReturn404() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("用户", 1L);
        when(request.getRequestURI()).thenReturn("/api/users/1");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        // Then
        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        when(request.getRequestURI()).thenReturn("/api/login");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
        assertEquals("用户名或密码错误", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        // Given
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/admin");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        // Then
        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getCode());
        assertEquals("权限不足，无法执行此操作", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("无效参数");
        when(request.getRequestURI()).thenReturn("/api/test");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getCode());
        assertEquals("无效参数", response.getBody().getMessage());
    }

    @Test
    void handleException_shouldReturn500() {
        // Given
        Exception ex = new Exception("未知错误");
        when(request.getRequestURI()).thenReturn("/api/test");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(ex, request);

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("系统内部错误，请稍后重试", response.getBody().getMessage());
    }
}
