package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.service.ClaudeAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 游戏截图与视觉分析服务
 *
 * <p>核心职责：将"游戏真实运行"从文件级验证升级为"视觉级验证"。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>截图：调用本地 Chrome Headless 截取游戏画面，支持多帧（动画捕捉）</li>
 *   <li>端口池：管理 18200-18299 端口范围，避免多项目并发截图时端口冲突</li>
 *   <li>AI 视觉分析：将截图发送给 Claude vision 进行多维度评估（可玩性、UI/UX、视觉缺陷）</li>
 *   <li>截图归档：所有截图统一保存到 {@code data/game-screenshots/{projectId}/} 目录</li>
 * </ul>
 *
 * <p>为什么不用 Puppeteer MCP：</p>
 * <ul>
 *   <li>Chrome Headless CLI 直接调用：零依赖、零网络开销、稳定可靠</li>
 *   <li>避免 MCP Server 进程管理的额外复杂度</li>
 *   <li>机器已预装 google-chrome-stable（部署机环境约束）</li>
 * </ul>
 *
 * <p>线程安全：</p>
 * <ul>
 *   <li>端口分配使用 ConcurrentHashMap，保证并发安全</li>
 *   <li>每次截图独立启动/销毁 Chrome 进程，无共享状态</li>
 * </ul>
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class GameScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(GameScreenshotService.class);

    /** 截图存储根目录（相对数据目录） */
    private static final String SCREENSHOT_BASE_DIR = "data/game-screenshots";

    /** 端口池范围：18200-18299 */
    private static final int PORT_POOL_START = 18200;
    private static final int PORT_POOL_END = 18300;

    /** 单次截图超时（秒） */
    private static final int SCREENSHOT_TIMEOUT_SECONDS = 30;

    /** Chrome 启动等待时间（毫秒），给页面渲染留时间 */
    private static final int DEFAULT_RENDER_WAIT_MS = 2000;

    /** 多帧截图默认间隔（毫秒） */
    private static final int DEFAULT_FRAME_INTERVAL_MS = 1500;

    /** 截图文件名前缀 */
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Value("${game.screenshot.chrome-path:/usr/bin/google-chrome}")
    private String chromePath;

    @Value("${game.screenshot.base-dir:}")
    private String configuredBaseDir;

    @Autowired(required = false)
    private ClaudeAiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已分配的端口集合（线程安全） */
    private final ConcurrentHashMap<Integer, Boolean> allocatedPorts = new ConcurrentHashMap<>();

    /** 截图基础目录（绝对路径） */
    private String baseDirAbsolute;

    /**
     * 初始化：检查 Chrome 可用性、创建截图目录
     */
    @PostConstruct
    public void init() {
        // 解析基础目录：优先用配置，否则用当前工作目录
        if (configuredBaseDir != null && !configuredBaseDir.isBlank()) {
            baseDirAbsolute = configuredBaseDir;
        } else {
            String userDir = System.getProperty("user.dir", ".");
            baseDirAbsolute = Paths.get(userDir, SCREENSHOT_BASE_DIR).toString();
        }

        File baseDir = new File(baseDirAbsolute);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            log.warn("创建截图目录失败: {}，将使用临时目录", baseDirAbsolute);
            baseDirAbsolute = System.getProperty("java.io.tmpdir") + "/game-screenshots";
            new File(baseDirAbsolute).mkdirs();
        }

        // 检查 Chrome
        File chrome = new File(chromePath);
        if (!chrome.exists()) {
            log.warn("Chrome 未在 {} 找到，截图功能将不可用。请安装 google-chrome-stable。", chromePath);
        } else {
            log.info("GameScreenshotService 初始化完成：Chrome={}, 截图目录={}", chromePath, baseDirAbsolute);
        }
    }

    // ============== 端口管理 ==============

    /**
     * 从端口池分配一个可用端口
     *
     * @return 分配的端口号
     * @throws IllegalStateException 端口池耗尽时抛出
     */
    private int allocatePort() {
        for (int port = PORT_POOL_START; port < PORT_POOL_END; port++) {
            // computeIfAbsent 原子操作：端口未被占用则标记为已用
            Boolean previous = allocatedPorts.computeIfAbsent(port, p -> {
                if (isPortAvailable(p)) {
                    return Boolean.TRUE;
                }
                return null; // 端口被外部占用，不加入池
            });
            if (previous != null) {
                return port;
            }
        }
        throw new IllegalStateException("截图端口池已耗尽（" + PORT_POOL_START + "-" + PORT_POOL_END + "）");
    }

    /**
     * 释放端口到端口池
     *
     * @param port 要释放的端口
     */
    private void releasePort(int port) {
        allocatedPorts.remove(port);
    }

    /**
     * 检查端口是否可用（未被系统进程占用）
     */
    private boolean isPortAvailable(int port) {
        try (var socket = new java.net.ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============== 截图核心 ==============

    /**
     * 对本地 HTTP 服务进行截图（单帧）
     *
     * @param port 游戏运行的端口
     * @param projectId 项目ID（用于归档）
     * @return 截图文件路径
     * @throws IOException 截图失败时抛出
     */
    public String screenshotUrl(int port, String projectId) throws IOException {
        return screenshotUrl(port, projectId, null, DEFAULT_RENDER_WAIT_MS);
    }

    /**
     * 对本地 HTTP 服务进行截图（带路径）
     *
     * @param port 游戏运行的端口
     * @param projectId 项目ID
     * @param path URL 路径（如 "/game.html"），可为 null
     * @param renderWaitMs 渲染等待时间（毫秒）
     * @return 截图文件路径
     * @throws IOException 截图失败时抛出
     */
    public String screenshotUrl(int port, String projectId, String path, int renderWaitMs) throws IOException {
        String url = buildUrl("http", "localhost", port, path);
        return captureScreenshot(url, projectId, renderWaitMs);
    }

    /**
     * 对本地 file:// 协议的 HTML 文件进行截图
     *
     * @param htmlFile 绝对路径的 HTML 文件
     * @param projectId 项目ID
     * @return 截图文件路径
     * @throws IOException 截图失败时抛出
     */
    public String screenshotFile(String htmlFile, String projectId) throws IOException {
        File f = new File(htmlFile);
        if (!f.exists()) {
            throw new IOException("HTML 文件不存在: " + htmlFile);
        }
        return captureScreenshot(f.toURI().toString(), projectId, DEFAULT_RENDER_WAIT_MS);
    }

    /**
     * 多帧截图：连续抓取 N 帧，捕捉游戏动画
     *
     * @param port 游戏运行的端口
     * @param projectId 项目ID
     * @param frameCount 帧数（1-10）
     * @param intervalMs 帧间隔（毫秒）
     * @return 截图文件路径列表
     * @throws IOException 截图失败时抛出
     */
    public List<String> screenshotMultiFrame(int port, String projectId, int frameCount, int intervalMs) throws IOException {
        if (frameCount < 1) frameCount = 1;
        if (frameCount > 10) frameCount = 10;
        if (intervalMs < 500) intervalMs = DEFAULT_FRAME_INTERVAL_MS;

        List<String> paths = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            try {
                String path = screenshotUrl(port, projectId, null, DEFAULT_RENDER_WAIT_MS);
                paths.add(path);
                if (i < frameCount - 1) {
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("多帧截图被中断", e);
            }
        }
        log.info("多帧截图完成: projectId={}, frames={}", projectId, paths.size());
        return paths;
    }

    /**
     * 内部方法：调用 Chrome Headless 截取单个画面
     *
     * @param url 目标 URL（http://... 或 file://...）
     * @param projectId 项目ID
     * @param renderWaitMs 渲染等待时间（毫秒）
     * @return 截图文件路径
     * @throws IOException 截图失败时抛出
     */
    private String captureScreenshot(String url, String projectId, int renderWaitMs) throws IOException {
        // 1. 准备输出目录
        Path projectDir = Paths.get(baseDirAbsolute, sanitize(projectId));
        Files.createDirectories(projectDir);

        // 2. 生成唯一文件名
        String filename = "shot_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".png";
        Path outputPath = projectDir.resolve(filename);

        // 3. 分配独立端口给 Chrome 调试协议（避免多实例冲突）
        int chromeDebugPort = allocatePort();
        try {
            // 4. 构建 Chrome 命令
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            command.add("--headless=new");
            command.add("--disable-gpu");
            command.add("--no-sandbox");
            command.add("--disable-dev-shm-usage");
            command.add("--disable-software-rasterizer");
            command.add("--hide-scrollbars");
            command.add("--mute-audio");
            command.add("--window-size=1280,800");
            command.add("--remote-debugging-port=" + chromeDebugPort);
            command.add("--user-data-dir=" + Paths.get(System.getProperty("java.io.tmpdir"),
                "chrome-screenshot-" + System.currentTimeMillis()).toString());
            command.add("--screenshot=" + outputPath.toString());
            command.add("--virtual-time-budget=" + renderWaitMs);
            command.add(url);

            log.debug("启动 Chrome 截图: url={}, output={}", url, outputPath);

            // 5. 启动 Chrome 进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 6. 读取输出（防止缓冲区满导致阻塞）
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (Exception e) {
                log.debug("读取 Chrome 输出异常: {}", e.getMessage());
            }

            // 7. 等待完成（含超时）
            boolean finished = process.waitFor(SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Chrome 截图超时（" + SCREENSHOT_TIMEOUT_SECONDS + "秒）: " + url);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("Chrome 退出码非零: exitCode={}, output={}", exitCode,
                    output.length() > 500 ? output.substring(0, 500) + "..." : output.toString());
            }

            // 8. 验证截图文件
            File outputFile = outputPath.toFile();
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Chrome 未生成截图文件: " + outputPath);
            }

            log.info("截图成功: projectId={}, path={}, size={}KB", projectId, outputPath,
                outputFile.length() / 1024);
            return outputPath.toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("截图被中断", e);
        } finally {
            // 9. 释放端口
            releasePort(chromeDebugPort);
        }
    }

    // ============== AI 视觉分析 ==============

    /**
     * 视觉分析结果
     */
    public static class VisualAnalysisResult {
        /** 是否成功 */
        public boolean success;
        /** 错误信息 */
        public String error;
        /** 视觉评分 0-100 */
        public int visualScore;
        /** 可玩性视觉评分 0-100 */
        public int playableScore;
        /** UI/UX 视觉评分 0-100 */
        public int uiuxScore;
        /** 渲染正常性 0-100（白屏/崩溃会很低） */
        public int renderHealthScore;
        /** 视觉问题列表 */
        public List<String> issues = new ArrayList<>();
        /** 优点列表 */
        public List<String> strengths = new ArrayList<>();
        /** 摘要 */
        public String summary;
        /** 原始 AI 响应 */
        public String rawResponse;

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public int getVisualScore() { return visualScore; }
        public int getPlayableScore() { return playableScore; }
        public int getUiuxScore() { return uiuxScore; }
        public int getRenderHealthScore() { return renderHealthScore; }
        public List<String> getIssues() { return issues; }
        public List<String> getStrengths() { return strengths; }
        public String getSummary() { return summary; }
        public String getRawResponse() { return rawResponse; }
    }

    /**
     * 对截图列表进行 AI 视觉分析
     *
     * <p>使用 Claude vision 多模态能力，从游戏画面中评估：</p>
     * <ul>
     *   <li>渲染健康度：是否白屏/黑屏/崩溃</li>
     *   <li>可玩性：是否有可见的玩法元素（角色、敌人、UI 控件）</li>
     *   <li>UI/UX：界面布局、配色、交互元素是否合理</li>
     * </ul>
     *
     * @param screenshotPaths 截图文件路径列表
     * @param projectName 项目名称
     * @param projectGoal 项目目标（可选）
     * @return 视觉分析结果
     */
    public VisualAnalysisResult analyzeScreenshots(List<String> screenshotPaths, String projectName, String projectGoal) {
        if (screenshotPaths == null || screenshotPaths.isEmpty()) {
            VisualAnalysisResult r = new VisualAnalysisResult();
            r.success = false;
            r.error = "无截图可分析";
            return r;
        }

        if (aiService == null) {
            VisualAnalysisResult r = new VisualAnalysisResult();
            r.success = false;
            r.error = "AI 服务未注入，无法进行视觉分析";
            return r;
        }

        // 1. 基础健康检查：文件大小/格式
        VisualAnalysisResult healthCheck = checkScreenshotHealth(screenshotPaths);
        if (!healthCheck.success) {
            return healthCheck;
        }

        // 2. 调用 AI 视觉分析
        try {
            String prompt = buildVisionPrompt(projectName, projectGoal, screenshotPaths.size());
            String aiResponse = aiService.sendMessageWithImages(prompt, screenshotPaths);
            return parseVisualAnalysisResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI 视觉分析失败: {}", e.getMessage(), e);
            VisualAnalysisResult r = new VisualAnalysisResult();
            r.success = false;
            r.error = "AI 视觉分析失败: " + e.getMessage();
            return r;
        }
    }

    /**
     * 截图健康检查：文件存在、大小合理、格式正确
     */
    private VisualAnalysisResult checkScreenshotHealth(List<String> paths) {
        VisualAnalysisResult result = new VisualAnalysisResult();
        result.success = true;

        long totalSize = 0;
        int validCount = 0;
        for (String path : paths) {
            File f = new File(path);
            if (!f.exists() || f.length() == 0) {
                result.issues.add("截图文件无效: " + path);
                continue;
            }
            if (f.length() < 1024) {
                // 小于 1KB 可能是空白截图
                result.issues.add("截图过小（" + f.length() + " 字节）: " + path);
            }
            totalSize += f.length();
            validCount++;
        }

        if (validCount == 0) {
            result.success = false;
            result.error = "所有截图都无效";
            return result;
        }

        result.renderHealthScore = validCount == paths.size() ? 100 : (validCount * 100 / paths.size());
        if (result.renderHealthScore < 100) {
            result.issues.add(validCount + "/" + paths.size() + " 张截图有效");
        }
        return result;
    }

    /**
     * 构建视觉分析 Prompt
     */
    private String buildVisionPrompt(String projectName, String projectGoal, int frameCount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的游戏视觉质量评估专家。请仔细查看以下 ").append(frameCount).append(" 张游戏运行截图，");
        prompt.append("从真实游戏画面的角度评估游戏质量。\n\n");

        if (projectName != null && !projectName.isEmpty()) {
            prompt.append("项目名称: ").append(projectName).append("\n");
        }
        if (projectGoal != null && !projectGoal.isEmpty()) {
            prompt.append("项目目标: ").append(projectGoal).append("\n");
        }

        prompt.append("\n## 评估维度\n");
        prompt.append("1. **renderHealth (渲染健康度)**: 截图是否正常渲染（是否白屏/黑屏/错误页面/明显错位）\n");
        prompt.append("2. **playable (可玩性视觉证据)**: 从画面能否看出游戏玩法（角色、敌人、UI 控件、得分等）\n");
        prompt.append("3. **uiux (UI/UX 视觉质量)**: 界面布局、配色、文字可读性、交互元素是否合理\n");
        prompt.append("4. **visual (整体视觉质量)**: 美术风格、视觉效果、动画流畅度\n\n");

        prompt.append("## 输出要求\n");
        prompt.append("请严格按以下 JSON 格式输出（不要有其他内容）：\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"renderHealth\": { \"score\": 90, \"comment\": \"...\" },\n");
        prompt.append("  \"playable\": { \"score\": 70, \"comment\": \"...\" },\n");
        prompt.append("  \"uiux\": { \"score\": 80, \"comment\": \"...\" },\n");
        prompt.append("  \"visual\": { \"score\": 75, \"comment\": \"...\" },\n");
        prompt.append("  \"overallScore\": 78,\n");
        prompt.append("  \"summary\": \"一句话总评\",\n");
        prompt.append("  \"strengths\": [\"优点1\", \"优点2\"],\n");
        prompt.append("  \"issues\": [\"问题1\", \"问题2\"]\n");
        prompt.append("}\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * 解析 AI 视觉分析响应
     */
    private VisualAnalysisResult parseVisualAnalysisResponse(String aiResponse) {
        VisualAnalysisResult result = new VisualAnalysisResult();
        if (aiResponse == null || aiResponse.isEmpty()) {
            result.success = false;
            result.error = "AI 未返回分析结果";
            return result;
        }

        result.rawResponse = aiResponse;
        result.success = true;

        try {
            String json = aiResponse;
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json = aiResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode root = objectMapper.readTree(json);
            result.renderHealthScore = extractScore(root, "renderHealth");
            result.playableScore = extractScore(root, "playable");
            result.uiuxScore = extractScore(root, "uiux");
            result.visualScore = extractScore(root, "visual");

            if (root.has("overallScore")) {
                result.visualScore = root.get("overallScore").asInt(result.visualScore);
            }

            if (root.has("summary")) {
                result.summary = root.get("summary").asText("");
            }

            if (root.has("issues") && root.get("issues").isArray()) {
                for (JsonNode item : root.get("issues")) {
                    if (item.isTextual()) result.issues.add(item.asText());
                }
            }

            if (root.has("strengths") && root.get("strengths").isArray()) {
                for (JsonNode item : root.get("strengths")) {
                    if (item.isTextual()) result.strengths.add(item.asText());
                }
            }

            log.info("视觉分析结果: render={}, playable={}, uiux={}, visual={}",
                result.renderHealthScore, result.playableScore, result.uiuxScore, result.visualScore);
        } catch (Exception e) {
            log.error("解析视觉分析响应失败: {}", e.getMessage(), e);
            result.success = false;
            result.error = "解析失败: " + e.getMessage();
        }

        return result;
    }

    private int extractScore(JsonNode root, String field) {
        if (!root.has(field)) return 0;
        JsonNode node = root.get(field);
        if (node.isObject() && node.has("score")) {
            return node.get("score").asInt(0);
        }
        if (node.isNumber()) {
            return node.asInt(0);
        }
        return 0;
    }

    // ============== 工具方法 ==============

    /**
     * 构建 URL（避免路径拼接错误）
     */
    private String buildUrl(String scheme, String host, int port, String path) {
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host).append(":").append(port);
        if (path != null && !path.isBlank()) {
            if (!path.startsWith("/")) sb.append("/");
            sb.append(path);
        }
        return sb.toString();
    }

    /**
     * 清理项目 ID 中可能存在的非法字符（防止路径穿越）
     */
    private String sanitize(String input) {
        if (input == null || input.isBlank()) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 清理过期截图（保留最近 N 天）
     *
     * @param retentionDays 保留天数
     * @return 清理的文件数
     */
    public int cleanupOldScreenshots(int retentionDays) {
        if (retentionDays < 1) return 0;
        File baseDir = new File(baseDirAbsolute);
        if (!baseDir.exists()) return 0;

        long cutoffMs = System.currentTimeMillis() - retentionDays * 24L * 3600_000L;
        int deleted = 0;
        File[] projects = baseDir.listFiles();
        if (projects == null) return 0;

        for (File projectDir : projects) {
            if (!projectDir.isDirectory()) continue;
            File[] files = projectDir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isFile() && f.lastModified() < cutoffMs) {
                    if (f.delete()) deleted++;
                }
            }
        }
        log.info("清理过期截图: 保留{}天, 删除{}个文件", retentionDays, deleted);
        return deleted;
    }

    /**
     * 获取截图基础目录（供外部查询）
     */
    public String getBaseDir() {
        return baseDirAbsolute;
    }

    /**
     * 检查 Chrome 是否可用
     */
    public boolean isChromeAvailable() {
        return new File(chromePath).exists();
    }
}
