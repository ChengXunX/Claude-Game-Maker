package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏知识库
 * 存储和管理游戏开发知识，包括：
 * - 模板使用记录和成功率
 * - 常见问题和解决方案
 * - 最佳实践和代码片段
 * - 用户反馈和评分
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(GameKnowledgeBase.class);

    /** 知识库存储目录 */
    private static final String KB_DIR = "data/knowledge-base";

    /** 模板使用记录 */
    private final Map<String, List<TemplateUsageRecord>> usageRecords = new ConcurrentHashMap<>();

    /** 问题解决方案库 */
    private final Map<String, List<Solution>> solutions = new ConcurrentHashMap<>();

    /** 最佳实践库 */
    private final Map<String, BestPractice> bestPractices = new ConcurrentHashMap<>();

    /**
     * 初始化知识库
     */
    public void init() {
        try {
            Files.createDirectories(Path.of(KB_DIR));
            loadFromDisk();
            log.info("游戏知识库初始化完成");
        } catch (IOException e) {
            log.error("初始化知识库失败", e);
        }
    }

    /**
     * 记录模板使用
     */
    public void recordTemplateUsage(String templateId, String gameDescription, boolean success, long durationMs) {
        TemplateUsageRecord record = new TemplateUsageRecord(
            UUID.randomUUID().toString(),
            templateId,
            gameDescription,
            success,
            durationMs,
            LocalDateTime.now()
        );

        usageRecords.computeIfAbsent(templateId, k -> new ArrayList<>()).add(record);

        // 保存到磁盘
        saveUsageRecord(record);

        log.info("记录模板使用: template={}, success={}, duration={}ms", templateId, success, durationMs);
    }

    /**
     * 记录问题解决方案
     */
    public void recordSolution(String problemType, String problemDescription, String solution) {
        Solution sol = new Solution(
            UUID.randomUUID().toString(),
            problemType,
            problemDescription,
            solution,
            LocalDateTime.now()
        );

        solutions.computeIfAbsent(problemType, k -> new ArrayList<>()).add(sol);

        // 保存到磁盘
        saveSolution(sol);

        log.info("记录问题解决方案: type={}", problemType);
    }

    /**
     * 记录最佳实践
     */
    public void recordBestPractice(String category, String title, String content) {
        BestPractice practice = new BestPractice(
            UUID.randomUUID().toString(),
            category,
            title,
            content,
            LocalDateTime.now()
        );

        bestPractices.put(practice.getId(), practice);

        // 保存到磁盘
        saveBestPractice(practice);

        log.info("记录最佳实践: category={}, title={}", category, title);
    }

    /**
     * 获取模板成功率
     */
    public double getTemplateSuccessRate(String templateId) {
        List<TemplateUsageRecord> records = usageRecords.get(templateId);
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        long successCount = records.stream().filter(TemplateUsageRecord::isSuccess).count();
        return (double) successCount / records.size() * 100;
    }

    /**
     * 获取模板平均生成时间
     */
    public long getTemplateAvgDuration(String templateId) {
        List<TemplateUsageRecord> records = usageRecords.get(templateId);
        if (records == null || records.isEmpty()) {
            return 0;
        }

        return (long) records.stream()
            .mapToLong(TemplateUsageRecord::getDurationMs)
            .average()
            .orElse(0);
    }

    /**
     * 获取问题解决方案
     */
    public List<Solution> getSolutions(String problemType) {
        return solutions.getOrDefault(problemType, Collections.emptyList());
    }

    /**
     * 获取所有问题解决方案（按问题类型分组）
     */
    public Map<String, List<Solution>> getAllSolutions() {
        return solutions;
    }

    /**
     * 获取最佳实践
     */
    public List<BestPractice> getBestPractices(String category) {
        return bestPractices.values().stream()
            .filter(bp -> bp.getCategory().equals(category))
            .toList();
    }

    /**
     * 获取所有最佳实践
     */
    public List<BestPractice> getAllBestPractices() {
        return new ArrayList<>(bestPractices.values());
    }

    /**
     * 获取模板使用统计
     */
    public Map<String, Object> getTemplateStats(String templateId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<TemplateUsageRecord> records = usageRecords.get(templateId);

        stats.put("templateId", templateId);
        stats.put("totalUsage", records != null ? records.size() : 0);
        stats.put("successRate", getTemplateSuccessRate(templateId));
        stats.put("avgDurationMs", getTemplateAvgDuration(templateId));

        if (records != null && !records.isEmpty()) {
            stats.put("lastUsed", records.get(records.size() - 1).getUsedAt());
        }

        return stats;
    }

    /**
     * 获取知识库统计
     */
    public Map<String, Object> getKnowledgeBaseStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalUsageRecords", usageRecords.values().stream().mapToInt(List::size).sum());
        stats.put("totalSolutions", solutions.values().stream().mapToInt(List::size).sum());
        stats.put("totalBestPractices", bestPractices.size());
        stats.put("templatesTracked", usageRecords.size());

        return stats;
    }

    /**
     * 定期清理过期记录（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRecords() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(90);

        usageRecords.values().forEach(records ->
            records.removeIf(r -> r.getUsedAt().isBefore(expireTime))
        );

        log.info("清理过期记录完成");
    }

    /**
     * 从磁盘加载知识库
     */
    private void loadFromDisk() {
        // 加载使用记录
        loadUsageRecords();

        // 加载解决方案
        loadSolutions();

        // 加载最佳实践
        loadBestPractices();
    }

    private void loadUsageRecords() {
        Path recordsFile = Path.of(KB_DIR, "usage-records.json");
        if (Files.exists(recordsFile)) {
            try {
                String content = Files.readString(recordsFile);
                // 简化实现：记录到日志
                log.info("加载使用记录: {}", recordsFile);
            } catch (IOException e) {
                log.error("加载使用记录失败", e);
            }
        }
    }

    private void loadSolutions() {
        Path solutionsDir = Path.of(KB_DIR, "solutions");
        if (Files.exists(solutionsDir)) {
            try {
                Files.list(solutionsDir).forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        log.info("加载解决方案: {}", file.getFileName());
                    } catch (IOException e) {
                        log.error("加载解决方案失败: {}", file, e);
                    }
                });
            } catch (IOException e) {
                log.error("加载解决方案目录失败", e);
            }
        }
    }

    private void loadBestPractices() {
        Path practicesDir = Path.of(KB_DIR, "best-practices");
        if (Files.exists(practicesDir)) {
            try {
                Files.list(practicesDir).forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        log.info("加载最佳实践: {}", file.getFileName());
                    } catch (IOException e) {
                        log.error("加载最佳实践失败: {}", file, e);
                    }
                });
            } catch (IOException e) {
                log.error("加载最佳实践目录失败", e);
            }
        }
    }

    private void saveUsageRecord(TemplateUsageRecord record) {
        try {
            Path recordsFile = Path.of(KB_DIR, "usage-records.json");
            Files.createDirectories(recordsFile.getParent());
            // 追加记录到文件（每行一个记录）
            String line = String.format("%s|%s|%s|%b|%d|%s\n",
                record.getRecordId(), record.getTemplateId(),
                record.getGameDescription().replace("|", "\\|"),
                record.isSuccess(), record.getDurationMs(), record.getUsedAt());
            Files.writeString(recordsFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.debug("保存使用记录: {}", record.getRecordId());
        } catch (Exception e) {
            log.error("保存使用记录失败", e);
        }
    }

    private void saveSolution(Solution solution) {
        try {
            Path solutionsDir = Path.of(KB_DIR, "solutions");
            Files.createDirectories(solutionsDir);
            Path file = solutionsDir.resolve(solution.getId() + ".txt");
            String content = String.format("问题类型: %s\n问题描述: %s\n解决方案: %s\n时间: %s\n",
                solution.getProblemType(), solution.getProblemDescription(),
                solution.getSolution(), solution.getCreatedAt());
            Files.writeString(file, content);
            log.debug("保存解决方案: {}", solution.getId());
        } catch (Exception e) {
            log.error("保存解决方案失败", e);
        }
    }

    private void saveBestPractice(BestPractice practice) {
        try {
            Path practicesDir = Path.of(KB_DIR, "best-practices");
            Files.createDirectories(practicesDir);
            Path file = practicesDir.resolve(practice.getId() + ".txt");
            String content = String.format("分类: %s\n标题: %s\n内容: %s\n时间: %s\n",
                practice.getCategory(), practice.getTitle(),
                practice.getContent(), practice.getCreatedAt());
            Files.writeString(file, content);
            log.debug("保存最佳实践: {}", practice.getId());
        } catch (Exception e) {
            log.error("保存最佳实践失败", e);
        }
    }

    // ===== 内部类 =====

    /**
     * 模板使用记录
     */
    public static class TemplateUsageRecord {
        private final String recordId;
        private final String templateId;
        private final String gameDescription;
        private final boolean success;
        private final long durationMs;
        private final LocalDateTime usedAt;

        public TemplateUsageRecord(String recordId, String templateId, String gameDescription,
                                   boolean success, long durationMs, LocalDateTime usedAt) {
            this.recordId = recordId;
            this.templateId = templateId;
            this.gameDescription = gameDescription;
            this.success = success;
            this.durationMs = durationMs;
            this.usedAt = usedAt;
        }

        public String getRecordId() { return recordId; }
        public String getTemplateId() { return templateId; }
        public String getGameDescription() { return gameDescription; }
        public boolean isSuccess() { return success; }
        public long getDurationMs() { return durationMs; }
        public LocalDateTime getUsedAt() { return usedAt; }
    }

    /**
     * 问题解决方案
     */
    public static class Solution {
        private final String id;
        private final String problemType;
        private final String problemDescription;
        private final String solution;
        private final LocalDateTime createdAt;

        public Solution(String id, String problemType, String problemDescription,
                       String solution, LocalDateTime createdAt) {
            this.id = id;
            this.problemType = problemType;
            this.problemDescription = problemDescription;
            this.solution = solution;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getProblemType() { return problemType; }
        public String getProblemDescription() { return problemDescription; }
        public String getSolution() { return solution; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * 最佳实践
     */
    public static class BestPractice {
        private final String id;
        private final String category;
        private final String title;
        private final String content;
        private final LocalDateTime createdAt;

        public BestPractice(String id, String category, String title,
                           String content, LocalDateTime createdAt) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
