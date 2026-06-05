package com.chengxun.gamemaker.web.websocket;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终端 WebSocket 处理器
 * 提供 Web 终端功能，支持实时命令执行和输出
 *
 * 安全限制：
 * - 需要管理员权限
 * - 限制危险命令（rm -rf / 等）
 * - 会话超时自动断开
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TerminalWebSocketHandler.class);

    /** 活跃的终端会话 */
    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();

    /** 会话超时时间（毫秒） */
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30分钟

    /** 危险命令列表 */
    private static final String[] DANGEROUS_COMMANDS = {
        "rm -rf /", "rm -rf /*", "mkfs", "dd if=/dev/zero",
        ":(){ :|:& };:", "chmod -R 777 /", "shutdown", "reboot", "halt"
    };

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public TerminalWebSocketHandler(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URL 参数获取 token
        String token = extractToken(session);
        if (token == null || !jwtUtils.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("认证失败"));
            return;
        }

        // 验证管理员权限
        String username = jwtUtils.getUsernameFromToken(token);
        User user = userService.getUserByUsername(username);
        if (user == null || !"ADMIN".equals(user.getRole().getName())) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("需要管理员权限"));
            return;
        }

        // 创建终端会话
        TerminalSession terminalSession = new TerminalSession(session, username);
        sessions.put(session.getId(), terminalSession);

        // 发送欢迎消息
        sendMessage(session, "\033[1;32m=== ChengXun Game Maker 终端 ===\033[0m\r\n");
        sendMessage(session, "用户: " + username + "\r\n");
        sendMessage(session, "输入 'help' 查看可用命令\r\n\r\n");
        sendMessage(session, getPrompt());

        log.info("终端会话建立: user={}, sessionId={}", username, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TerminalSession terminalSession = sessions.get(session.getId());
        if (terminalSession == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("会话不存在"));
            return;
        }

        String command = message.getPayload().trim();
        if (command.isEmpty()) {
            sendMessage(session, getPrompt());
            return;
        }

        // 记录命令审计日志
        log.info("终端命令: user={}, command={}", terminalSession.getUsername(), command);

        // 检查危险命令
        if (isDangerousCommand(command)) {
            log.warn("终端危险命令被拦截: user={}, command={}", terminalSession.getUsername(), command);
            sendMessage(session, "\033[1;31m错误: 此命令被安全策略禁止\033[0m\r\n");
            sendMessage(session, getPrompt());
            return;
        }

        // 处理内置命令
        if (handleBuiltinCommand(session, command)) {
            return;
        }

        // 执行系统命令
        executeCommand(session, terminalSession, command);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        TerminalSession terminalSession = sessions.remove(session.getId());
        if (terminalSession != null) {
            terminalSession.destroy();
            log.info("终端会话关闭: user={}, sessionId={}", terminalSession.getUsername(), session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("终端传输错误: sessionId={}", session.getId(), exception);
        sessions.remove(session.getId());
    }

    /**
     * 执行系统命令
     */
    private void executeCommand(WebSocketSession session, TerminalSession terminalSession, String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", command);
            pb.redirectErrorStream(true);

            // 设置工作目录
            String workDir = terminalSession.getWorkDir();
            if (workDir != null && !workDir.isEmpty()) {
                pb.directory(new File(workDir));
            }

            Process process = pb.start();
            terminalSession.setCurrentProcess(process);

            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (session.isOpen()) {
                        sendMessage(session, line + "\r\n");
                    }
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            if (session.isOpen()) {
                sendMessage(session, "\r\n\033[1;33m退出码: " + exitCode + "\033[0m\r\n");
                sendMessage(session, getPrompt());
            }

        } catch (Exception e) {
            if (session.isOpen()) {
                sendMessage(session, "\033[1;31m执行错误: " + e.getMessage() + "\033[0m\r\n");
                sendMessage(session, getPrompt());
            }
        }
    }

    /**
     * 处理内置命令
     */
    private boolean handleBuiltinCommand(WebSocketSession session, String command) {
        switch (command.toLowerCase()) {
            case "help":
                showHelp(session);
                return true;
            case "clear":
                sendMessage(session, "\033[2J\033[H");
                sendMessage(session, getPrompt());
                return true;
            case "status":
                showStatus(session);
                return true;
            case "exit":
            case "quit":
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.error("关闭会话失败", e);
                }
                return true;
            default:
                if (command.startsWith("cd ")) {
                    changeDirectory(session, command.substring(3).trim());
                    return true;
                }
                return false;
        }
    }

    /**
     * 切换目录
     */
    private void changeDirectory(WebSocketSession session, String path) {
        TerminalSession terminalSession = sessions.get(session.getId());
        if (terminalSession == null) return;

        File dir;
        if (path.equals("~")) {
            dir = new File(System.getProperty("user.home"));
        } else if (path.startsWith("/")) {
            dir = new File(path);
        } else {
            dir = new File(terminalSession.getWorkDir(), path);
        }

        if (dir.exists() && dir.isDirectory()) {
            terminalSession.setWorkDir(dir.getAbsolutePath());
            sendMessage(session, "切换到: " + dir.getAbsolutePath() + "\r\n");
        } else {
            sendMessage(session, "\033[1;31m目录不存在: " + path + "\033[0m\r\n");
        }
        sendMessage(session, getPrompt());
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(WebSocketSession session) {
        StringBuilder help = new StringBuilder();
        help.append("\033[1;36m=== 可用命令 ===\033[0m\r\n");
        help.append("  help        - 显示此帮助\r\n");
        help.append("  clear       - 清屏\r\n");
        help.append("  status      - 显示系统状态\r\n");
        help.append("  cd <dir>    - 切换目录\r\n");
        help.append("  exit/quit   - 退出终端\r\n");
        help.append("\r\n\033[1;33m支持所有标准 bash 命令\033[0m\r\n");
        sendMessage(session, help.toString());
        sendMessage(session, getPrompt());
    }

    /**
     * 显示系统状态
     */
    private void showStatus(WebSocketSession session) {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder status = new StringBuilder();
        status.append("\033[1;36m=== 系统状态 ===\033[0m\r\n");
        status.append("  Java版本: ").append(System.getProperty("java.version")).append("\r\n");
        status.append("  操作系统: ").append(System.getProperty("os.name")).append("\r\n");
        status.append("  CPU核心: ").append(runtime.availableProcessors()).append("\r\n");
        status.append("  最大内存: ").append(runtime.maxMemory() / 1024 / 1024).append("MB\r\n");
        status.append("  已用内存: ").append((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024).append("MB\r\n");
        status.append("  可用内存: ").append(runtime.freeMemory() / 1024 / 1024).append("MB\r\n");
        sendMessage(session, status.toString());
        sendMessage(session, getPrompt());
    }

    /**
     * 检查是否为危险命令
     */
    private boolean isDangerousCommand(String command) {
        String lowerCmd = command.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCmd.contains(dangerous.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取命令提示符
     */
    private String getPrompt() {
        return "\033[1;32m$ \033[0m";
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 从 WebSocket 会话提取 token
     */
    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    /**
     * 终端会话内部类
     */
    private static class TerminalSession {
        private final WebSocketSession session;
        private final String username;
        private String workDir;
        private Process currentProcess;

        public TerminalSession(WebSocketSession session, String username) {
            this.session = session;
            this.username = username;
            this.workDir = System.getProperty("user.home");
        }

        public void setCurrentProcess(Process process) {
            this.currentProcess = process;
        }

        public void setWorkDir(String workDir) {
            this.workDir = workDir;
        }

        public String getWorkDir() {
            return workDir;
        }

        public String getUsername() {
            return username;
        }

        public void destroy() {
            if (currentProcess != null && currentProcess.isAlive()) {
                currentProcess.destroyForcibly();
            }
        }
    }
}
