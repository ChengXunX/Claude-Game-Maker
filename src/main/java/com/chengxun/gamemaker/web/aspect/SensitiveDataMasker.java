package com.chengxun.gamemaker.web.aspect;

import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具
 * 自动识别并脱敏日志中的敏感信息
 *
 * @author chengxun
 * @since 2.0.0
 */
public class SensitiveDataMasker {

    /** 手机号模式 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    /** 邮箱模式 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{2})\\w+@(\\w{2})\\w+(\\.\\w+)");

    /** API Key 模式（sk- 开头或长字符串） */
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(sk-[a-zA-Z0-9]{4})[a-zA-Z0-9]{20,}");

    /** JWT Token 模式 */
    private static final Pattern JWT_PATTERN = Pattern.compile("(eyJ[a-zA-Z0-9]{10})[a-zA-Z0-9_-]{20,}");

    /** 密码字段模式 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token|apikey|api_key)[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s,}]{4})[^\"'\\s,}]*",
        Pattern.CASE_INSENSITIVE);

    /** 身份证号模式 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{10}(\\d{4})");

    /** 银行卡号模式 */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})");

    /**
     * 对字符串进行脱敏处理
     */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;

        // 脱敏手机号
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");

        // 脱敏邮箱
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***$2***$3");

        // 脱敏 API Key
        result = API_KEY_PATTERN.matcher(result).replaceAll("$1****");

        // 脱敏 JWT
        result = JWT_PATTERN.matcher(result).replaceAll("$1****");

        // 脱敏密码字段
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1=****");

        // 脱敏身份证号
        result = ID_CARD_PATTERN.matcher(result).replaceAll("$1**********$2");

        // 脱敏银行卡号
        result = BANK_CARD_PATTERN.matcher(result).replaceAll("$1********$2");

        return result;
    }

    /**
     * 脱敏 API Key（专用方法）
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, atIndex));
        String domain = email.substring(atIndex);
        return prefix + "***" + domain;
    }

    /**
     * 脱敏姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
