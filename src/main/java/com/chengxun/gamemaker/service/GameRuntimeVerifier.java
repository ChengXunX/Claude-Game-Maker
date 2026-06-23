package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用项目结构验证服务
 * 验证 Agent 产出的项目是否结构完整、可继续开发
 *
 * 验证维度：
 * 1. 结构验证：项目目录、源代码、配置文件
 * 2. AI 深度分析：可玩性、玩法完整性、UI/UX 质量
 *
 * 设计原则：
 * - 不假设特定技术栈（HTML/Unity/Godot 等）
 * - 根据项目目录中实际存在的文件进行验证
 * - 验证结构完整性，不尝试运行或编译项目
 * - 游戏类型和技术栈由 Agent 协作决定，验证器只检查产出质量
 *
 * 验证维度：
 * 1. 项目目录存在且非空
 * 2. 存在源代码或配置文件（证明有实际产出）
 * 3. 配置文件格式正确（如有）
 * 4. 无明显的空文件或占位符
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class GameRuntimeVerifier {

    private static final Logger log = LoggerFactory.getLogger(GameRuntimeVerifier.class);

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ClaudeAiService aiService;

    /** 构建策略注册中心（延迟注入） */
    @Autowired(required = false)
    private BuildStrategyRegistry buildStrategyRegistry;

    /** 运行时错误检测器（延迟注入） */
    @Autowired(required = false)
    private RuntimeErrorDetector runtimeErrorDetector;

    /** JSON 序列化工具 */
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /** 源代码文件扩展名（通用，不限定具体语言） */
    private static final String[] SOURCE_EXTENSIONS = {
        ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".vue", ".svelte",
        ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php",
        ".swift", ".kt", ".dart", ".lua", ".r", ".zig", ".nim", ".ex",
        ".gd", ".gdscript", ".shader", ".hlsl", ".glsl"
    };

    /** 配置/构建文件名 */
    private static final String[] BUILD_CONFIG_FILES = {
        "package.json", "pom.xml", "build.gradle", "Cargo.toml",
        "CMakeLists.txt", "Makefile", "requirements.txt", "pyproject.toml",
        "project.godot", "*.csproj", "*.sln", "*.uproject",
        "index.html", "webpack.config.js", "vite.config.js", "tsconfig.json"
    };

    /** 文档/设计文件扩展名 */
    private static final String[] DOC_EXTENSIONS = {
        ".md", ".txt", ".doc", ".docx", ".pdf", ".rst", ".adoc"
    };

    /**
     * 验证项目结构完整性
     *
     * @param projectDir 项目目录绝对路径
     * @return 验证结果
     */
    public VerifyResult verify(String projectDir) {
        if (projectDir == null || projectDir.isEmpty()) {
            return VerifyResult.failure("项目目录为空");
        }

        File dir = new File(projectDir);
        if (!dir.exists()) {
            return VerifyResult.failure("项目目录不存在: " + projectDir);
        }
        if (!dir.isDirectory()) {
            return VerifyResult.failure("路径不是目录: " + projectDir);
        }

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            return VerifyResult.failure("项目目录为空，没有任何文件");
        }

        List<String> warnings = new ArrayList<>();
        List<String> passed = new ArrayList<>();

        // 1. 检查是否有源代码文件
        List<File> sourceFiles = findFilesByExtensions(dir, SOURCE_EXTENSIONS, 5);
        if (sourceFiles.isEmpty()) {
            // 没有常见源代码文件，检查是否有其他可执行内容
            List<File> anyFiles = findFilesByExtensions(dir, new String[]{".html", ".xml", ".json", ".yaml", ".yml"}, 3);
            if (anyFiles.isEmpty()) {
                return VerifyResult.failure("项目中未找到源代码文件或配置文件");
            }
            warnings.add("未找到常见编程语言的源代码文件，可能使用了非标准技术栈");
        } else {
            passed.add("找到 " + sourceFiles.size() + " 个源代码文件");
        }

        // 2. 检查是否有构建/项目配置文件
        boolean hasBuildConfig = false;
        for (String configPattern : BUILD_CONFIG_FILES) {
            if (configPattern.startsWith("*")) {
                // 通配符匹配
                String ext = configPattern.substring(1);
                File[] matches = dir.listFiles((d, name) -> name.endsWith(ext));
                if (matches != null && matches.length > 0) {
                    hasBuildConfig = true;
                    break;
                }
            } else {
                if (findFileInProject(dir, configPattern, 2) != null) {
                    hasBuildConfig = true;
                    break;
                }
            }
        }
        if (hasBuildConfig) {
            passed.add("存在构建/项目配置文件");
        } else {
            warnings.add("未找到标准构建配置文件（如 package.json、pom.xml、Cargo.toml 等）");
        }

        // 3. 检查是否有文档/设计文件
        List<File> docFiles = findFilesByExtensions(dir, DOC_EXTENSIONS, 3);
        if (!docFiles.isEmpty()) {
            passed.add("存在 " + docFiles.size() + " 个文档文件");
        }

        // 4. 检查是否有明显的空文件（可能是占位符）
        int emptyFileCount = countEmptyFiles(dir, 3);
        if (emptyFileCount > 3) {
            warnings.add("发现 " + emptyFileCount + " 个空文件，可能是未完成的占位符");
        }

        // 5. 检查目录结构深度（过于扁平可能表示未组织好）
        int maxDepth = getMaxDepth(dir, 0, 5);
        if (maxDepth == 0) {
            warnings.add("所有文件都在根目录，缺少目录组织");
        } else if (maxDepth >= 2) {
            passed.add("目录结构有 " + (maxDepth + 1) + " 层深度");
        }

        // 汇总结果
        if (passed.isEmpty() && !warnings.isEmpty()) {
            return VerifyResult.failure("项目结构不完整: " + String.join("; ", warnings));
        }

        String message = "项目结构验证通过";
        if (!passed.isEmpty()) {
            message += " (" + String.join(", ", passed) + ")";
        }

        if (warnings.isEmpty()) {
            return VerifyResult.success(message);
        } else {
            return VerifyResult.successWithWarnings(message, warnings);
        }
    }

    /**
     * 递归查找指定扩展名的文件
     *
     * @param dir 搜索目录
     * @param extensions 扩展名数组
     * @param maxDepth 最大搜索深度
     * @return 找到的文件列表
     */
    private List<File> findFilesByExtensions(File dir, String[] extensions, int maxDepth) {
        List<File> result = new ArrayList<>();
        findFilesRecursive(dir, extensions, 0, maxDepth, result);
        return result;
    }

    private void findFilesRecursive(File dir, String[] extensions, int depth, int maxDepth, List<File> result) {
        if (depth > maxDepth) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 跳过常见的非源码目录
                String name = file.getName();
                if (name.equals("node_modules") || name.equals(".git") || name.equals(".claude")
                    || name.equals("target") || name.equals("build") || name.equals("dist")
                    || name.equals("__pycache__") || name.equals(".idea") || name.equals(".vscode")) {
                    continue;
                }
                findFilesRecursive(file, extensions, depth + 1, maxDepth, result);
            } else {
                String fileName = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        result.add(file);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 在项目中查找指定文件名
     */
    private File findFileInProject(File dir, String fileName, int maxDepth) {
        if (maxDepth < 0) return null;

        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return file;
            }
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                File found = findFileInProject(file, fileName, maxDepth - 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * 统计空文件数量
     */
    private int countEmptyFiles(File dir, int maxDepth) {
        if (maxDepth < 0) return 0;

        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isFile() && file.length() == 0) {
                count++;
            } else if (file.isDirectory() && !isExcludedDir(file.getName())) {
                count += countEmptyFiles(file, maxDepth - 1);
            }
        }
        return count;
    }

    /**
     * 获取目录最大深度
     */
    private int getMaxDepth(File dir, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) return currentDepth;

        int deepest = currentDepth;
        File[] files = dir.listFiles();
        if (files == null) return currentDepth;

        for (File file : files) {
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                deepest = Math.max(deepest, getMaxDepth(file, currentDepth + 1, maxDepth));
            }
        }
        return deepest;
    }

    /**
     * 判断是否为应排除的目录
     */
    private boolean isExcludedDir(String name) {
        return name.equals("node_modules") || name.equals(".git") || name.equals(".claude")
            || name.equals("target") || name.equals("build") || name.equals("dist")
            || name.equals("__pycache__") || name.equals(".idea") || name.equals(".vscode")
            || name.equals(".gradle") || name.equals(".mvn");
    }

    /**
     * 验证结果
     */
    public static class VerifyResult {
        private final boolean success;
        private final String message;
        private final List<String> warnings;
        private final String error;

        private VerifyResult(boolean success, String message, List<String> warnings, String error) {
            this.success = success;
            this.message = message;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.error = error;
        }

        public static VerifyResult success(String message) {
            return new VerifyResult(true, message, null, null);
        }

        public static VerifyResult successWithWarnings(String message, List<String> warnings) {
            return new VerifyResult(true, message, warnings, null);
        }

        public static VerifyResult failure(String error) {
            return new VerifyResult(false, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<String> getWarnings() { return warnings; }
        public String getError() { return error; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }

        /**
         * 获取格式化的结果摘要（用于注入 Agent prompt）
         */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            if (success) {
                sb.append("验证通过: ").append(message).append("\n");
                if (!warnings.isEmpty()) {
                    sb.append("警告:\n");
                    for (String w : warnings) {
                        sb.append("  - ").append(w).append("\n");
                    }
                }
            } else {
                sb.append("验证失败: ").append(error).append("\n");
            }
            return sb.toString();
        }
    }

    // ===== AI 深度分析 =====

    /**
     * AI 深度分析游戏质量
     * 读取项目代码，使用 AI 评估可玩性、玩法完整性、UI/UX 质量
     *
     * @param projectDir 项目目录
     * @param projectName 项目名称（可选）
     * @param projectGoal 项目目标（可选）
     * @return 分析结果
     */
    public QualityAnalysisResult analyzeQuality(String projectDir, String projectName, String projectGoal) {
        if (projectDir == null || projectDir.isEmpty()) {
            return QualityAnalysisResult.failure("项目目录为空");
        }

        File dir = new File(projectDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return QualityAnalysisResult.failure("项目目录不存在: " + projectDir);
        }

        // 1. 收集项目代码样本
        String codeSample = collectCodeSample(dir);
        if (codeSample.isEmpty()) {
            return QualityAnalysisResult.failure("项目中未找到可分析的代码文件");
        }

        // 2. 收集项目结构信息
        String structureInfo = collectStructureInfo(dir);

        // 3. 使用 AI 分析
        if (aiService == null) {
            return QualityAnalysisResult.failure("AI 服务不可用，无法进行深度分析");
        }

        try {
            String prompt = buildAnalysisPrompt(projectName, projectGoal, structureInfo, codeSample);
            String aiResponse = aiService.sendMessage(prompt);

            // 4. 解析 AI 响应
            return parseAnalysisResult(aiResponse);
        } catch (Exception e) {
            log.error("AI 深度分析失败: {}", e.getMessage(), e);
            return QualityAnalysisResult.failure("AI 分析失败: " + e.getMessage());
        }
    }

    /**
     * 收集项目代码样本（用于 AI 分析）
     * 取每个源代码文件的前 200 行，总长度限制 15000 字符
     */
    private String collectCodeSample(File dir) {
        StringBuilder sample = new StringBuilder();
        List<File> sourceFiles = findFilesByExtensions(dir, SOURCE_EXTENSIONS, 4);

        for (File file : sourceFiles) {
            if (sample.length() > 15000) break;
            try {
                String content = Files.readString(file.toPath());
                // 截取前 200 行
                String[] lines = content.split("\n");
                int lineCount = Math.min(lines.length, 200);
                StringBuilder fileContent = new StringBuilder();
                for (int i = 0; i < lineCount; i++) {
                    fileContent.append(lines[i]).append("\n");
                }
                if (lines.length > 200) {
                    fileContent.append("... (").append(lines.length - 200).append(" more lines)\n");
                }

                String relativePath = dir.toPath().relativize(file.toPath()).toString();
                sample.append("### ").append(relativePath).append("\n");
                sample.append("```\n").append(fileContent).append("```\n\n");
            } catch (Exception e) {
                // 忽略读取失败的文件
            }
        }
        return sample.toString();
    }

    /**
     * 收集项目结构信息
     */
    private String collectStructureInfo(File dir) {
        StringBuilder info = new StringBuilder();
        info.append("项目目录: ").append(dir.getAbsolutePath()).append("\n");

        // 列出一级目录和文件
        File[] files = dir.listFiles();
        if (files != null) {
            info.append("根目录内容:\n");
            for (File file : files) {
                if (file.isDirectory() && !isExcludedDir(file.getName())) {
                    int fileCount = countFiles(file, 0, 2);
                    info.append("  📁 ").append(file.getName()).append("/ (").append(fileCount).append(" 个文件)\n");
                } else if (file.isFile()) {
                    info.append("  📄 ").append(file.getName()).append("\n");
                }
            }
        }

        // 统计文件类型
        info.append("\n文件类型统计:\n");
        String[][] typeGroups = {
            {".js", ".ts", ".jsx", ".tsx", "JavaScript/TypeScript"},
            {".py", "Python"},
            {".java", "Java"},
            {".html", ".css", "HTML/CSS"},
            {".cs", "C#/.NET"},
            {".gd", ".gdscript", "GDScript/Godot"},
            {".json", ".yaml", ".yml", "配置文件"},
            {".md", ".txt", "文档"}
        };
        for (String[] group : typeGroups) {
            int count = 0;
            for (int i = 0; i < group.length - 1; i++) {
                count += countFilesByExt(dir, group[i], 4);
            }
            if (count > 0) {
                info.append("  ").append(group[group.length - 1]).append(": ").append(count).append(" 个\n");
            }
        }

        return info.toString();
    }

    /**
     * 递归统计文件数量
     */
    private int countFiles(File dir, int depth, int maxDepth) {
        if (depth > maxDepth) return 0;
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isFile()) count++;
            else if (file.isDirectory() && !isExcludedDir(file.getName())) {
                count += countFiles(file, depth + 1, maxDepth);
            }
        }
        return count;
    }

    /**
     * 统计指定扩展名的文件数量
     */
    private int countFilesByExt(File dir, String ext, int maxDepth) {
        List<File> files = findFilesByExtensions(dir, new String[]{ext}, maxDepth);
        return files.size();
    }

    /**
     * 构建 AI 分析 prompt
     */
    private String buildAnalysisPrompt(String projectName, String projectGoal,
                                         String structureInfo, String codeSample) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的游戏质量评估专家。请分析以下游戏项目的代码和结构，给出全面的质量评估。\n\n");

        if (projectName != null && !projectName.isEmpty()) {
            prompt.append("项目名称: ").append(projectName).append("\n");
        }
        if (projectGoal != null && !projectGoal.isEmpty()) {
            prompt.append("项目目标: ").append(projectGoal).append("\n");
        }
        prompt.append("\n");

        prompt.append("## 项目结构\n\n").append(structureInfo).append("\n\n");

        prompt.append("## 代码样本\n\n").append(codeSample).append("\n\n");

        prompt.append("## 评估要求\n\n");
        prompt.append("请从以下 5 个维度评估，每个维度 0-100 分：\n\n");
        prompt.append("1. **可运行性** (runnable): 代码是否完整、无明显语法错误、能否正常启动运行\n");
        prompt.append("2. **可玩性** (playable): 游戏是否有完整的核心玩法循环、玩家能否实际游玩\n");
        prompt.append("3. **玩法完整性** (completeness): 游戏机制是否完整、有无明显的功能缺失\n");
        prompt.append("4. **UI/UX 质量** (uiux): 界面是否有设计、交互是否合理、视觉效果如何\n");
        prompt.append("5. **代码质量** (codeQuality): 代码结构是否清晰、有无明显 bug\n\n");

        prompt.append("请严格按以下 JSON 格式输出（不要有其他内容）：\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"runnable\": { \"score\": 80, \"comment\": \"简要说明\" },\n");
        prompt.append("  \"playable\": { \"score\": 60, \"comment\": \"简要说明\" },\n");
        prompt.append("  \"completeness\": { \"score\": 50, \"comment\": \"简要说明\" },\n");
        prompt.append("  \"uiux\": { \"score\": 70, \"comment\": \"简要说明\" },\n");
        prompt.append("  \"codeQuality\": { \"score\": 65, \"comment\": \"简要说明\" },\n");
        prompt.append("  \"overallScore\": 65,\n");
        prompt.append("  \"summary\": \"一句话总评\",\n");
        prompt.append("  \"strengths\": [\"优点1\", \"优点2\"],\n");
        prompt.append("  \"issues\": [\"问题1\", \"问题2\"],\n");
        prompt.append("  \"suggestions\": [\"建议1\", \"建议2\"]\n");
        prompt.append("}\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * 解析 AI 分析结果
     * 使用 Jackson ObjectMapper 解析 JSON，更健壮
     */
    private QualityAnalysisResult parseAnalysisResult(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return QualityAnalysisResult.failure("AI 未返回分析结果");
        }

        QualityAnalysisResult result = new QualityAnalysisResult();
        result.rawResponse = aiResponse;

        try {
            // 提取 JSON 块
            String json = aiResponse;
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json = aiResponse.substring(jsonStart, jsonEnd + 1);
            }

            // 使用 Jackson ObjectMapper 解析 JSON
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(json);

            // 解析各维度分数（支持嵌套对象和直接数值两种格式）
            result.runnableScore = extractScoreFromNode(rootNode, "runnable");
            result.playableScore = extractScoreFromNode(rootNode, "playable");
            result.completenessScore = extractScoreFromNode(rootNode, "completeness");
            result.uiuxScore = extractScoreFromNode(rootNode, "uiux");
            result.codeQualityScore = extractScoreFromNode(rootNode, "codeQuality");

            // 解析 overallScore（直接数值）
            if (rootNode.has("overallScore")) {
                result.overallScore = rootNode.get("overallScore").asInt(0);
            }

            // 解析字符串字段
            if (rootNode.has("summary")) {
                result.summary = rootNode.get("summary").asText("");
            }

            // 解析数组字段
            result.strengths = extractArrayFromNode(rootNode, "strengths");
            result.issues = extractArrayFromNode(rootNode, "issues");
            result.suggestions = extractArrayFromNode(rootNode, "suggestions");

            result.success = true;

            // 日志：记录原始解析结果
            log.info("AI 质量分析原始解析: runnable={}, playable={}, completeness={}, uiux={}, codeQuality={}, overall={}",
                result.runnableScore, result.playableScore, result.completenessScore,
                result.uiuxScore, result.codeQualityScore, result.overallScore);

            // overallScore 兜底：如果 AI 未返回 overallScore，则从各维度分数计算平均值
            if (result.overallScore == 0) {
                int[] dims = {result.runnableScore, result.playableScore,
                    result.completenessScore, result.uiuxScore, result.codeQualityScore};
                int nonZeroCount = 0;
                int sum = 0;
                for (int d : dims) {
                    if (d > 0) { sum += d; nonZeroCount++; }
                }
                if (nonZeroCount > 0) {
                    result.overallScore = sum / nonZeroCount;
                    log.info("AI 未返回 overallScore，从 {} 个维度计算平均分: {}", nonZeroCount, result.overallScore);
                } else {
                    // 所有维度分数都是 0，说明解析完全失败
                    log.error("AI 返回的质量分数全部为 0，解析失败。原始响应: {}", aiResponse);
                    return QualityAnalysisResult.failure("AI 返回的质量分数解析失败，请检查 AI 服务配置");
                }
            }

            // 确保各维度分数不为 0（如果为 0 说明解析失败，使用 overallScore 的值）
            if (result.runnableScore == 0) result.runnableScore = result.overallScore;
            if (result.playableScore == 0) result.playableScore = result.overallScore;
            if (result.completenessScore == 0) result.completenessScore = result.overallScore;
            if (result.uiuxScore == 0) result.uiuxScore = result.overallScore;
            if (result.codeQualityScore == 0) result.codeQualityScore = result.overallScore;

            log.info("质量分析最终结果: runnable={}, playable={}, completeness={}, uiux={}, codeQuality={}, overall={}",
                result.runnableScore, result.playableScore, result.completenessScore,
                result.uiuxScore, result.codeQualityScore, result.overallScore);

        } catch (Exception e) {
            log.error("解析 AI 分析结果失败: {}, 原始响应: {}", e.getMessage(), aiResponse);
            return QualityAnalysisResult.failure("AI 分析结果解析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从 JsonNode 中提取分数
     * 支持两种格式：
     * 1. "fieldName": { "score": 80, "comment": "..." } （嵌套对象）
     * 2. "fieldName": 80 （直接数值）
     */
    private int extractScoreFromNode(com.fasterxml.jackson.databind.JsonNode rootNode, String fieldName) {
        if (!rootNode.has(fieldName)) {
            return 0;
        }

        com.fasterxml.jackson.databind.JsonNode fieldNode = rootNode.get(fieldName);

        // 格式1: 嵌套对象 { "score": 80, "comment": "..." }
        if (fieldNode.isObject() && fieldNode.has("score")) {
            return fieldNode.get("score").asInt(0);
        }

        // 格式2: 直接数值
        if (fieldNode.isNumber()) {
            return fieldNode.asInt(0);
        }

        return 0;
    }

    /**
     * 从 JsonNode 中提取字符串数组
     */
    private List<String> extractArrayFromNode(com.fasterxml.jackson.databind.JsonNode rootNode, String fieldName) {
        List<String> result = new ArrayList<>();
        if (!rootNode.has(fieldName)) {
            return result;
        }

        com.fasterxml.jackson.databind.JsonNode arrayNode = rootNode.get(fieldName);
        if (arrayNode.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : arrayNode) {
                if (item.isTextual()) {
                    result.add(item.asText());
                }
            }
        }

        return result;
    }

    /**
     * 从 JSON 字符串中提取整数字段
     * 支持两种格式：
    /**
     * 游戏质量分析结果
     */
    public static class QualityAnalysisResult {
        private boolean success;
        private String error;
        private int runnableScore;
        private int playableScore;
        private int completenessScore;
        private int uiuxScore;
        private int codeQualityScore;
        private int overallScore;
        private String summary;
        private List<String> strengths = new ArrayList<>();
        private List<String> issues = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private String rawResponse;

        public static QualityAnalysisResult failure(String error) {
            QualityAnalysisResult r = new QualityAnalysisResult();
            r.success = false;
            r.error = error;
            return r;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public int getRunnableScore() { return runnableScore; }
        public int getPlayableScore() { return playableScore; }
        public int getCompletenessScore() { return completenessScore; }
        public int getUiuxScore() { return uiuxScore; }
        public int getCodeQualityScore() { return codeQualityScore; }
        public int getOverallScore() { return overallScore; }
        public String getSummary() { return summary; }
        public List<String> getStrengths() { return strengths; }
        public List<String> getIssues() { return issues; }
        public List<String> getSuggestions() { return suggestions; }
        public String getRawResponse() { return rawResponse; }

        // Setters (用于从数据库恢复)
        public void setSuccess(boolean success) { this.success = success; }
        public void setError(String error) { this.error = error; }
        public void setRunnableScore(int runnableScore) { this.runnableScore = runnableScore; }
        public void setPlayableScore(int playableScore) { this.playableScore = playableScore; }
        public void setCompletenessScore(int completenessScore) { this.completenessScore = completenessScore; }
        public void setUiuxScore(int uiuxScore) { this.uiuxScore = uiuxScore; }
        public void setCodeQualityScore(int codeQualityScore) { this.codeQualityScore = codeQualityScore; }
        public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
        public void setSummary(String summary) { this.summary = summary; }
        public void setStrengths(List<String> strengths) { this.strengths = strengths; }
        public void setIssues(List<String> issues) { this.issues = issues; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    }

    // ===== 运行验证（新增） =====

    /**
     * 执行构建验证
     * 使用 BuildStrategyRegistry 自动检测项目类型并执行构建
     *
     * @param projectDir 项目目录
     * @return 构建结果
     */
    public BuildResult verifyBuild(String projectDir) {
        if (buildStrategyRegistry == null) {
            log.debug("BuildStrategyRegistry 未注入，跳过构建验证");
            return BuildResult.success("构建策略注册中心不可用，跳过构建验证", 0, new ArrayList<>());
        }
        if (projectDir == null || projectDir.isEmpty()) {
            return BuildResult.failure("项目目录为空", "unknown", 0);
        }
        return buildStrategyRegistry.build(projectDir);
    }

    /**
     * 执行运行验证
     * 启动游戏服务，检查是否能正常运行，检测运行时错误
     *
     * @param projectDir 项目目录
     * @return 运行验证结果
     */
    public RuntimeVerifyResult verifyRuntime(String projectDir) {
        if (buildStrategyRegistry == null) {
            log.debug("BuildStrategyRegistry 未注入，跳过运行验证");
            return RuntimeVerifyResult.success(null, List.of(), List.of());
        }
        if (projectDir == null || projectDir.isEmpty()) {
            return RuntimeVerifyResult.failure("项目目录为空", null);
        }

        int port = findAvailablePort();
        Process gameProcess = null;

        try {
            // 启动游戏
            BuildStrategyRegistry.GameProcess gp = buildStrategyRegistry.startGame(projectDir, port);
            gameProcess = gp.getProcess();

            // 等待就绪（最多 30 秒）
            boolean ready = waitForReady(gp, 30);

            if (!ready) {
                String output = collectProcessOutput(gameProcess);
                return RuntimeVerifyResult.failure("游戏启动超时（30秒）", output);
            }

            // HTTP 检查
            HttpStatusCheck httpCheck = checkHttpStatus(port);

            // 收集控制台错误
            List<String> consoleErrors = new ArrayList<>();
            if (runtimeErrorDetector != null) {
                String output = collectProcessOutput(gameProcess);
                List<RuntimeErrorDetector.RuntimeError> errors = runtimeErrorDetector.detectFromOutput(output);
                consoleErrors = errors.stream()
                    .map(e -> String.format("[%s] %s (行%d)", e.getType().getDescription(), e.getMessage(), e.getLineNumber()))
                    .collect(Collectors.toList());
            }

            // 收集资源错误
            List<String> resourceErrors = runtimeErrorDetector != null
                ? runtimeErrorDetector.detectResourceErrors(port)
                : List.of();

            boolean passed = httpCheck.isSuccess() && consoleErrors.isEmpty();

            return passed
                ? RuntimeVerifyResult.success(httpCheck, consoleErrors, resourceErrors)
                : RuntimeVerifyResult.failure(
                    "运行验证失败: HTTP=" + httpCheck.getStatusCode()
                        + ", 控制台错误=" + consoleErrors.size(),
                    collectProcessOutput(gameProcess));

        } catch (Exception e) {
            return RuntimeVerifyResult.failure("运行验证异常: " + e.getMessage(), null);
        } finally {
            // 确保停止游戏进程
            if (gameProcess != null) {
                gameProcess.descendants().forEach(ProcessHandle::destroy);
                gameProcess.destroy();
            }
        }
    }

    /**
     * 执行完整验证（结构 + 构建 + 运行 + AI 质量分析）
     *
     * @param projectDir 项目目录
     * @param projectName 项目名称（可选）
     * @param projectGoal 项目目标（可选）
     * @return 完整验证结果
     */
    public FullVerifyResult verifyFull(String projectDir, String projectName, String projectGoal) {
        FullVerifyResult result = new FullVerifyResult();

        // 1. 结构验证
        result.setStructuralResult(verify(projectDir));

        // 2. 构建验证
        result.setBuildResult(verifyBuild(projectDir));

        // 3. 运行验证（仅当构建通过时）
        if (result.getBuildResult() != null && result.getBuildResult().isSuccess()) {
            result.setRuntimeResult(verifyRuntime(projectDir));
        }

        // 4. AI 质量分析（仅当结构验证通过时）
        if (result.getStructuralResult() != null && result.getStructuralResult().isSuccess()) {
            result.setQualityResult(analyzeQuality(projectDir, projectName, projectGoal));
        }

        // 5. 综合评分
        result.calculateOverallScore();

        return result;
    }

    /**
     * 查找可用端口（18100-18199 范围）
     *
     * @return 可用端口号
     */
    private int findAvailablePort() {
        for (int port = 18100; port < 18200; port++) {
            try (var socket = new java.net.ServerSocket(port)) {
                return port;
            } catch (Exception e) {
                // 端口被占用，尝试下一个
            }
        }
        return 18100; // 兜底
    }

    /**
     * 等待游戏服务就绪
     *
     * @param gp 游戏进程包装
     * @param timeoutSeconds 超时秒数
     * @return 是否就绪
     */
    private boolean waitForReady(BuildStrategyRegistry.GameProcess gp, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (gp.isReady()) return true;
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return false;
    }

    /**
     * HTTP 状态检查
     *
     * @param port 端口号
     * @return HTTP 状态检查结果
     */
    private HttpStatusCheck checkHttpStatus(int port) {
        try {
            var url = new java.net.URL("http://localhost:" + port);
            var conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return new HttpStatusCheck(code, code == 200);
        } catch (Exception e) {
            return new HttpStatusCheck(0, false);
        }
    }

    /**
     * 收集进程输出（最后 100 行）
     *
     * @param process 进程
     * @return 进程输出文本
     */
    private String collectProcessOutput(Process process) {
        if (process == null) return "";
        try {
            byte[] bytes = process.getInputStream().readAllBytes();
            String output = new String(bytes);
            String[] lines = output.split("\n");
            if (lines.length <= 100) return output;
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - 100; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ===== 运行验证结果类 =====

    /**
     * 运行验证结果
     */
    public static class RuntimeVerifyResult {
        private final boolean success;
        private final HttpStatusCheck httpCheck;
        private final List<String> consoleErrors;
        private final List<String> resourceErrors;
        private final String error;
        private final String processOutput;

        private RuntimeVerifyResult(boolean success, HttpStatusCheck httpCheck,
                                     List<String> consoleErrors, List<String> resourceErrors,
                                     String error, String processOutput) {
            this.success = success;
            this.httpCheck = httpCheck;
            this.consoleErrors = consoleErrors != null ? consoleErrors : new ArrayList<>();
            this.resourceErrors = resourceErrors != null ? resourceErrors : new ArrayList<>();
            this.error = error;
            this.processOutput = processOutput;
        }

        public static RuntimeVerifyResult success(HttpStatusCheck httpCheck,
                                                    List<String> consoleErrors,
                                                    List<String> resourceErrors) {
            return new RuntimeVerifyResult(true, httpCheck, consoleErrors, resourceErrors, null, null);
        }

        public static RuntimeVerifyResult failure(String error, String processOutput) {
            return new RuntimeVerifyResult(false, null, null, null, error, processOutput);
        }

        public boolean isSuccess() { return success; }
        public HttpStatusCheck getHttpCheck() { return httpCheck; }
        public List<String> getConsoleErrors() { return consoleErrors; }
        public List<String> getResourceErrors() { return resourceErrors; }
        public String getError() { return error; }
        public String getProcessOutput() { return processOutput; }
    }

    /**
     * HTTP 状态检查结果
     */
    public static class HttpStatusCheck {
        private final int statusCode;
        private final boolean success;

        public HttpStatusCheck(int statusCode, boolean success) {
            this.statusCode = statusCode;
            this.success = success;
        }

        public int getStatusCode() { return statusCode; }
        public boolean isSuccess() { return success; }
    }

    /**
     * 完整验证结果（结构 + 构建 + 运行 + AI 质量）
     */
    public static class FullVerifyResult {
        private VerifyResult structuralResult;
        private BuildResult buildResult;
        private RuntimeVerifyResult runtimeResult;
        private QualityAnalysisResult qualityResult;
        private int overallScore;
        private boolean overallPassed;

        /**
         * 计算综合评分
         * 权重：结构 10%, 构建 20%, 运行 30%, AI质量 40%
         */
        public void calculateOverallScore() {
            int score = 0;
            int weight = 0;

            if (structuralResult != null) {
                score += (structuralResult.isSuccess() ? 100 : 0) * 10;
                weight += 10;
            }
            if (buildResult != null) {
                score += (buildResult.isSuccess() ? 100 : 0) * 20;
                weight += 20;
            }
            if (runtimeResult != null) {
                score += (runtimeResult.isSuccess() ? 100 : 0) * 30;
                weight += 30;
            }
            if (qualityResult != null && qualityResult.isSuccess()) {
                score += qualityResult.getOverallScore() * 40;
                weight += 40;
            }

            overallScore = weight > 0 ? score / weight : 0;
            overallPassed = overallScore >= 60
                && (structuralResult == null || structuralResult.isSuccess())
                && (buildResult == null || buildResult.isSuccess());
        }

        public VerifyResult getStructuralResult() { return structuralResult; }
        public void setStructuralResult(VerifyResult r) { this.structuralResult = r; }
        public BuildResult getBuildResult() { return buildResult; }
        public void setBuildResult(BuildResult r) { this.buildResult = r; }
        public RuntimeVerifyResult getRuntimeResult() { return runtimeResult; }
        public void setRuntimeResult(RuntimeVerifyResult r) { this.runtimeResult = r; }
        public QualityAnalysisResult getQualityResult() { return qualityResult; }
        public void setQualityResult(QualityAnalysisResult r) { this.qualityResult = r; }
        public int getOverallScore() { return overallScore; }
        public boolean isOverallPassed() { return overallPassed; }
    }
}
