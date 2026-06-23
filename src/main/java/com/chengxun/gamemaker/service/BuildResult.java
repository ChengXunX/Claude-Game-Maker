package com.chengxun.gamemaker.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 构建结果数据类
 * 封装一次构建操作的完整结果信息，包括成功/失败状态、输出日志、错误信息和耗时。
 *
 * <p>使用静态工厂方法创建实例：</p>
 * <ul>
 *   <li>{@link #success(String, long, List)} 创建成功结果</li>
 *   <li>{@link #failure(String, String, long)} 创建失败结果</li>
 * </ul>
 *
 * @author chengxun
 * @since 1.0.0
 */
public class BuildResult {

    /** 构建是否成功 */
    private boolean success;

    /** 构建标准输出内容 */
    private String output;

    /** 构建错误输出内容 */
    private String error;

    /** 构建类型标识，如 "npm"、"maven" 等 */
    private String buildType;

    /** 构建耗时（毫秒） */
    private long durationMs;

    /** 构建过程中的警告信息列表 */
    private List<String> warnings;

    /**
     * 无参构造函数（用于序列化/反序列化）
     */
    public BuildResult() {
        this.warnings = new ArrayList<>();
    }

    /**
     * 全参构造函数
     *
     * @param success 是否成功
     * @param output 标准输出内容
     * @param error 错误输出内容
     * @param buildType 构建类型标识
     * @param durationMs 构建耗时（毫秒）
     * @param warnings 警告信息列表
     */
    public BuildResult(boolean success, String output, String error, String buildType, long durationMs, List<String> warnings) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.buildType = buildType;
        this.durationMs = durationMs;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    /**
     * 创建成功的构建结果
     *
     * @param output 构建标准输出内容
     * @param durationMs 构建耗时（毫秒）
     * @param warnings 警告信息列表，可为 null
     * @return 成功的 BuildResult 实例
     */
    public static BuildResult success(String output, long durationMs, List<String> warnings) {
        return new BuildResult(true, output, null, null, durationMs, warnings);
    }

    /**
     * 创建失败的构建结果
     *
     * @param output 构建标准输出内容
     * @param error 错误输出内容
     * @param durationMs 构建耗时（毫秒）
     * @return 失败的 BuildResult 实例
     */
    public static BuildResult failure(String output, String error, long durationMs) {
        return new BuildResult(false, output, error, null, durationMs, null);
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取构建是否成功
     *
     * @return 构建成功返回 true，失败返回 false
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置构建成功状态
     *
     * @param success 是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取构建标准输出内容
     *
     * @return 标准输出字符串
     */
    public String getOutput() {
        return output;
    }

    /**
     * 设置构建标准输出内容
     *
     * @param output 标准输出字符串
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * 获取构建错误输出内容
     *
     * @return 错误输出字符串
     */
    public String getError() {
        return error;
    }

    /**
     * 设置构建错误输出内容
     *
     * @param error 错误输出字符串
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * 获取构建类型标识
     *
     * @return 构建类型名称
     */
    public String getBuildType() {
        return buildType;
    }

    /**
     * 设置构建类型标识
     *
     * @param buildType 构建类型名称
     */
    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }

    /**
     * 获取构建耗时（毫秒）
     *
     * @return 耗时毫秒数
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * 设置构建耗时
     *
     * @param durationMs 耗时毫秒数
     */
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 获取构建过程中的警告信息列表
     *
     * @return 警告信息列表，不会返回 null
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * 设置警告信息列表
     *
     * @param warnings 警告信息列表
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "BuildResult{" +
                "success=" + success +
                ", buildType='" + buildType + '\'' +
                ", durationMs=" + durationMs +
                ", warnings=" + (warnings != null ? warnings.size() : 0) +
                '}';
    }
}
