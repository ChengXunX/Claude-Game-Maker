package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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

            // 简单解析 JSON（不依赖外部库）
            result.runnableScore = extractIntField(json, "runnable");
            result.playableScore = extractIntField(json, "playable");
            result.completenessScore = extractIntField(json, "completeness");
            result.uiuxScore = extractIntField(json, "uiux");
            result.codeQualityScore = extractIntField(json, "codeQuality");
            result.overallScore = extractIntField(json, "overallScore");
            result.summary = extractStringField(json, "summary");
            result.strengths = extractStringArray(json, "strengths");
            result.issues = extractStringArray(json, "issues");
            result.suggestions = extractStringArray(json, "suggestions");
            result.success = true;

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
                }
            }

            // 日志：记录解析结果，便于排查 0 分问题
            log.info("质量分析解析结果: runnable={}, playable={}, completeness={}, uiux={}, codeQuality={}, overall={}",
                result.runnableScore, result.playableScore, result.completenessScore,
                result.uiuxScore, result.codeQualityScore, result.overallScore);

        } catch (Exception e) {
            log.warn("解析 AI 分析结果失败: {}", e.getMessage());
            result.success = true; // 仍然标记为成功，返回原始响应
            result.summary = "AI 分析完成，但结果解析失败，请查看原始响应";
        }

        return result;
    }

    private int extractIntField(String json, String fieldName) {
        try {
            // 匹配 "fieldName": { "score": 80, ... } 或 "fieldName": 80
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + fieldName + "\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*(\\d+)")
                .matcher(json);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            matcher = java.util.regex.Pattern
                .compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) { /* ignore */ }
        return 0;
    }

    private String extractStringField(String json, String fieldName) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"")
                .matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    private List<String> extractStringArray(String json, String fieldName) {
        List<String> result = new ArrayList<>();
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + fieldName + "\"\\s*:\\s*\\[([^\\]]*?)\\]")
                .matcher(json);
            if (matcher.find()) {
                String arrayContent = matcher.group(1);
                java.util.regex.Matcher itemMatcher = java.util.regex.Pattern
                    .compile("\"([^\"]*?)\"")
                    .matcher(arrayContent);
                while (itemMatcher.find()) {
                    result.add(itemMatcher.group(1));
                }
            }
        } catch (Exception e) { /* ignore */ }
        return result;
    }

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
}
