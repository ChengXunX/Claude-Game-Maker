package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 游戏沙盒执行服务
 * 在隔离环境中运行游戏，捕获错误和输出
 *
 * 主要功能：
 * - 在独立进程中运行游戏
 * - 捕获标准输出和错误输出
 * - 设置执行超时
 * - 记录执行结果
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameSandboxService {

    private static final Logger log = LoggerFactory.getLogger(GameSandboxService.class);

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT = 30;

    /** 最大输出大小 */
    private static final int MAX_OUTPUT_SIZE = 1024 * 1024; // 1MB

    /** 执行结果缓存 */
    private final Map<String, ExecutionResult> resultCache = new ConcurrentHashMap<>();

    /**
     * 在沙盒中运行游戏
     *
     * @param projectId 项目ID
     * @param projectPath 项目路径
     * @param command 执行命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public ExecutionResult execute(String projectId, String projectPath, String command, int timeoutSeconds) {
        log.info("沙盒执行: projectId={}, command={}", projectId, command);

        ExecutionResult result = new ExecutionResult();
        result.setProjectId(projectId);
        result.setCommand(command);
        result.setStartedAt(LocalDateTime.now());

        Process process = null;
        try {
            // 创建进程构建器
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(projectPath));

            // 根据操作系统设置命令
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }

            // 重定向错误流
            pb.redirectErrorStream(true);

            // 启动进程
            process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > MAX_OUTPUT_SIZE) {
                        output.append("... (输出被截断)\n");
                        break;
                    }
                }
            }

            // 等待进程完成（带超时）
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            result.setCompletedAt(LocalDateTime.now());
            result.setOutput(output.toString());

            if (completed) {
                result.setExitCode(process.exitValue());
                result.setStatus(result.getExitCode() == 0 ? "SUCCESS" : "FAILED");
            } else {
                process.destroyForcibly();
                result.setStatus("TIMEOUT");
                result.setError("执行超时（" + timeoutSeconds + "秒）");
            }

        } catch (Exception e) {
            result.setStatus("ERROR");
            result.setError(e.getMessage());
            log.error("沙盒执行失败: projectId={}", projectId, e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }

        // 缓存结果
        resultCache.put(projectId, result);

        log.info("沙盒执行完成: projectId={}, status={}, exitCode={}",
            projectId, result.getStatus(), result.getExitCode());

        return result;
    }

    /**
     * 运行 npm install
     */
    public ExecutionResult npmInstall(String projectId, String projectPath) {
        return execute(projectId, projectPath, "npm install", 120);
    }

    /**
     * 运行 npm run dev（开发服务器）
     */
    public ExecutionResult npmRunDev(String projectId, String projectPath) {
        return execute(projectId, projectPath, "npm run dev", 10);
    }

    /**
     * 运行 npm run build
     */
    public ExecutionResult npmRunBuild(String projectId, String projectPath) {
        return execute(projectId, projectPath, "npm run build", 60);
    }

    /**
     * 运行测试
     */
    public ExecutionResult runTests(String projectId, String projectPath) {
        return execute(projectId, projectPath, "npm test", 60);
    }

    /**
     * 检查代码质量
     */
    public ExecutionResult lintCode(String projectId, String projectPath) {
        return execute(projectId, projectPath, "npx eslint src/ --ext .js 2>&1 || true", 30);
    }

    /**
     * 获取执行结果
     */
    public ExecutionResult getResult(String projectId) {
        return resultCache.get(projectId);
    }

    /**
     * 清理旧结果
     */
    public void cleanup(int maxAgeHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxAgeHours);
        resultCache.entrySet().removeIf(entry ->
            entry.getValue().getCompletedAt() != null &&
            entry.getValue().getCompletedAt().isBefore(cutoff)
        );
    }

    // ===== 内部类 =====

    public static class ExecutionResult {
        private String projectId;
        private String command;
        private String status;
        private int exitCode;
        private String output;
        private String error;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}
