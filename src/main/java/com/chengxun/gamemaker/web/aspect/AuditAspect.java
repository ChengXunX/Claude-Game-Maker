package com.chengxun.gamemaker.web.aspect;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一审计日志切面
 * 自动记录所有标注 @Auditable 的方法调用
 *
 * @author chengxun
 * @since 2.0.0
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final OperationLogService operationLogService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AuditAspect(OperationLogService operationLogService, UserService userService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /**
     * 审计注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Auditable {
        /** 操作类型 */
        String action();
        /** 操作描述 */
        String description() default "";
        /** 是否记录参数 */
        boolean logParams() default true;
        /** 是否记录返回值 */
        boolean logResult() default false;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        String action = auditable.action();
        String description = auditable.description();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 获取当前用户
        Long userId = null;
        String username = "system";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
                // 尝试获取用户ID
                try {
                    User user = userService.getUserByUsername(username);
                    if (user != null) {
                        userId = user.getId();
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // 忽略
        }

        // 记录请求参数
        String requestParams = null;
        if (auditable.logParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 过滤掉 Authentication 等不可序列化的参数
                    List<Object> serializableArgs = new java.util.ArrayList<>();
                    for (Object arg : args) {
                        if (arg != null && !(arg instanceof Authentication)) {
                            serializableArgs.add(arg);
                        }
                    }
                    if (!serializableArgs.isEmpty()) {
                        requestParams = SensitiveDataMasker.mask(
                            objectMapper.writeValueAsString(serializableArgs));
                    }
                }
            } catch (Exception e) {
                requestParams = "[序列化失败: " + e.getMessage() + "]";
            }
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;
        String responseData = null;

        try {
            result = joinPoint.proceed();

            // 记录响应数据
            if (auditable.logResult() && result != null) {
                try {
                    responseData = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    responseData = "[序列化失败: " + e.getMessage() + "]";
                }
            }

            return result;
        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 构建描述
            String fullDesc = String.format("[%s.%s] %s", className, methodName,
                description.isEmpty() ? action : description);

            // 确定目标类型（从类名推导）
            String targetType = className.replace("Controller", "").replace("Service", "");

            // 记录操作日志
            try {
                operationLogService.logAudit(
                    userId, username, action, targetType,
                    className + "." + methodName, fullDesc,
                    requestParams, responseData, duration, success, errorMessage
                );
            } catch (Exception e) {
                log.warn("Failed to write audit log: {}", e.getMessage());
            }

            log.info("Audit: user={}, action={}, method={}, duration={}ms, success={}",
                username, action, className + "." + methodName, duration, success);
        }
    }
}
