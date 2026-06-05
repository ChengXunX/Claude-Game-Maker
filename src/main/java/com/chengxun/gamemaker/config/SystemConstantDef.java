package com.chengxun.gamemaker.config;

import java.lang.annotation.*;

/**
 * 系统常量声明注解
 * 标注在字段上，启动时自动同步到数据库
 *
 * 使用方式：
 *   @SystemConstantDef(
 *       key = "agent.max-idle-minutes",
 *       name = "最大空闲时间",
 *       description = "Agent 最大无响应时间",
 *       defaultValue = "30",
 *       valueType = "int",
 *       group = "agent",
 *       unit = "分钟",
 *       min = 1, max = 1440
 *   )
 *   private int maxIdleMinutes;
 *
 * @author chengxun
 * @since 2.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemConstantDef {
    /** 常量标识 */
    String key();
    /** 显示名称 */
    String name();
    /** 描述 */
    String description() default "";
    /** 默认值 */
    String defaultValue();
    /** 值类型：int, long, boolean, string */
    String valueType() default "string";
    /** 分组 */
    String group();
    /** 单位 */
    String unit() default "";
    /** 最小值 */
    long min() default Long.MIN_VALUE;
    /** 最大值 */
    long max() default Long.MAX_VALUE;
    /** 是否需要重启 */
    boolean requireRestart() default false;
}
