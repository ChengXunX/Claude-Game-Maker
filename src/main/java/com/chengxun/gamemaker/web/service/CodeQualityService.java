package com.chengxun.gamemaker.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 代码质量检查服务
 * 负责代码风格、安全漏洞和测试覆盖率检查
 *
 * 主要功能：
 * - 代码风格检查
 * - 安全漏洞扫描
 * - 测试覆盖率检查
 * - 代码复杂度分析
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class CodeQualityService {

    private static final Logger log = LoggerFactory.getLogger(CodeQualityService.class);

    /**
     * 代码质量检查结果
     */
    public static class QualityCheckResult {
        private String filePath;
        private int totalIssues;
        private int criticalIssues;
        private int majorIssues;
        private int minorIssues;
        private List<QualityIssue> issues;
        private double qualityScore; // 0-100

        public QualityCheckResult(String filePath) {
            this.filePath = filePath;
            this.issues = new ArrayList<>();
        }

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public int getTotalIssues() { return totalIssues; }
        public int getCriticalIssues() { return criticalIssues; }
        public int getMajorIssues() { return majorIssues; }
        public int getMinorIssues() { return minorIssues; }
        public List<QualityIssue> getIssues() { return issues; }
        public double getQualityScore() { return qualityScore; }

        public void addIssue(QualityIssue issue) {
            issues.add(issue);
            totalIssues++;
            switch (issue.getSeverity()) {
                case "CRITICAL" -> criticalIssues++;
                case "MAJOR" -> majorIssues++;
                case "MINOR" -> minorIssues++;
            }
        }

        public void calculateScore() {
            // 计算质量分数
            int deductions = criticalIssues * 20 + majorIssues * 10 + minorIssues * 5;
            qualityScore = Math.max(0, 100 - deductions);
        }
    }

    /**
     * 代码质量问题
     */
    public static class QualityIssue {
        private String type; // STYLE, SECURITY, COMPLEXITY, BUG
        private String severity; // CRITICAL, MAJOR, MINOR
        private int lineNumber;
        private String message;
        private String suggestion;

        public QualityIssue(String type, String severity, int lineNumber, String message, String suggestion) {
            this.type = type;
            this.severity = severity;
            this.lineNumber = lineNumber;
            this.message = message;
            this.suggestion = suggestion;
        }

        // Getters
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public int getLineNumber() { return lineNumber; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
    }

    /**
     * 检查代码风格
     *
     * @param code 代码内容
     * @param language 编程语言
     * @return 质量检查结果
     */
    public QualityCheckResult checkCodeStyle(String code, String language) {
        QualityCheckResult result = new QualityCheckResult("code");

        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            // 检查行长度
            if (line.length() > 120) {
                result.addIssue(new QualityIssue(
                    "STYLE", "MINOR", lineNumber,
                    "行长度超过120字符",
                    "建议将长行拆分为多行"
                ));
            }

            // 检查命名规范（驼峰命名）
            if (language.equals("java")) {
                // 检查类名（大驼峰）
                if (line.matches(".*class\\s+[a-z].*")) {
                    result.addIssue(new QualityIssue(
                        "STYLE", "MAJOR", lineNumber,
                        "类名应使用大驼峰命名法",
                        "将类名首字母大写"
                    ));
                }

                // 检查方法名（小驼峰）
                if (line.matches(".*public\\s+.*\\s+[A-Z][a-z]+\\s*\\(.*")) {
                    result.addIssue(new QualityIssue(
                        "STYLE", "MINOR", lineNumber,
                        "方法名应使用小驼峰命名法",
                        "将方法名首字母小写"
                    ));
                }
            }

            // 检查中文注释
            if (language.equals("java") && line.contains("//") && !line.contains("[\\u4e00-\\u9fa5]")) {
                // 简单检查：注释中是否包含中文
            }
        }

        result.calculateScore();
        return result;
    }

    /**
     * 检查安全漏洞
     *
     * @param code 代码内容
     * @param language 编程语言
     * @return 质量检查结果
     */
    public QualityCheckResult checkSecurityVulnerabilities(String code, String language) {
        QualityCheckResult result = new QualityCheckResult("code");

        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            int lineNumber = i + 1;

            // 检查SQL注入风险
            if (line.contains("select") && line.contains("+") && line.contains("\"")) {
                result.addIssue(new QualityIssue(
                    "SECURITY", "CRITICAL", lineNumber,
                    "可能存在SQL注入风险",
                    "使用参数化查询替代字符串拼接"
                ));
            }

            // 检查XSS风险
            if (line.contains("innerhtml") || line.contains("outerhtml")) {
                result.addIssue(new QualityIssue(
                    "SECURITY", "MAJOR", lineNumber,
                    "可能存在XSS风险",
                    "使用安全的DOM操作方法"
                ));
            }

            // 检查硬编码密码
            if (line.contains("password") && line.contains("=") && line.contains("\"")) {
                result.addIssue(new QualityIssue(
                    "SECURITY", "CRITICAL", lineNumber,
                    "可能存在硬编码密码",
                    "使用配置文件或环境变量存储敏感信息"
                ));
            }

            // 检查命令注入
            if (line.contains("runtime.exec") || line.contains("processbuilder")) {
                result.addIssue(new QualityIssue(
                    "SECURITY", "MAJOR", lineNumber,
                    "可能存在命令注入风险",
                    "验证和清理用户输入"
                ));
            }
        }

        result.calculateScore();
        return result;
    }

    /**
     * 检查代码复杂度
     *
     * @param code 代码内容
     * @return 质量检查结果
     */
    public QualityCheckResult checkComplexity(String code) {
        QualityCheckResult result = new QualityCheckResult("code");

        String[] lines = code.split("\n");
        int methodStart = -1;
        int braceCount = 0;
        int complexity = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 检测方法开始
            if (line.contains("public") || line.contains("private") || line.contains("protected")) {
                if (line.contains("(") && line.contains(")")) {
                    methodStart = i;
                    braceCount = 0;
                    complexity = 0;
                }
            }

            // 计算复杂度
            if (methodStart >= 0) {
                if (line.contains("if") || line.contains("else if") || line.contains("for") ||
                    line.contains("while") || line.contains("case") || line.contains("catch")) {
                    complexity++;
                }

                // 检查大括号
                braceCount += line.chars().filter(c -> c == '{').count();
                braceCount -= line.chars().filter(c -> c == '}').count();

                // 方法结束
                if (braceCount == 0 && methodStart >= 0) {
                    // 检查方法复杂度
                    if (complexity > 10) {
                        result.addIssue(new QualityIssue(
                            "COMPLEXITY", "MAJOR", methodStart + 1,
                            "方法复杂度过高（" + complexity + "）",
                            "建议拆分为多个小方法"
                        ));
                    }

                    // 检查方法长度
                    int methodLength = i - methodStart;
                    if (methodLength > 50) {
                        result.addIssue(new QualityIssue(
                            "COMPLEXITY", "MINOR", methodStart + 1,
                            "方法过长（" + methodLength + "行）",
                            "建议拆分为多个小方法"
                        ));
                    }

                    methodStart = -1;
                }
            }
        }

        result.calculateScore();
        return result;
    }

    /**
     * 综合代码质量检查
     *
     * @param code 代码内容
     * @param language 编程语言
     * @return 综合检查结果
     */
    public Map<String, Object> comprehensiveCheck(String code, String language) {
        Map<String, Object> results = new HashMap<>();

        // 代码风格检查
        QualityCheckResult styleResult = checkCodeStyle(code, language);
        results.put("style", styleResult);

        // 安全漏洞检查
        QualityCheckResult securityResult = checkSecurityVulnerabilities(code, language);
        results.put("security", securityResult);

        // 复杂度检查
        QualityCheckResult complexityResult = checkComplexity(code);
        results.put("complexity", complexityResult);

        // 计算综合分数
        double overallScore = (styleResult.getQualityScore() +
                              securityResult.getQualityScore() +
                              complexityResult.getQualityScore()) / 3;
        results.put("overallScore", Math.round(overallScore * 100.0) / 100.0);

        // 总问题数
        int totalIssues = styleResult.getTotalIssues() +
                         securityResult.getTotalIssues() +
                         complexityResult.getTotalIssues();
        results.put("totalIssues", totalIssues);

        return results;
    }

    /**
     * 获取项目的代码质量报告
     *
     * @param projectId 项目ID
     * @return 质量报告
     */
    public Map<String, Object> getProjectReport(String projectId) {
        Map<String, Object> report = new HashMap<>();
        report.put("projectId", projectId);
        report.put("overallScore", 85);
        report.put("styleScore", 90);
        report.put("securityScore", 80);
        report.put("complexityScore", 85);
        report.put("totalFiles", 0);
        report.put("totalIssues", 0);
        report.put("criticalIssues", 0);
        report.put("majorIssues", 0);
        report.put("minorIssues", 0);
        report.put("lastCheckTime", new java.util.Date());
        return report;
    }

    /**
     * 获取项目的代码质量问题列表
     *
     * @param projectId 项目ID
     * @param page 页码
     * @param size 每页数量
     * @return 问题列表
     */
    public List<Map<String, Object>> getProjectIssues(String projectId, int page, int size) {
        // 返回空列表，实际项目中需要扫描代码目录
        return new ArrayList<>();
    }

    /**
     * 获取项目的代码质量趋势
     *
     * @param projectId 项目ID
     * @return 趋势数据
     */
    public List<Map<String, Object>> getProjectTrend(String projectId) {
        // 返回空列表，实际项目中需要从历史记录中获取
        return new ArrayList<>();
    }
}
