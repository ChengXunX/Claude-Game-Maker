package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时错误检测器
 * 检测游戏运行时的各种错误
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>从进程输出（stdout/stderr）中识别错误模式</li>
 *   <li>检测 HTTP 资源加载失败</li>
 *   <li>判断是否存在严重错误（会导致进程崩溃）</li>
 *   <li>格式化错误信息为可读文本</li>
 * </ul>
 *
 * <p>支持的错误类型：</p>
 * <ul>
 *   <li>JavaScript 运行时错误（TypeError、ReferenceError 等）</li>
 *   <li>资源加载错误（404、连接拒绝等）</li>
 *   <li>端口冲突（EADDRINUSE）</li>
 *   <li>权限错误（EACCES）</li>
 *   <li>内存溢出（Heap Out of Memory）</li>
 *   <li>Node.js 模块未找到</li>
 *   <li>Python 错误（Traceback、ImportError 等）</li>
 * </ul>
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class RuntimeErrorDetector {

    private static final Logger log = LoggerFactory.getLogger(RuntimeErrorDetector.class);

    /**
     * 从进程输出中检测错误
     *
     * <p>逐行分析进程的 stdout/stderr 输出，根据关键词匹配识别各类错误。</p>
     *
     * @param processOutput 进程的 stdout/stderr 输出文本
     * @return 检测到的错误列表，如果没有错误则返回空列表
     */
    public List<RuntimeError> detectFromOutput(String processOutput) {
        List<RuntimeError> errors = new ArrayList<>();
        if (processOutput == null || processOutput.isEmpty()) return errors;

        String[] lines = processOutput.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lower = line.toLowerCase().trim();

            // 跳过空行
            if (lower.isEmpty()) continue;

            // JavaScript 运行时错误
            if (lower.contains("uncaught") || lower.contains("unhandled")
                || lower.contains("typeerror") || lower.contains("referenceerror")
                || lower.contains("syntaxerror") || lower.contains("rangeerror")
                || lower.contains("evalerror")) {
                errors.add(new RuntimeError(ErrorType.JS_RUNTIME, line.trim(), i, null));
            }
            // 资源加载错误
            else if (lower.contains("failed to load") || lower.contains("net::err_")
                || lower.contains("err_file_not_found") || lower.contains("404 (not found)")
                || lower.contains("err_connection_refused")) {
                errors.add(new RuntimeError(ErrorType.RESOURCE_LOAD, line.trim(), i, null));
            }
            // 端口占用
            else if (lower.contains("eaddrinuse") || lower.contains("address already in use")
                || lower.contains("listen eaddrinuse")) {
                errors.add(new RuntimeError(ErrorType.PORT_CONFLICT, line.trim(), i, null));
            }
            // 权限错误
            else if (lower.contains("eacces") || lower.contains("permission denied")
                || lower.contains("operation not permitted")) {
                errors.add(new RuntimeError(ErrorType.PERMISSION, line.trim(), i, null));
            }
            // 内存溢出
            else if (lower.contains("heap out of memory") || lower.contains("javascript heap")
                || lower.contains("fatal error: reached heap limit")) {
                errors.add(new RuntimeError(ErrorType.MEMORY, line.trim(), i, null));
            }
            // Node.js 模块未找到
            else if (lower.contains("cannot find module") || lower.contains("module_not_found")
                || lower.contains("err_module_not_found")) {
                errors.add(new RuntimeError(ErrorType.MODULE_NOT_FOUND, line.trim(), i, null));
            }
            // Python 错误
            else if (lower.contains("traceback") || lower.contains("syntaxerror:")
                || lower.contains("nameerror") || lower.contains("typeerror:")
                || lower.contains("importerror") || lower.contains("modulenotfounderror")) {
                errors.add(new RuntimeError(ErrorType.PYTHON_ERROR, line.trim(), i, null));
            }
        }

        return errors;
    }

    /**
     * 检测 HTTP 资源加载错误
     *
     * <p>发送 HTTP 请求到游戏页面，解析 HTML 中的资源引用（src/href），
     * 然后逐一检查这些资源是否可以正常访问。</p>
     *
     * <p>跳过的资源类型：</p>
     * <ul>
     *   <li>外部资源（以 http:// 或 // 开头）</li>
     *   <li>data URI（以 data: 开头）</li>
     *   <li>锚点（以 # 开头）</li>
     *   <li>JavaScript 代码（以 javascript: 开头）</li>
     *   <li>邮件链接（以 mailto: 开头）</li>
     * </ul>
     *
     * @param port 游戏服务端口
     * @return 资源加载错误列表，每个元素描述一个加载失败的资源
     */
    public List<String> detectResourceErrors(int port) {
        List<String> errors = new ArrayList<>();

        try {
            // 获取页面 HTML
            var url = new java.net.URL("http://localhost:" + port);
            var conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                errors.add("无法访问游戏页面: HTTP " + conn.getResponseCode());
                conn.disconnect();
                return errors;
            }

            String html = new String(conn.getInputStream().readAllBytes());
            conn.disconnect();

            // 提取资源引用（src 和 href 属性）
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:src|href)=[\"']([^\"']+)[\"']")
                .matcher(html);

            while (matcher.find()) {
                String resourcePath = matcher.group(1);
                // 跳过外部资源、锚点、data URI、JavaScript
                if (resourcePath.startsWith("http") || resourcePath.startsWith("//")
                    || resourcePath.startsWith("data:") || resourcePath.startsWith("#")
                    || resourcePath.startsWith("javascript:") || resourcePath.startsWith("mailto:")) {
                    continue;
                }

                // 检查本地资源
                try {
                    String resourceUrl = resourcePath.startsWith("/")
                        ? "http://localhost:" + port + resourcePath
                        : "http://localhost:" + port + "/" + resourcePath;
                    var resConn = (java.net.HttpURLConnection) new java.net.URL(resourceUrl).openConnection();
                    resConn.setConnectTimeout(2000);
                    resConn.setReadTimeout(2000);
                    int code = resConn.getResponseCode();
                    resConn.disconnect();

                    if (code != 200) {
                        errors.add("资源加载失败: " + resourcePath + " (HTTP " + code + ")");
                    }
                } catch (Exception e) {
                    errors.add("资源加载异常: " + resourcePath + " (" + e.getMessage() + ")");
                }
            }
        } catch (Exception e) {
            log.debug("资源检测异常: {}", e.getMessage());
        }

        return errors;
    }

    /**
     * 检查进程是否有严重错误（会导致进程崩溃的错误）
     *
     * <p>严重错误包括：</p>
     * <ul>
     *   <li>内存溢出（MEMORY）</li>
     *   <li>端口冲突（PORT_CONFLICT）</li>
     *   <li>权限错误（PERMISSION）</li>
     * </ul>
     *
     * @param errors 错误列表
     * @return 如果存在严重错误返回 true，否则返回 false
     */
    public boolean hasCriticalErrors(List<RuntimeError> errors) {
        if (errors == null || errors.isEmpty()) return false;
        return errors.stream().anyMatch(e ->
            e.getType() == ErrorType.MEMORY
            || e.getType() == ErrorType.PORT_CONFLICT
            || e.getType() == ErrorType.PERMISSION);
    }

    /**
     * 格式化错误列表为可读文本
     *
     * <p>输出格式示例：</p>
     * <pre>
     * 检测到 2 个错误:
     *
     * 1. [JavaScript运行时错误] Uncaught TypeError: Cannot read property 'x' of undefined (行 42)
     * 2. [资源加载失败] Failed to load resource: /images/logo.png (行 15)
     * </pre>
     *
     * @param errors 错误列表
     * @return 格式化的错误文本，如果没有错误则返回 "无错误"
     */
    public String formatErrors(List<RuntimeError> errors) {
        if (errors == null || errors.isEmpty()) return "无错误";

        StringBuilder sb = new StringBuilder();
        sb.append("检测到 ").append(errors.size()).append(" 个错误:\n\n");
        for (int i = 0; i < errors.size(); i++) {
            RuntimeError e = errors.get(i);
            sb.append(String.format("%d. [%s] %s (行 %d)\n",
                i + 1, e.getType().getDescription(), e.getMessage(), e.getLineNumber()));
        }
        return sb.toString();
    }

    /**
     * 运行时错误
     *
     * <p>封装检测到的运行时错误信息，包括错误类型、错误消息、行号和文件路径。</p>
     */
    public static class RuntimeError {
        /** 错误类型 */
        private final ErrorType type;
        /** 错误消息内容 */
        private final String message;
        /** 错误所在行号 */
        private final int lineNumber;
        /** 错误所在文件路径（可为 null） */
        private final String filePath;

        /**
         * 构造运行时错误
         *
         * @param type 错误类型
         * @param message 错误消息
         * @param lineNumber 错误所在行号
         * @param filePath 错误所在文件路径（可为 null）
         */
        public RuntimeError(ErrorType type, String message, int lineNumber, String filePath) {
            this.type = type;
            this.message = message;
            this.lineNumber = lineNumber;
            this.filePath = filePath;
        }

        /** 获取错误类型 */
        public ErrorType getType() { return type; }
        /** 获取错误消息 */
        public String getMessage() { return message; }
        /** 获取错误所在行号 */
        public int getLineNumber() { return lineNumber; }
        /** 获取错误所在文件路径 */
        public String getFilePath() { return filePath; }
    }

    /**
     * 错误类型枚举
     *
     * <p>定义了所有支持的运行时错误类型及其描述。</p>
     */
    public enum ErrorType {
        /** JavaScript 运行时错误（TypeError、ReferenceError 等） */
        JS_RUNTIME("JavaScript运行时错误"),
        /** 资源加载失败（404、连接拒绝等） */
        RESOURCE_LOAD("资源加载失败"),
        /** 端口冲突（EADDRINUSE） */
        PORT_CONFLICT("端口冲突"),
        /** 权限错误（EACCES） */
        PERMISSION("权限错误"),
        /** 内存溢出（Heap Out of Memory） */
        MEMORY("内存溢出"),
        /** 语法错误 */
        SYNTAX("语法错误"),
        /** 网络错误 */
        NETWORK("网络错误"),
        /** Node.js 模块未找到 */
        MODULE_NOT_FOUND("模块未找到"),
        /** Python 错误（Traceback、ImportError 等） */
        PYTHON_ERROR("Python错误"),
        /** 未知错误 */
        UNKNOWN("未知错误");

        /** 错误类型的中文描述 */
        private final String description;

        /**
         * 构造错误类型
         *
         * @param description 错误类型的中文描述
         */
        ErrorType(String description) {
            this.description = description;
        }

        /** 获取错误类型的中文描述 */
        public String getDescription() { return description; }
    }
}
