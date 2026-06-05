package com.chengxun.gamemaker.web.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具类
 * 提供日期时间处理的通用方法
 *
 * 主要功能：
 * - 格式化日期时间
 * - 计算时间差
 * - 获取当前时间
 * - 时间比较
 *
 * 使用示例：
 * ```java
 * // 格式化日期时间
 * String formatted = DateTimeUtil.formatNow();
 *
 * // 计算时间差
 * long minutes = DateTimeUtil.minutesBetween(start, end);
 *
 * // 判断是否超时
 * boolean timeout = DateTimeUtil.isTimeout(createdAt, 30);
 * ```
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class DateTimeUtil {

    /** 默认日期时间格式 */
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 日期格式 */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** 时间格式 */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /** 紧凑格式 */
    public static final String COMPACT_FORMAT = "yyyyMMddHHmmss";

    private DateTimeUtil() {
        // 工具类不允许实例化
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 格式化当前时间
     *
     * @return 格式化后的时间字符串
     */
    public static String formatNow() {
        return format(LocalDateTime.now());
    }

    /**
     * 格式化日期时间
     *
     * @param dateTime 日期时间
     * @return 格式化后的时间字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_FORMAT);
    }

    /**
     * 格式化日期时间
     *
     * @param dateTime 日期时间
     * @param pattern 格式模式
     * @return 格式化后的时间字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 计算两个时间之间的分钟数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 分钟数
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的秒数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 秒数
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 计算两个时间之间的小时数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 小时数
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的天数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 天数
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 判断是否超时
     *
     * @param createdAt 创建时间
     * @param timeoutMinutes 超时时间（分钟）
     * @return 是否超时
     */
    public static boolean isTimeout(LocalDateTime createdAt, int timeoutMinutes) {
        if (createdAt == null) {
            return false;
        }
        return minutesBetween(createdAt, now()) > timeoutMinutes;
    }

    /**
     * 判断是否在指定时间范围内
     *
     * @param dateTime 要检查的时间
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime startTime, LocalDateTime endTime) {
        if (dateTime == null || startTime == null || endTime == null) {
            return false;
        }
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    /**
     * 获取友好的时间描述
     *
     * @param dateTime 日期时间
     * @return 友好的时间描述，如"刚刚"、"5分钟前"、"2小时前"等
     */
    public static String getFriendlyDescription(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        long seconds = secondsBetween(dateTime, now());

        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "分钟前";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + "小时前";
        } else if (seconds < 604800) {
            long days = seconds / 86400;
            return days + "天前";
        } else {
            return format(dateTime, DATE_FORMAT);
        }
    }

    /**
     * 格式化持续时间
     *
     * @param durationMs 持续时间（毫秒）
     * @return 格式化后的持续时间，如"1小时30分钟"、"2分钟15秒"
     */
    public static String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "毫秒";
        }

        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) {
            if (remainingSeconds == 0) {
                return minutes + "分钟";
            }
            return minutes + "分钟" + remainingSeconds + "秒";
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + "小时";
        }
        return hours + "小时" + remainingMinutes + "分钟";
    }
}
