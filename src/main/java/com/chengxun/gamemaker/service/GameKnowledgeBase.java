package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 游戏知识库
 * 存储和管理游戏开发知识，包括：
 * - 模板使用记录和成功率
 * - 常见问题和解决方案（支持去重、精选、归档、精华提取）
 * - 最佳实践和代码片段
 *
 * 解决方案优化策略：
 * 1. 写入去重：同 problemType 下 description 前缀匹配则合并
 * 2. 检索精选：综合评分（质量*0.5 + 时效*0.3 + 引用*0.2）取 Top N
 * 3. 定期合并：同类型相似方案合并，生成精华摘要
 * 4. 过期归档：90天未引用的方案移入 archive 目录
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(GameKnowledgeBase.class);

    /** 知识库存储目录 */
    private static final String KB_DIR = "data/knowledge-base";

    /** 精华摘要目录 */
    private static final String ESSENCES_DIR = KB_DIR + "/essences";

    /** 归档目录 */
    private static final String ARCHIVE_DIR = KB_DIR + "/solutions-archive";

    /** 每个 problemType 最少保留的方案数 */
    private static final int MIN_SOLUTIONS_PER_TYPE = 10;

    /** 过期天数 */
    private static final int EXPIRE_DAYS = 90;

    /** 描述前缀匹配长度（去重用） */
    private static final int DEDUP_PREFIX_LEN = 50;

    /** 模板使用记录 */
    private final Map<String, List<TemplateUsageRecord>> usageRecords = new ConcurrentHashMap<>();

    /** 通用使用记录（模板、解决方案、最佳实践、知识提取、进化等） */
    private final List<UsageRecord> generalUsageRecords = new CopyOnWriteArrayList<>();

    /** 问题解决方案库：problemType -> List<Solution> */
    private final Map<String, List<Solution>> solutions = new ConcurrentHashMap<>();

    /** 最佳实践库 */
    private final Map<String, BestPractice> bestPractices = new ConcurrentHashMap<>();

    /**
     * 初始化知识库
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(KB_DIR));
            Files.createDirectories(Path.of(ESSENCES_DIR));
            Files.createDirectories(Path.of(ARCHIVE_DIR));
            loadFromDisk();
            int total = solutions.values().stream().mapToInt(List::size).sum();
            log.info("游戏知识库初始化完成，共 {} 种问题类型，{} 个解决方案", solutions.size(), total);

            // 启动时执行一次性清理（合并重复 + 归档过期 + 生成精华）
            if (total > 100) {
                log.info("解决方案数量较多({})，启动时自动执行清理...", total);
                new Thread(() -> {
                    try {
                        Thread.sleep(10000); // 等待系统完全启动
                        mergeDuplicateSolutions();
                        archiveExpiredSolutions();
                        generateEssences();
                        int afterTotal = solutions.values().stream().mapToInt(List::size).sum();
                        log.info("启动清理完成，方案数: {} -> {}", total, afterTotal);
                    } catch (Exception e) {
                        log.error("启动清理失败", e);
                    }
                }, "kb-cleanup").start();
            }
        } catch (IOException e) {
            log.error("初始化知识库失败", e);
        }
    }

    // ===== 解决方案核心方法 =====

    /**
     * 记录问题解决方案（带去重）
     * 如果同 problemType 下已有相似描述的方案，则合并而非新建
     *
     * @param problemType 问题类型
     * @param problemDescription 问题描述
     * @param solutionText 解决方案内容
     */
    public void recordSolution(String problemType, String problemDescription, String solutionText) {
        List<Solution> typeSolutions = solutions.computeIfAbsent(problemType, k -> new ArrayList<>());

        // 去重检查：同类型下 description 前缀匹配
        String prefix = problemDescription.length() > DEDUP_PREFIX_LEN
            ? problemDescription.substring(0, DEDUP_PREFIX_LEN) : problemDescription;
        synchronized (typeSolutions) {
            for (Solution existing : typeSolutions) {
                String existPrefix = existing.getProblemDescription().length() > DEDUP_PREFIX_LEN
                    ? existing.getProblemDescription().substring(0, DEDUP_PREFIX_LEN) : existing.getProblemDescription();
                if (prefix.equalsIgnoreCase(existPrefix)) {
                    // 匹配到已有方案，合并更新
                    existing.setSolution(existing.getSolution() + "\n---\n" + solutionText);
                    existing.setUsageCount(existing.getUsageCount() + 1);
                    existing.setLastUsedAt(LocalDateTime.now());
                    saveSolution(existing);
                    log.info("合并已有解决方案: type={}, id={}", problemType, existing.getId());
                    return;
                }
            }

            // 未匹配，创建新方案
            Solution sol = new Solution(
                UUID.randomUUID().toString(),
                problemType,
                problemDescription,
                solutionText,
                LocalDateTime.now()
            );
            typeSolutions.add(sol);
            saveSolution(sol);
            log.info("记录新解决方案: type={}, total={}", problemType, typeSolutions.size());
        }
    }

    /**
     * 获取精选解决方案（综合评分排序）
     * 评分公式：quality * 0.5 + recency * 0.3 + usageCount_norm * 0.2
     * 每次返回时自动更新引用计数
     *
     * @param problemType 问题类型
     * @param limit 最大返回数
     * @return 按综合评分排序的 Top N 方案
     */
    public List<Solution> getTopSolutions(String problemType, int limit) {
        List<Solution> typeSolutions = solutions.getOrDefault(problemType, Collections.emptyList());
        if (typeSolutions.isEmpty()) return Collections.emptyList();

        synchronized (typeSolutions) {
            // 计算该类型下 usageCount 的最大值（用于归一化）
            final int maxUsage = Math.max(1, typeSolutions.stream().mapToInt(Solution::getUsageCount).max().orElse(1));
            final LocalDateTime now = LocalDateTime.now();
            return typeSolutions.stream()
                .sorted((a, b) -> Double.compare(
                    calculateScore(b, maxUsage, now),
                    calculateScore(a, maxUsage, now)))
                .limit(limit)
                .peek(sol -> {
                    sol.setUsageCount(sol.getUsageCount() + 1);
                    sol.setLastUsedAt(now);
                })
                .toList();
        }
    }

    /**
     * 计算方案综合评分
     */
    private double calculateScore(Solution sol, int maxUsage, LocalDateTime now) {
        double quality = sol.getQuality();
        // 时效分：越新越高，30天内满分
        long daysSinceCreated = java.time.Duration.between(sol.getCreatedAt(), now).toDays();
        double recency = Math.max(0, 1.0 - daysSinceCreated / 30.0);
        // 引用分：归一化
        double usageNorm = (double) sol.getUsageCount() / maxUsage;
        return quality * 0.5 + recency * 0.3 + usageNorm * 0.2;
    }

    /**
     * 获取问题解决方案（兼容旧接口）
     */
    public List<Solution> getSolutions(String problemType) {
        return getTopSolutions(problemType, 3);
    }

    /**
     * 获取所有问题解决方案（按问题类型分组）
     */
    public Map<String, List<Solution>> getAllSolutions() {
        return solutions;
    }

    /**
     * 获取所有解决方案列表（按综合评分排序）
     */
    public List<Solution> getAllSolutionsList() {
        final LocalDateTime now = LocalDateTime.now();
        final int maxUsage = solutions.values().stream()
            .flatMap(List::stream).mapToInt(Solution::getUsageCount).max().orElse(1);
        return solutions.values().stream()
            .flatMap(List::stream)
            .sorted((a, b) -> Double.compare(calculateScore(b, maxUsage, now), calculateScore(a, maxUsage, now)))
            .toList();
    }

    // ===== 模板和最佳实践 =====

    /**
     * 记录模板使用
     */
    public void recordTemplateUsage(String templateId, String gameDescription, boolean success, long durationMs) {
        TemplateUsageRecord record = new TemplateUsageRecord(
            UUID.randomUUID().toString(), templateId, gameDescription, success, durationMs, LocalDateTime.now());
        usageRecords.computeIfAbsent(templateId, k -> new ArrayList<>()).add(record);
        saveUsageRecord(record);
        log.info("记录模板使用: template={}, success={}, duration={}ms", templateId, success, durationMs);
    }

    /**
     * 记录最佳实践
     */
    public void recordBestPractice(String category, String title, String content) {
        BestPractice practice = new BestPractice(
            UUID.randomUUID().toString(), category, title, content, LocalDateTime.now());
        bestPractices.put(practice.getId(), practice);
        saveBestPractice(practice);
        log.info("记录最佳实践: category={}, title={}", category, title);
    }

    /**
     * 获取模板成功率
     */
    public double getTemplateSuccessRate(String templateId) {
        List<TemplateUsageRecord> records = usageRecords.get(templateId);
        if (records == null || records.isEmpty()) return 0.0;
        return (double) records.stream().filter(TemplateUsageRecord::isSuccess).count() / records.size() * 100;
    }

    /**
     * 获取模板平均生成时间
     */
    public long getTemplateAvgDuration(String templateId) {
        List<TemplateUsageRecord> records = usageRecords.get(templateId);
        if (records == null || records.isEmpty()) return 0;
        return (long) records.stream().mapToLong(TemplateUsageRecord::getDurationMs).average().orElse(0);
    }

    /**
     * 获取最佳实践
     */
    public List<BestPractice> getBestPractices(String category) {
        return bestPractices.values().stream().filter(bp -> bp.getCategory().equals(category)).toList();
    }

    /**
     * 获取所有最佳实践
     */
    public List<BestPractice> getAllBestPractices() {
        return new ArrayList<>(bestPractices.values());
    }

    /**
     * 获取所有使用记录列表
     */
    public List<TemplateUsageRecord> getAllUsageRecords() {
        return usageRecords.values().stream().flatMap(List::stream)
            .sorted((a, b) -> b.getUsedAt().compareTo(a.getUsedAt())).toList();
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
        int templateUsageCount = usageRecords.values().stream().mapToInt(List::size).sum();
        int generalUsageCount = generalUsageRecords.size();
        stats.put("totalUsageRecords", templateUsageCount + generalUsageCount);
        stats.put("totalSolutions", solutions.values().stream().mapToInt(List::size).sum());
        stats.put("totalBestPractices", bestPractices.size());
        stats.put("templatesTracked", usageRecords.size());
        return stats;
    }

    // ===== 定期维护 =====

    /**
     * 定期清理过期记录（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRecords() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(EXPIRE_DAYS);
        usageRecords.values().forEach(records ->
            records.removeIf(r -> r.getUsedAt().isBefore(expireTime))
        );
        log.info("清理过期使用记录完成");
    }

    /**
     * 合并同类型下的重复方案（每天凌晨2:30执行）
     * 相似度判断：description 前 50 字符相同视为重复
     * 合并规则：保留 quality 较高的，将其他方案的 solution 追加
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void mergeDuplicateSolutions() {
        log.info("开始合并重复解决方案...");
        int mergedCount = 0;

        for (Map.Entry<String, List<Solution>> entry : solutions.entrySet()) {
            List<Solution> typeSolutions = entry.getValue();
            synchronized (typeSolutions) {
                Map<String, List<Solution>> groups = typeSolutions.stream()
                    .collect(Collectors.groupingBy(s -> {
                        String desc = s.getProblemDescription();
                        return desc.length() > DEDUP_PREFIX_LEN
                            ? desc.substring(0, DEDUP_PREFIX_LEN).toLowerCase()
                            : desc.toLowerCase();
                    }));

                for (Map.Entry<String, List<Solution>> group : groups.entrySet()) {
                    if (group.getValue().size() <= 1) continue;

                    // 按 quality 降序，保留第一个（最高质量）
                    List<Solution> dupes = group.getValue().stream()
                        .sorted((a, b) -> Double.compare(b.getQuality(), a.getQuality()))
                        .toList();
                    Solution keeper = dupes.get(0);

                    // 将其他方案的精华追加到 keeper
                    for (int i = 1; i < dupes.size(); i++) {
                        Solution dupe = dupes.get(i);
                        String extraSolution = dupe.getSolution();
                        if (!keeper.getSolution().contains(extraSolution.substring(0, Math.min(50, extraSolution.length())))) {
                            keeper.setSolution(keeper.getSolution() + "\n---\n" + extraSolution);
                        }
                        keeper.setUsageCount(keeper.getUsageCount() + dupe.getUsageCount());
                        typeSolutions.remove(dupe);
                        deleteSolutionFile(dupe.getId());
                        mergedCount++;
                    }
                    saveSolution(keeper);
                }
            }
        }
        log.info("合并重复解决方案完成，合并了 {} 个方案", mergedCount);
    }

    /**
     * 过期归档（每天凌晨2:45执行）
     * 90天未被引用且 createdAt 超过 90 天的方案移入 archive
     * 每个 problemType 至少保留 MIN_SOLUTIONS_PER_TYPE 个方案
     */
    @Scheduled(cron = "0 45 2 * * ?")
    public void archiveExpiredSolutions() {
        log.info("开始归档过期解决方案...");
        int archivedCount = 0;
        LocalDateTime expireTime = LocalDateTime.now().minusDays(EXPIRE_DAYS);

        for (Map.Entry<String, List<Solution>> entry : solutions.entrySet()) {
            List<Solution> typeSolutions = entry.getValue();
            synchronized (typeSolutions) {
                if (typeSolutions.size() <= MIN_SOLUTIONS_PER_TYPE) continue;

                List<Solution> toArchive = new ArrayList<>();
                for (Solution sol : typeSolutions) {
                    boolean isExpired = sol.getCreatedAt().isBefore(expireTime)
                        && (sol.getLastUsedAt() == null || sol.getLastUsedAt().isBefore(expireTime));
                    if (isExpired) toArchive.add(sol);
                }

                // 确保至少保留 MIN_SOLUTIONS_PER_TYPE 个
                int maxCanArchive = typeSolutions.size() - MIN_SOLUTIONS_PER_TYPE;
                if (toArchive.size() > maxCanArchive) {
                    // 按 quality 升序，归档质量最低的
                    toArchive.sort(Comparator.comparingDouble(Solution::getQuality));
                    toArchive = toArchive.subList(0, maxCanArchive);
                }

                for (Solution sol : toArchive) {
                    archiveSolution(sol);
                    typeSolutions.remove(sol);
                    deleteSolutionFile(sol.getId());
                    archivedCount++;
                }
            }
        }
        log.info("归档过期解决方案完成，归档了 {} 个方案", archivedCount);
    }

    /**
     * 生成精华摘要（每天凌晨3:15执行）
     * 对每个 problemType 生成精华文件，包含 Top 10 方案的浓缩版
     */
    @Scheduled(cron = "0 15 3 * * ?")
    public void generateEssences() {
        log.info("开始生成精华摘要...");
        int count = 0;

        for (Map.Entry<String, List<Solution>> entry : solutions.entrySet()) {
            String problemType = entry.getKey();
            List<Solution> topSolutions = getTopSolutions(problemType, 10);
            if (topSolutions.isEmpty()) continue;

            StringBuilder essence = new StringBuilder();
            essence.append("# ").append(problemType).append(" 精华摘要\n\n");
            essence.append("自动生成于: ").append(LocalDateTime.now()).append("\n");
            essence.append("方案总数: ").append(entry.getValue().size()).append("\n\n");

            for (int i = 0; i < topSolutions.size(); i++) {
                Solution sol = topSolutions.get(i);
                essence.append("## ").append(i + 1).append(". ").append(sol.getProblemDescription()).append("\n");
                // 浓缩：取 solution 前 200 字符
                String condensed = sol.getSolution().length() > 200
                    ? sol.getSolution().substring(0, 200) + "..." : sol.getSolution();
                essence.append(condensed).append("\n\n");
            }

            try {
                Path essenceFile = Path.of(ESSENCES_DIR, problemType + ".md");
                Files.writeString(essenceFile, essence.toString());
                count++;
            } catch (IOException e) {
                log.error("生成精华文件失败: {}", problemType, e);
            }
        }
        log.info("生成精华摘要完成，共 {} 个类型", count);
    }

    /**
     * 获取精华摘要内容
     *
     * @param problemType 问题类型
     * @return 精华摘要文本，不存在返回 null
     */
    public String getEssence(String problemType) {
        Path essenceFile = Path.of(ESSENCES_DIR, problemType + ".md");
        if (!Files.exists(essenceFile)) return null;
        try {
            return Files.readString(essenceFile);
        } catch (IOException e) {
            log.warn("读取精华文件失败: {}", problemType);
            return null;
        }
    }

    /**
     * 获取所有精华摘要
     *
     * @return problemType -> essence content
     */
    public Map<String, String> getAllEssences() {
        Map<String, String> essences = new LinkedHashMap<>();
        try {
            Path dir = Path.of(ESSENCES_DIR);
            if (!Files.exists(dir)) return essences;
            Files.list(dir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(p -> {
                    String type = p.getFileName().toString().replace(".md", "");
                    try {
                        essences.put(type, Files.readString(p));
                    } catch (IOException ignored) {}
                });
        } catch (IOException e) {
            log.warn("读取精华目录失败", e);
        }
        return essences;
    }

    // ===== 内部方法 =====

    /**
     * 归档单个方案
     */
    private void archiveSolution(Solution sol) {
        try {
            Path archiveDir = Path.of(ARCHIVE_DIR, sol.getProblemType());
            Files.createDirectories(archiveDir);
            Path file = archiveDir.resolve(sol.getId() + ".txt");
            String content = String.format(
                "问题类型: %s\n问题描述: %s\n解决方案: %s\n创建时间: %s\n最后引用: %s\n引用次数: %d\n质量评分: %.2f\n归档时间: %s\n",
                sol.getProblemType(), sol.getProblemDescription(), sol.getSolution(),
                sol.getCreatedAt(), sol.getLastUsedAt(), sol.getUsageCount(),
                sol.getQuality(), LocalDateTime.now());
            Files.writeString(file, content);
            log.debug("归档解决方案: {}/{}", sol.getProblemType(), sol.getId());
        } catch (IOException e) {
            log.error("归档解决方案失败: {}", sol.getId(), e);
        }
    }

    /**
     * 删除解决方案文件
     */
    private void deleteSolutionFile(String solutionId) {
        try {
            Path file = Path.of(KB_DIR, "solutions", solutionId + ".txt");
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("删除解决方案文件失败: {}", solutionId);
        }
    }

    /**
     * 从磁盘加载知识库
     */
    private void loadFromDisk() {
        loadUsageRecords();
        loadSolutions();
        loadBestPractices();
    }

    private void loadUsageRecords() {
        Path recordsFile = Path.of(KB_DIR, "usage-records.json");
        if (Files.exists(recordsFile)) {
            try {
                Files.readString(recordsFile);
                log.info("加载使用记录: {}", recordsFile);
            } catch (IOException e) {
                log.error("加载使用记录失败", e);
            }
        }
    }

    /**
     * 加载解决方案文件
     * 支持新格式（含元数据）和旧格式（纯文本）
     */
    private void loadSolutions() {
        Path solutionsDir = Path.of(KB_DIR, "solutions");
        if (!Files.exists(solutionsDir)) return;

        try {
            Files.list(solutionsDir).forEach(file -> {
                try {
                    String content = Files.readString(file);
                    Solution sol = parseSolutionFile(file, content);
                    if (sol != null) {
                        solutions.computeIfAbsent(sol.getProblemType(), k -> new ArrayList<>()).add(sol);
                    }
                } catch (IOException e) {
                    log.error("加载解决方案失败: {}", file, e);
                }
            });
            int total = solutions.values().stream().mapToInt(List::size).sum();
            log.info("加载了 {} 个解决方案", total);
        } catch (IOException e) {
            log.error("加载解决方案目录失败", e);
        }
    }

    /**
     * 解析解决方案文件
     * 新格式包含 usageCount/lastUsedAt/quality 元数据行
     */
    private Solution parseSolutionFile(Path file, String content) {
        try {
            String id = file.getFileName().toString().replace(".txt", "");
            String problemType = extractField(content, "问题类型");
            String problemDesc = extractField(content, "问题描述");
            String solutionText = extractField(content, "解决方案");
            if (problemType == null || problemDesc == null) return null;

            int usageCount = 0;
            LocalDateTime lastUsedAt = null;
            double quality = 0.5;
            LocalDateTime createdAt = LocalDateTime.now();

            String usageStr = extractField(content, "引用次数");
            if (usageStr != null) {
                try { usageCount = Integer.parseInt(usageStr.trim()); } catch (NumberFormatException ignored) {}
            }
            String lastUsedStr = extractField(content, "最后引用");
            if (lastUsedStr != null && !lastUsedStr.equals("null")) {
                try { lastUsedAt = LocalDateTime.parse(lastUsedStr.trim()); } catch (DateTimeParseException ignored) {}
            }
            String qualityStr = extractField(content, "质量评分");
            if (qualityStr != null) {
                try { quality = Double.parseDouble(qualityStr.trim()); } catch (NumberFormatException ignored) {}
            }
            String timeStr = extractField(content, "时间");
            if (timeStr == null) timeStr = extractField(content, "创建时间");
            if (timeStr != null) {
                try { createdAt = LocalDateTime.parse(timeStr.trim()); } catch (DateTimeParseException ignored) {}
            }

            return new Solution(id, problemType, problemDesc, solutionText, createdAt,
                               usageCount, lastUsedAt, quality);
        } catch (Exception e) {
            log.warn("解析解决方案文件失败: {}", file.getFileName());
            return null;
        }
    }

    /**
     * 从文件内容中提取字段值
     */
    private String extractField(String content, String fieldName) {
        for (String line : content.split("\n")) {
            if (line.startsWith(fieldName + ":")) {
                return line.substring(fieldName.length() + 1).trim();
            }
        }
        return null;
    }

    private void loadBestPractices() {
        Path practicesDir = Path.of(KB_DIR, "best-practices");
        if (!Files.exists(practicesDir)) return;
        try {
            Files.list(practicesDir).forEach(file -> {
                try {
                    Files.readString(file);
                } catch (IOException e) {
                    log.error("加载最佳实践失败: {}", file, e);
                }
            });
        } catch (IOException e) {
            log.error("加载最佳实践目录失败", e);
        }
    }

    private void saveUsageRecord(TemplateUsageRecord record) {
        try {
            Path recordsFile = Path.of(KB_DIR, "usage-records.json");
            Files.createDirectories(recordsFile.getParent());
            String line = String.format("%s|%s|%s|%b|%d|%s\n",
                record.getRecordId(), record.getTemplateId(),
                record.getGameDescription().replace("|", "\\|"),
                record.isSuccess(), record.getDurationMs(), record.getUsedAt());
            Files.writeString(recordsFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("保存使用记录失败", e);
        }
    }

    /**
     * 保存解决方案到磁盘（含元数据）
     */
    private void saveSolution(Solution solution) {
        try {
            Path solutionsDir = Path.of(KB_DIR, "solutions");
            Files.createDirectories(solutionsDir);
            Path file = solutionsDir.resolve(solution.getId() + ".txt");
            String content = String.format(
                "问题类型: %s\n问题描述: %s\n解决方案: %s\n创建时间: %s\n引用次数: %d\n最后引用: %s\n质量评分: %.2f\n",
                solution.getProblemType(), solution.getProblemDescription(),
                solution.getSolution(), solution.getCreatedAt(),
                solution.getUsageCount(),
                solution.getLastUsedAt() != null ? solution.getLastUsedAt().toString() : "null",
                solution.getQuality());
            Files.writeString(file, content);
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
        } catch (Exception e) {
            log.error("保存最佳实践失败", e);
        }
    }

    // ===== 内部类 =====

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
     * 通用使用记录类型
     */
    public enum UsageRecordType {
        TEMPLATE,           // 模板使用
        SOLUTION,           // 解决方案应用
        BEST_PRACTICE,      // 最佳实践引用
        KNOWLEDGE_EXTRACTION, // 知识提取
        EVOLUTION,          // 自进化
        GAME_LEARNING       // 游戏学习
    }

    /**
     * 通用使用记录
     * 记录知识库中各类资源的使用情况
     */
    public static class UsageRecord {
        private final String recordId;
        private final UsageRecordType type;
        private final String resourceId;      // 关联的资源 ID（模板ID、方案ID等）
        private final String resourceName;    // 资源名称（用于展示）
        private final String description;     // 使用描述
        private final boolean success;
        private final LocalDateTime usedAt;
        private final String metadata;        // 额外元数据（JSON）

        public UsageRecord(String recordId, UsageRecordType type, String resourceId,
                          String resourceName, String description, boolean success,
                          LocalDateTime usedAt, String metadata) {
            this.recordId = recordId;
            this.type = type;
            this.resourceId = resourceId;
            this.resourceName = resourceName;
            this.description = description;
            this.success = success;
            this.usedAt = usedAt;
            this.metadata = metadata;
        }

        public String getRecordId() { return recordId; }
        public UsageRecordType getType() { return type; }
        public String getResourceId() { return resourceId; }
        public String getResourceName() { return resourceName; }
        public String getDescription() { return description; }
        public boolean isSuccess() { return success; }
        public LocalDateTime getUsedAt() { return usedAt; }
        public String getMetadata() { return metadata; }
    }

    /**
     * 记录解决方案使用
     *
     * @param problemType 问题类型
     * @param problemDescription 问题描述
     * @param applied 是否成功应用
     */
    public void recordSolutionUsage(String problemType, String problemDescription, boolean applied) {
        UsageRecord record = new UsageRecord(
            UUID.randomUUID().toString(),
            UsageRecordType.SOLUTION,
            problemType,
            problemType,
            problemDescription,
            applied,
            LocalDateTime.now(),
            null
        );
        generalUsageRecords.add(record);
        trimGeneralUsageRecords();
        log.info("记录解决方案使用: type={}, applied={}", problemType, applied);
    }

    /**
     * 记录最佳实践引用
     *
     * @param category 类别
     * @param title 标题
     * @param referenced 是否成功引用
     */
    public void recordBestPracticeUsage(String category, String title, boolean referenced) {
        UsageRecord record = new UsageRecord(
            UUID.randomUUID().toString(),
            UsageRecordType.BEST_PRACTICE,
            category,
            title,
            "引用最佳实践: " + title,
            referenced,
            LocalDateTime.now(),
            null
        );
        generalUsageRecords.add(record);
        trimGeneralUsageRecords();
        log.info("记录最佳实践引用: category={}, title={}", category, title);
    }

    /**
     * 记录知识提取事件
     *
     * @param agentId Agent ID
     * @param projectId 项目 ID
     * @param extractedCount 提取数量
     * @param savedCount 保存数量
     */
    public void recordKnowledgeExtraction(String agentId, String projectId, int extractedCount, int savedCount) {
        UsageRecord record = new UsageRecord(
            UUID.randomUUID().toString(),
            UsageRecordType.KNOWLEDGE_EXTRACTION,
            projectId,
            "知识提取",
            String.format("从 %s 提取 %d 条知识，保存 %d 条", agentId, extractedCount, savedCount),
            savedCount > 0,
            LocalDateTime.now(),
            String.format("{\"extractedCount\":%d,\"savedCount\":%d}", extractedCount, savedCount)
        );
        generalUsageRecords.add(record);
        trimGeneralUsageRecords();
        log.info("记录知识提取: agent={}, project={}, saved={}", agentId, projectId, savedCount);
    }

    /**
     * 记录自进化事件
     *
     * @param evolutionType 进化类型
     * @param description 描述
     * @param success 是否成功
     */
    public void recordEvolution(String evolutionType, String description, boolean success) {
        UsageRecord record = new UsageRecord(
            UUID.randomUUID().toString(),
            UsageRecordType.EVOLUTION,
            evolutionType,
            "自进化",
            description,
            success,
            LocalDateTime.now(),
            null
        );
        generalUsageRecords.add(record);
        trimGeneralUsageRecords();
        log.info("记录进化事件: type={}, success={}", evolutionType, success);
    }

    /**
     * 记录游戏学习事件
     *
     * @param projectId 项目 ID
     * @param gameDescription 游戏描述
     * @param success 是否成功
     */
    public void recordGameLearning(String projectId, String gameDescription, boolean success) {
        UsageRecord record = new UsageRecord(
            UUID.randomUUID().toString(),
            UsageRecordType.GAME_LEARNING,
            projectId,
            "游戏学习",
            gameDescription,
            success,
            LocalDateTime.now(),
            null
        );
        generalUsageRecords.add(record);
        trimGeneralUsageRecords();
        log.info("记录游戏学习: project={}, success={}", projectId, success);
    }

    /**
     * 获取所有通用使用记录
     */
    public List<UsageRecord> getAllGeneralUsageRecords() {
        return generalUsageRecords.stream()
            .sorted((a, b) -> b.getUsedAt().compareTo(a.getUsedAt()))
            .toList();
    }

    /**
     * 裁剪通用使用记录，保持在限制内
     */
    private void trimGeneralUsageRecords() {
        while (generalUsageRecords.size() > 1000) {
            generalUsageRecords.remove(0);
        }
    }

    /**
     * 问题解决方案（可变，支持引用计数和质量评分）
     */
    public static class Solution {
        private final String id;
        private final String problemType;
        private final String problemDescription;
        private String solution;
        private final LocalDateTime createdAt;
        private int usageCount;
        private LocalDateTime lastUsedAt;
        private double quality;

        public Solution(String id, String problemType, String problemDescription,
                       String solution, LocalDateTime createdAt) {
            this(id, problemType, problemDescription, solution, createdAt, 0, null, 0.5);
        }

        public Solution(String id, String problemType, String problemDescription,
                       String solution, LocalDateTime createdAt,
                       int usageCount, LocalDateTime lastUsedAt, double quality) {
            this.id = id;
            this.problemType = problemType;
            this.problemDescription = problemDescription;
            this.solution = solution;
            this.createdAt = createdAt;
            this.usageCount = usageCount;
            this.lastUsedAt = lastUsedAt;
            this.quality = quality;
        }

        public String getId() { return id; }
        public String getProblemType() { return problemType; }
        public String getProblemDescription() { return problemDescription; }
        public String getSolution() { return solution; }
        public void setSolution(String solution) { this.solution = solution; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public int getUsageCount() { return usageCount; }
        public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
        public double getQuality() { return quality; }
        public void setQuality(double quality) { this.quality = quality; }
    }

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
