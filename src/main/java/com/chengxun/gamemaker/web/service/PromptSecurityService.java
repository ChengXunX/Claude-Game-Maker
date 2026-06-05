package com.chengxun.gamemaker.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 提示词安全服务
 * 防止提示词注入、越权和敏感信息泄露
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class PromptSecurityService {

    private static final Logger log = LoggerFactory.getLogger(PromptSecurityService.class);

    /** 提示词注入模式 */
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
        // 尝试覆盖系统指令
        Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)ignore\\s+(all\\s+)?above", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)disregard\\s+(all\\s+)?previous", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)forget\\s+(all\\s+)?previous", Pattern.CASE_INSENSITIVE),

        // 尝试改变身份
        Pattern.compile("(?i)you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)act\\s+as\\s+if", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)pretend\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)new\\s+instructions?:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)system\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)assistant\\s*:", Pattern.CASE_INSENSITIVE),

        // 尝试执行代码
        Pattern.compile("(?i)execute\\s+code", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)run\\s+command", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)exec\\s*\\(", Pattern.CASE_INSENSITIVE),

        // 尝试访问敏感数据
        Pattern.compile("(?i)show\\s+(me\\s+)?(all\\s+)?passwords", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)show\\s+(me\\s+)?(all\\s+)?api\\s*keys", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)show\\s+(me\\s+)?(all\\s+)?secrets", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)dump\\s+(the\\s+)?database", Pattern.CASE_INSENSITIVE),

        // 尝试绕过限制
        Pattern.compile("(?i)bypass\\s+(all\\s+)?restrictions", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)disable\\s+(all\\s+)?safety", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)override\\s+(all\\s+)?rules", Pattern.CASE_INSENSITIVE)
    );

    /** 敏感信息模式 */
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
        // API Keys
        Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)sk-[a-zA-Z0-9_-]{20,}", Pattern.CASE_INSENSITIVE),

        // 密码
        Pattern.compile("(?i)(password|passwd|pwd)\\s*[:=]\\s*['\"]?([^'\"\\s]+)", Pattern.CASE_INSENSITIVE),

        // Token
        Pattern.compile("(?i)(token|secret)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})", Pattern.CASE_INSENSITIVE),

        // 数据库连接
        Pattern.compile("(?i)jdbc:[a-zA-Z]+://[^'\"\\s]+", Pattern.CASE_INSENSITIVE),

        // 私钥
        Pattern.compile("-----BEGIN\\s+(RSA\\s+)?PRIVATE\\s+KEY-----", Pattern.CASE_INSENSITIVE)
    );

    /** 权限提升模式 */
    private static final List<Pattern> PRIVILEGE_ESCALATION_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)grant\\s+(me\\s+)?admin", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)give\\s+(me\\s+)?admin", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)make\\s+(me\\s+)?admin", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)elevate\\s+(my\\s+)?privileges", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)i\\s+(am|should\\s+be)\\s+admin", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 清理提示词，移除潜在的注入内容
     *
     * @param input 原始输入
     * @return 清理后的输入
     */
    public String sanitizePrompt(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // 1. 移除提示词注入
        for (Pattern pattern : INJECTION_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[已过滤]");
        }

        // 2. 移除敏感信息
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[敏感信息已隐藏]");
        }

        // 3. 移除权限提升尝试
        for (Pattern pattern : PRIVILEGE_ESCALATION_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[权限提升已阻止]");
        }

        return result;
    }

    /**
     * 检测提示词注入
     *
     * @param input 输入内容
     * @return 如果检测到注入返回 true
     */
    public boolean detectInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("Prompt injection detected: {}", input.substring(0, Math.min(100, input.length())));
                return true;
            }
        }

        return false;
    }

    /**
     * 检测敏感信息泄露
     *
     * @param content 内容
     * @return 如果检测到敏感信息返回 true
     */
    public boolean detectSensitiveInfo(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("Sensitive information detected in content");
                return true;
            }
        }

        return false;
    }

    /**
     * 检测权限提升尝试
     *
     * @param input 输入内容
     * @return 如果检测到权限提升返回 true
     */
    public boolean detectPrivilegeEscalation(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : PRIVILEGE_ESCALATION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("Privilege escalation attempt detected: {}", input.substring(0, Math.min(100, input.length())));
                return true;
            }
        }

        return false;
    }

    /**
     * 完整的安全检查
     *
     * @param input 输入内容
     * @return 安全检查结果
     */
    public SecurityCheckResult checkSecurity(String input) {
        SecurityCheckResult result = new SecurityCheckResult();
        result.setInput(input);
        result.setSafe(true);

        if (detectInjection(input)) {
            result.setSafe(false);
            result.addIssue("prompt_injection", "检测到提示词注入尝试");
        }

        if (detectSensitiveInfo(input)) {
            result.setSafe(false);
            result.addIssue("sensitive_info", "检测到敏感信息");
        }

        if (detectPrivilegeEscalation(input)) {
            result.setSafe(false);
            result.addIssue("privilege_escalation", "检测到权限提升尝试");
        }

        return result;
    }

    /**
     * 过滤输出内容，移除敏感信息
     *
     * @param output 原始输出
     * @return 过滤后的输出
     */
    public String filterOutput(String output) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        String result = output;

        // 过滤敏感信息
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[已隐藏]");
        }

        return result;
    }

    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        private String input;
        private boolean safe;
        private Map<String, String> issues = new HashMap<>();

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public boolean isSafe() { return safe; }
        public void setSafe(boolean safe) { this.safe = safe; }

        public Map<String, String> getIssues() { return issues; }
        public void setIssues(Map<String, String> issues) { this.issues = issues; }

        public void addIssue(String type, String description) {
            this.issues.put(type, description);
        }

        public String getIssueSummary() {
            if (issues.isEmpty()) {
                return "无安全问题";
            }
            return String.join("; ", issues.values());
        }
    }
}
