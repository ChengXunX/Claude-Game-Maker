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

        // 获取当前用户（避免数据库查询）
        String username = "system";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
            }
        } catch (Exception e) {
            // 忽略
        }

        // 记录参数
        String params = null;
        if (auditable.logParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    params = SensitiveDataMasker.mask(objectMapper.writeValueAsString(args));
                }
            } catch (Exception e) {
                params = "[序列化失败]";
            }
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
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
            if (!success) {
                fullDesc += " [FAILED: " + errorMessage + "]";
            }

            // 记录操作日志
            try {
                operationLogService.log(null, action, fullDesc, params, duration);
            } catch (Exception e) {
                log.warn("Failed to write audit log: {}", e.getMessage());
            }

            log.info("Audit: user={}, action={}, method={}, duration={}ms, success={}",
                username, action, className + "." + methodName, duration, success);
        }
    }
}
