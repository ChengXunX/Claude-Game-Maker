package com.chengxun.gamemaker.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Git命令工具类
 * 提供Git命令执行和仓库操作的通用方法
 *
 * 主要功能：
 * - 执行Git命令
 * - 检查Git仓库状态
 * - 获取Git仓库信息
 *
 * 使用示例：
 * ```java
 * // 执行Git命令
 * String status = GitCommandUtil.runCommand(workDir, "status", "--short");
 *
 * // 检查是否为Git仓库
 * boolean isGit = GitCommandUtil.isGitRepo(workDir);
 *
 * // 获取当前分支
 * String branch = GitCommandUtil.getCurrentBranch(workDir);
 * ```
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class GitCommandUtil {

    private static final Logger log = LoggerFactory.getLogger(GitCommandUtil.class);

    private GitCommandUtil() {
        // 工具类不允许实例化
    }

    /**
     * 执行Git命令
     *
     * @param workDir 工作目录
     * @param args 命令参数
     * @return 命令输出，如果出错返回 "Error: " + 错误信息
     */
    public static String runCommand(String workDir, String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.addAll(Arrays.asList(args));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(workDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("Git command failed with exit code {}: git {}", exitCode, String.join(" ", args));
            }

            return output.trim();
        } catch (IOException e) {
            log.error("Failed to execute git command: {}", e.getMessage());
            return "Error: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Git command interrupted: {}", e.getMessage());
            return "Error: Command interrupted";
        }
    }

    /**
     * 检查目录是否为Git仓库
     *
     * @param dir 目录路径
     * @return 是否为Git仓库
     */
    public static boolean isGitRepo(String dir) {
        return Files.exists(Path.of(dir, ".git"));
    }

    /**
     * 获取当前分支名称
     *
     * @param workDir 工作目录
     * @return 当前分支名称，如果出错返回空字符串
     */
    public static String getCurrentBranch(String workDir) {
        String result = runCommand(workDir, "branch", "--show-current");
        return result.startsWith("Error") ? "" : result.trim();
    }

    /**
     * 获取Git状态
     *
     * @param workDir 工作目录
     * @return git status --short 的输出
     */
    public static String getStatus(String workDir) {
        return runCommand(workDir, "status", "--short");
    }

    /**
     * 获取最近的提交记录
     *
     * @param workDir 工作目录
     * @param count 获取的提交数量
     * @return git log --oneline -n 的输出
     */
    public static String getRecentCommits(String workDir, int count) {
        return runCommand(workDir, "log", "--oneline", "-" + count);
    }

    /**
     * 获取远程仓库信息
     *
     * @param workDir 工作目录
     * @return git remote -v 的输出
     */
    public static String getRemoteInfo(String workDir) {
        return runCommand(workDir, "remote", "-v");
    }

    /**
     * 获取远程仓库URL
     *
     * @param workDir 工作目录
     * @return 远程仓库URL，如果不存在返回空字符串
     */
    public static String getRemoteUrl(String workDir) {
        String result = runCommand(workDir, "remote", "get-url", "origin");
        return result.startsWith("Error") ? "" : result;
    }

    /**
     * 获取分支列表
     *
     * @param workDir 工作目录
     * @return git branch -a 的输出
     */
    public static String getBranches(String workDir) {
        return runCommand(workDir, "branch", "-a");
    }

    /**
     * 获取工作区差异
     *
     * @param workDir 工作目录
     * @return git diff 的输出
     */
    public static String getDiff(String workDir) {
        return runCommand(workDir, "diff");
    }

    /**
     * 获取暂存区差异
     *
     * @param workDir 工作目录
     * @return git diff --staged 的输出
     */
    public static String getStagedDiff(String workDir) {
        return runCommand(workDir, "diff", "--staged");
    }

    /**
     * 获取Stash列表
     *
     * @param workDir 工作目录
     * @return git stash list 的输出
     */
    public static String getStashList(String workDir) {
        return runCommand(workDir, "stash", "list");
    }

    /**
     * 初始化Git仓库
     *
     * @param workDir 工作目录
     * @return 执行结果
     */
    public static String initRepo(String workDir) {
        return runCommand(workDir, "init");
    }

    /**
     * 添加文件到暂存区
     *
     * @param workDir 工作目录
     * @param files 文件路径，"." 表示所有文件
     * @return 执行结果
     */
    public static String addFiles(String workDir, String files) {
        return runCommand(workDir, "add", files);
    }

    /**
     * 提交更改
     *
     * @param workDir 工作目录
     * @param message 提交信息
     * @return 执行结果
     */
    public static String commit(String workDir, String message) {
        return runCommand(workDir, "commit", "-m", message);
    }

    /**
     * 推送到远程仓库
     *
     * @param workDir 工作目录
     * @param force 是否强制推送
     * @return 执行结果
     */
    public static String push(String workDir, boolean force) {
        String branch = getCurrentBranch(workDir);
        if (branch.isEmpty()) {
            return "Error: Cannot determine current branch";
        }

        if (force) {
            return runCommand(workDir, "push", "-u", "origin", branch, "--force");
        } else {
            return runCommand(workDir, "push", "-u", "origin", branch);
        }
    }

    /**
     * 从远程仓库拉取
     *
     * @param workDir 工作目录
     * @return 执行结果
     */
    public static String pull(String workDir) {
        return runCommand(workDir, "pull");
    }

    /**
     * 创建分支
     *
     * @param workDir 工作目录
     * @param branchName 分支名称
     * @return 执行结果
     */
    public static String createBranch(String workDir, String branchName) {
        return runCommand(workDir, "branch", branchName);
    }

    /**
     * 切换分支
     *
     * @param workDir 工作目录
     * @param branchName 分支名称
     * @return 执行结果
     */
    public static String checkoutBranch(String workDir, String branchName) {
        return runCommand(workDir, "checkout", branchName);
    }

    /**
     * 设置远程仓库URL
     *
     * @param workDir 工作目录
     * @param remoteUrl 远程仓库URL
     * @return 执行结果
     */
    public static String setRemoteUrl(String workDir, String remoteUrl) {
        String existing = getRemoteUrl(workDir);
        if (existing.isEmpty()) {
            return runCommand(workDir, "remote", "add", "origin", remoteUrl);
        } else {
            return runCommand(workDir, "remote", "set-url", "origin", remoteUrl);
        }
    }

    /**
     * 保存Stash
     *
     * @param workDir 工作目录
     * @param message Stash消息
     * @return 执行结果
     */
    public static String stashSave(String workDir, String message) {
        return runCommand(workDir, "stash", "save", message != null ? message : "");
    }

    /**
     * 弹出Stash
     *
     * @param workDir 工作目录
     * @return 执行结果
     */
    public static String stashPop(String workDir) {
        return runCommand(workDir, "stash", "pop");
    }

    /**
     * 删除Stash
     *
     * @param workDir 工作目录
     * @return 执行结果
     */
    public static String stashDrop(String workDir) {
        return runCommand(workDir, "stash", "drop");
    }
}
