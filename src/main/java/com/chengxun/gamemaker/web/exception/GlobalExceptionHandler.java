package com.chengxun.gamemaker.web.exception;

import com.chengxun.gamemaker.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准化的错误响应格式
 *
 * 覆盖的异常类型：
 * - 业务异常：ResourceNotFoundException、BusinessException
 * - 参数验证：MethodArgumentNotValidException、MissingServletRequestParameterException
 * - 认证授权：BadCredentialsException、AccessDeniedException
 * - HTTP 方法/媒体类型：HttpRequestMethodNotSupportedException、HttpMediaTypeNotSupportedException
 * - 数据库：DataIntegrityViolationException
 * - 文件上传：MaxUploadSizeExceededException
 * - 兜底：Exception（所有未捕获异常）
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理资源未找到异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 404 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源未找到: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            e.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理业务逻辑异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            e.getErrorCode(),
            e.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理参数验证异常（@Valid 注解触发）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应，包含字段验证错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数验证失败: {} - {}", request.getRequestURI(), e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "参数验证失败",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        error.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理认证异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 401 错误响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        log.warn("认证失败: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "UNAUTHORIZED",
            "用户名或密码错误",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * 处理权限不足异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 403 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "FORBIDDEN",
            "权限不足，无法执行此操作",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * 处理非法参数异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_ARGUMENT",
            e.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理空指针异常（请求参数缺失或内部逻辑错误）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException e, HttpServletRequest request) {
        log.warn("空指针异常（可能缺少参数）: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "MISSING_PARAMETER",
            "请求参数不完整，请检查必填字段",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理静态资源未找到异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 404 错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.debug("静态资源未找到: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            "请求的资源不存在",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理 HTTP 请求方法不支持异常（如用 POST 访问 GET 接口）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 405 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("HTTP方法不支持: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "METHOD_NOT_ALLOWED",
            "不支持的请求方法: " + request.getMethod(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * 处理不支持的媒体类型异常（如 Content-Type 不正确）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 415 错误响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("媒体类型不支持: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "UNSUPPORTED_MEDIA_TYPE",
            "不支持的媒体类型: " + e.getContentType(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    /**
     * 处理缺少请求参数异常（@RequestParam 标记 required=true 但未传）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "MISSING_PARAMETER",
            "缺少必填参数: " + e.getParameterName(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理请求参数类型不匹配异常（如期望数字传了字符串）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配: {} - {}", request.getRequestURI(), e.getMessage());
        String message = String.format("参数 '%s' 类型不正确，期望类型: %s",
            e.getName(),
            e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "TYPE_MISMATCH",
            message,
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理请求体不可读异常（JSON 格式错误等）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "MALFORMED_REQUEST",
            "请求体格式不正确，请检查 JSON 格式",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理缺少路径变量异常（@PathVariable 缺失）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariable(MissingPathVariableException e, HttpServletRequest request) {
        log.warn("缺少路径变量: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "MISSING_PATH_VARIABLE",
            "缺少路径参数: " + e.getVariableName(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理文件上传大小超限异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 413 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "FILE_TOO_LARGE",
            "上传文件大小超过限制",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * 处理数据库数据完整性违反异常（唯一约束、外键约束等）
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 409 错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性违反: {} - {}", request.getRequestURI(), e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "DATA_INTEGRITY_ERROR",
            "数据操作冲突，可能违反了唯一性约束或引用完整性",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * 处理页面未找到异常
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 404 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.debug("页面未找到: {} {}", request.getMethod(), request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            "请求的页面不存在",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理 Session 失效异常
     * 当用户的 Redis Session 被失效时，记录日志但不返回 401（避免误踢用户）
     * JWT 认证的 API 不依赖 Session，Session 失效不应影响 API 访问
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 500 错误响应（而不是 401）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        if (e.getMessage() != null && e.getMessage().contains("Session was invalidated")) {
            // Session 失效不影响 JWT 认证的 API，记录日志即可
            log.debug("Session 已失效（不影响API访问）: {}", request.getRequestURI());
            // 返回 200 而不是 401，避免前端误判为未登录
            // 前端使用 JWT 认证，Session 失效不应该导致重新登录
            return ResponseEntity.ok().build();
        }
        // 其他 IllegalStateException 按通用异常处理
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "系统内部错误，请稍后重试",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * 处理所有其他未捕获的异常（兜底）
     * 确保任何异常都不会返回裸的 500 错误
     *
     * @param e 异常对象
     * @param request HTTP 请求
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "系统内部错误，请稍后重试",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
