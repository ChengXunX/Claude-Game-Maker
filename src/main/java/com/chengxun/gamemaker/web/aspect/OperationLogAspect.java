package com.chengxun.gamemaker.web.aspect;

import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 自动记录所有 Controller 的写操作（POST/PUT/DELETE）
 *
 * 通过拦截 HTTP 方法自动判断操作类型，无需在每个 Controller 中手动调用 logService
 *
 * @author chengxun
 * @since 1.0.0
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 拦截所有 Controller 的写操作
     */
    @Around("execution(* com.chengxun.gamemaker.web.controller..*.*(..)) && " +
            "@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 只记录写操作
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();

        // 只记录 POST、PUT、DELETE 操作
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "FAILURE";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                saveLog(joinPoint, request, method, status, errorMessage, duration, result);
            } catch (Exception e) {
                log.warn("Failed to save operation log: {}", e.getMessage());
            }
        }
    }

    /**
     * 保存操作日志
     */
    private void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request,
                         String httpMethod, String status, String errorMessage,
                         long duration, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 生成操作类型
        String action = generateAction(httpMethod, request.getRequestURI(), method);

        // 获取当前用户
        String username = null;
        Long userId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
        }

        // 提取目标信息
        String targetType = extractTargetType(request.getRequestURI());
        String targetName = extractTargetName(joinPoint, request);

        // 提取请求参数
        String requestParams = extractRequestParams(joinPoint);

        // 提取响应数据
        String responseData = extractResponseData(result);

        // 构建日志
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetName(targetName);
        operationLog.setDetail(String.format("%s %s", httpMethod, request.getRequestURI()));
        operationLog.setIpAddress(getClientIp(request));
        operationLog.setStatus(status);
        operationLog.setErrorMessage(errorMessage);
        operationLog.setDurationMs(duration);
        operationLog.setCreatedAt(LocalDateTime.now());
        if (requestParams != null) {
            operationLog.setRequestParams(requestParams);
        }
        if (responseData != null) {
            operationLog.setResponseData(responseData);
        }

        // 异步入队，不阻塞请求线程
        operationLogService.enqueue(operationLog);
    }

    /**
     * 从方法返回值中提取响应数据
     * 将返回对象序列化为 JSON，限制最大 2000 字符
     */
    private String extractResponseData(Object result) {
        if (result == null) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 处理 ResponseEntity 包装
            Object body = result;
            if (result instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
                body = responseEntity.getBody();
            }
            if (body == null) {
                return null;
            }
            String json = mapper.writeValueAsString(body);
            return json.length() > 2000 ? json.substring(0, 2000) + "..." : json;
        } catch (Exception e) {
            return "[序列化失败]";
        }
    }

    /**
     * 根据 HTTP 方法和 URL 生成操作类型
     */
    private String generateAction(String httpMethod, String uri, Method method) {
        String resource = extractResource(uri);

        return switch (httpMethod) {
            case "POST" -> "CREATE_" + resource;
            case "PUT" -> "UPDATE_" + resource;
            case "DELETE" -> "DELETE_" + resource;
            default -> "OPERATE_" + resource;
        };
    }

    /**
     * 从 URL 中提取资源类型
     */
    private String extractResource(String uri) {
        // 移除 /api/ 前缀
        String path = uri;
        if (path.startsWith("/api/")) {
            path = path.substring(5);
        }

        // 提取第一段作为资源类型
        String[] parts = path.split("/");
        if (parts.length > 0) {
            String resource = parts[0].toUpperCase();
            // 资源名称映射
            return switch (resource) {
                case "PROJECTS" -> "PROJECT";
                case "AGENTS" -> "AGENT";
                case "TOKENS" -> "TOKEN";
                case "ROLES" -> "ROLE";
                case "USERS" -> "USER";
                case "SKILLS" -> "SKILL";
                case "ALERTS" -> "ALERT";
                case "NOTIFICATIONS" -> "NOTIFICATION";
                case "INTERVENTIONS" -> "INTERVENTION";
                case "WORKFLOW" -> "WORKFLOW";
                case "SEARCH" -> "SEARCH";
                case "CONFIGS" -> "CONFIG";
                case "LOGS" -> "LOG";
                case "V1" -> extractV1Resource(parts);
                default -> resource;
            };
        }
        return "SYSTEM";
    }

    /**
     * 提取 /api/v1/xxx 格式的资源类型
     */
    private String extractV1Resource(String[] parts) {
        if (parts.length > 1) {
            return parts[1].toUpperCase();
        }
        return "SYSTEM";
    }

    /**
     * 从 URL 中提取目标类型
     */
    private String extractTargetType(String uri) {
        if (uri.contains("/projects")) return "PROJECT";
        if (uri.contains("/agents")) return "AGENT";
        if (uri.contains("/tokens")) return "TOKEN";
        if (uri.contains("/roles")) return "ROLE";
        if (uri.contains("/users")) return "USER";
        if (uri.contains("/skills")) return "SKILL";
        return "SYSTEM";
    }

    /**
     * 从请求中提取目标名称
     * 优先从请求参数中获取 name/title 等字段，其次从 URL 路径中提取
     */
    private String extractTargetName(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        // 1. 尝试从请求参数中获取有意义的名称
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> body = (java.util.Map<String, Object>) arg;
                    // 优先使用 name 字段
                    Object name = body.get("name");
                    if (name == null) name = body.get("displayName");
                    if (name == null) name = body.get("title");
                    if (name == null) name = body.get("username");
                    if (name != null) {
                        return name.toString();
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. 从 URL 路径中提取
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        // 返回最后一段非空、非 api/v1 的段
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty() && !parts[i].equals("api") && !parts[i].equals("v1")) {
                return parts[i];
            }
        }
        return uri;
    }

    /**
     * 从方法参数中提取请求体
     * 将 Map/对象类型的参数序列化为 JSON，限制最大 2000 字符
     */
    private String extractRequestParams(ProceedingJoinPoint joinPoint) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object[] args = joinPoint.getArgs();
            java.util.Map<String, Object> paramMap = new java.util.LinkedHashMap<>();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof org.springframework.security.core.Authentication
                    || arg instanceof org.springframework.web.multipart.MultipartFile) {
                    continue;
                }
                if (arg != null) {
                    String key = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                    paramMap.put(key, arg);
                }
            }
            if (paramMap.isEmpty()) return null;
            String json = mapper.writeValueAsString(paramMap);
            return json.length() > 2000 ? json.substring(0, 2000) + "..." : json;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后取第一个 IP
            int index = ip.indexOf(',');
            if (index > 0) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
