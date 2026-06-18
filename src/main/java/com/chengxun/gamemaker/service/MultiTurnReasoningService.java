package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.MultiTurnRecord;
import com.chengxun.gamemaker.web.repository.MultiTurnRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮推理服务
 * 实现 Think → Plan → Act → Verify 循环，提升 Agent 决策质量
 *
 * 工作流程：
 * 1. Think：分析任务，理解需求和约束
 * 2. Plan：制定执行计划，分解步骤
 * 3. Act：执行计划
 * 4. Verify：验证执行结果，判断是否通过
 * 5. 如果不通过且未超过最大轮次，回到 Think 重新分析
 *
 * 设计特点：
 * - 异步执行：reason() 立即返回记录ID，后台线程执行推理
 * - 每步持久化：每个阶段完成后立即保存到数据库
 * - 前端轮询：通过 getStatus() 获取实时进度
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class MultiTurnReasoningService {

    private static final Logger log = LoggerFactory.getLogger(MultiTurnReasoningService.class);

    /** 默认最大推理轮次 */
    private static final int DEFAULT_MAX_TURNS = 3;

    @Autowired
    private MultiTurnRecordRepository recordRepository;

    @Autowired
    private AgentManager agentManager;

    @Autowired(required = false)
    private ClaudeCliEngine cliEngine;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ClaudeAiService aiService;

    @Autowired
    private com.chengxun.gamemaker.manager.ProjectManager projectManager;

    /** 正在执行的推理任务 */
    private final Map<Long, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 触发多轮推理（异步）
     * 立即返回记录，后台线程执行推理过程
     *
     * @param agentId Agent ID
     * @param projectId 项目 ID
     * @param taskId 任务 ID
     * @param taskDescription 任务描述
     * @return 推理记录（状态为 THINKING）
     */
    public MultiTurnRecord reason(String agentId, String projectId, String taskId, String taskDescription) {
        int maxTurns = DEFAULT_MAX_TURNS;
        MultiTurnRecord record = new MultiTurnRecord();
        record.setAgentId(agentId);
        record.setProjectId(projectId);
        record.setTaskId(taskId);
        record.setTaskDescription(taskDescription);
        record.setMaxTurns(maxTurns);
        record.setStatus(MultiTurnRecord.ReasoningStatus.THINKING);
        record = recordRepository.save(record);

        final long recordId = record.getId();
        final MultiTurnRecord savedRecord = record;

        // 异步执行推理
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeReasoning(savedRecord);
            } catch (Exception e) {
                log.error("多轮推理异常: recordId={}", recordId, e);
                savedRecord.setStatus(MultiTurnRecord.ReasoningStatus.FAILED);
                savedRecord.setVerifyResult("推理异常: " + e.getMessage());
                recordRepository.save(savedRecord);
            } finally {
                runningTasks.remove(recordId);
            }
        });
        runningTasks.put(recordId, future);

        log.info("多轮推理已启动: recordId={}, agent={}, projectId={}", recordId, agentId, projectId);
        return record;
    }

    /**
     * 获取推理状态（供前端轮询）
     *
     * @param recordId 记录 ID
     * @return 最新记录
     */
    public MultiTurnRecord getStatus(Long recordId) {
        return recordRepository.findById(recordId).orElse(null);
    }

    /**
     * 检查推理是否仍在执行
     */
    public boolean isRunning(Long recordId) {
        return runningTasks.containsKey(recordId);
    }

    /**
     * 执行推理循环（后台线程）
     * 每个步骤完成后立即持久化到数据库
     */
    private void executeReasoning(MultiTurnRecord record) {
        long startTime = System.currentTimeMillis();
        int maxTurns = record.getMaxTurns();

        for (int turn = 1; turn <= maxTurns; turn++) {
            record.setTurnNumber(turn);
            log.info("多轮推理: recordId={}, turn={}/{}", record.getId(), turn, maxTurns);

            // 1. Think
            record.setStatus(MultiTurnRecord.ReasoningStatus.THINKING);
            record.setThinkResult(doThink(record));
            recordRepository.save(record);

            // 2. Plan
            record.setStatus(MultiTurnRecord.ReasoningStatus.PLANNING);
            record.setPlanResult(doPlan(record));
            recordRepository.save(record);

            // 3. Act
            record.setStatus(MultiTurnRecord.ReasoningStatus.EXECUTING);
            record.setActResult(doAct(record));
            recordRepository.save(record);

            // 4. Verify
            record.setStatus(MultiTurnRecord.ReasoningStatus.VERIFYING);
            record.setVerifyResult(doVerify(record));
            boolean passed = checkVerifyPassed(record.getVerifyResult());
            record.setVerifyPassed(passed);
            recordRepository.save(record);

            if (passed) {
                record.setStatus(MultiTurnRecord.ReasoningStatus.PASSED);
                record.setDurationMs(System.currentTimeMillis() - startTime);
                recordRepository.save(record);
                log.info("多轮推理通过: recordId={}, turn={}", record.getId(), turn);
                return;
            }
            log.info("多轮推理未通过: recordId={}, turn={}, 继续下一轮", record.getId(), turn);
        }

        record.setStatus(MultiTurnRecord.ReasoningStatus.MAX_TURNS);
        record.setDurationMs(System.currentTimeMillis() - startTime);
        recordRepository.save(record);
        log.info("多轮推理达到最大轮次: recordId={}", record.getId());
    }

    /**
     * 获取推理统计
     */
    public Map<String, Object> getStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        long total = recordRepository.countByProject(projectId);
        long passed = recordRepository.countPassedByProject(projectId);
        Double avgTurns = recordRepository.avgTurnsByProject(projectId);

        stats.put("total", total);
        stats.put("passed", passed);
        stats.put("failed", total - passed);
        stats.put("passRate", total > 0 ? Math.round((double) passed / total * 1000) / 10.0 : 0);
        stats.put("avgTurns", avgTurns != null ? Math.round(avgTurns * 10) / 10.0 : 0);
        return stats;
    }

    /**
     * 获取推理历史
     */
    public List<MultiTurnRecord> getHistory(String projectId) {
        return recordRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // ===== 内部方法 =====

    private String doThink(MultiTurnRecord record) {
        String prompt = String.format(
            "你是游戏开发项目的AI助手。请分析以下任务：\n\n任务描述：%s\n\n" +
            "请从以下角度思考：\n1. 任务的核心目标是什么\n2. 有哪些约束条件\n3. 需要哪些资源\n4. 潜在的风险点\n\n" +
            "输出格式：简明的分析摘要（不超过300字）",
            truncate(record.getTaskDescription(), 500));
        return callAi(prompt);
    }

    private String doPlan(MultiTurnRecord record) {
        String prompt = String.format(
            "基于以下分析，请制定执行计划：\n\n分析结果：%s\n\n" +
            "请制定：\n1. 具体执行步骤（最多5步）\n2. 每步的预期产出\n3. 验证标准\n\n" +
            "输出格式：步骤列表（不超过300字）",
            truncate(record.getThinkResult(), 500));
        return callAi(prompt);
    }

    private String doAct(MultiTurnRecord record) {
        // Act 阶段返回计划摘要，实际执行由 Agent 的 doWork 完成
        return "计划已制定，等待 Agent 执行: " + truncate(record.getPlanResult(), 200);
    }

    private String doVerify(MultiTurnRecord record) {
        // 【优化2】先检查实际产出物，再用 AI 判断质量
        String actualOutput = checkActualOutputs(record);

        String prompt = String.format(
            "请验证以下执行结果是否符合预期：\n\n任务：%s\n计划：%s\n执行结果：%s\n\n实际产出检查：\n%s\n\n" +
            "判断标准：\n1. 是否完成了核心目标\n2. 是否符合约束条件\n3. 实际产出是否存在且有效\n4. 质量是否达标\n\n" +
            "输出格式：PASS 或 FAIL + 原因（不超过200字）",
            truncate(record.getTaskDescription(), 200),
            truncate(record.getPlanResult(), 200),
            truncate(record.getActResult(), 200),
            actualOutput);
        return callAi(prompt);
    }

    /**
     * 检查项目的实际产出物
     * 扫描项目目录中的代码文件，检查文件数量、大小、是否为空等
     */
    private String checkActualOutputs(MultiTurnRecord record) {
        if (record.getProjectId() == null) return "无项目信息，跳过产出检查";

        com.chengxun.gamemaker.model.GameProject project = projectManager.getProject(record.getProjectId());
        if (project == null) return "项目不存在";

        String workDir = project.getWorkDir();
        if (workDir == null || workDir.isEmpty()) return "未设置工作目录";

        File dir = new File(workDir);
        if (!dir.exists() || !dir.isDirectory()) return "工作目录不存在: " + workDir;

        StringBuilder sb = new StringBuilder();
        String[] codeExts = {".java", ".py", ".js", ".ts", ".vue", ".lua", ".gd", ".cs", ".cpp", ".go", ".rs"};
        int totalFiles = 0;
        int codeFiles = 0;
        int emptyFiles = 0;
        long totalSize = 0;

        List<File> allFiles = new ArrayList<>();
        collectFiles(dir, allFiles, 0, 5);
        for (File f : allFiles) {
            totalFiles++;
            totalSize += f.length();
            if (f.length() == 0) emptyFiles++;
            String name = f.getName().toLowerCase();
            for (String ext : codeExts) {
                if (name.endsWith(ext)) { codeFiles++; break; }
            }
        }

        sb.append(String.format("- 总文件数: %d\n- 代码文件: %d\n- 空文件: %d\n- 总大小: %.1f KB",
            totalFiles, codeFiles, emptyFiles, totalSize / 1024.0));

        if (codeFiles == 0) {
            sb.append("\n⚠️ 未发现代码文件，任务可能未实际执行");
        }
        if (emptyFiles > 0) {
            sb.append(String.format("\n⚠️ 存在 %d 个空文件，可能是占位符", emptyFiles));
        }
        return sb.toString();
    }

    private void collectFiles(File dir, List<File> result, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) result.add(f);
            else if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("node_modules")
                     && !f.getName().equals("target") && !f.getName().equals("build")) {
                collectFiles(f, result, depth + 1, maxDepth);
            }
        }
    }

    private boolean checkVerifyPassed(String verifyResult) {
        if (verifyResult == null) return false;
        String upper = verifyResult.toUpperCase();
        return upper.contains("PASS") && !upper.contains("FAIL");
    }

    private String callAi(String prompt) {
        if (aiService != null) {
            try {
                return aiService.sendMessage(prompt);
            } catch (Exception e) {
                log.warn("AI调用失败: {}", e.getMessage());
            }
        }
        return "AI服务不可用，跳过此步骤";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
