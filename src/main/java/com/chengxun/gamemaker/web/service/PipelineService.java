package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.Pipeline;
import com.chengxun.gamemaker.web.entity.PipelineStage;
import com.chengxun.gamemaker.web.repository.PipelineRepository;
import com.chengxun.gamemaker.web.repository.PipelineStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * CICD流水线服务
 * 负责流水线的创建、执行和管理
 *
 * 主要功能：
 * - 创建和配置流水线
 * - 执行流水线（支持手动触发）
 * - 管理流水线阶段
 * - 记录执行日志
 * - Agent协作执行
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private PipelineStageRepository stageRepository;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    /** 流水线执行线程池 */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /** 运行中的流水线 */
    private final Map<Long, Future<?>> runningPipelines = new ConcurrentHashMap<>();

    /**
     * 应用关闭时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down pipeline executor service...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Pipeline executor service shut down");
    }

    /**
     * 创建流水线
     *
     * @param name 流水线名称
     * @param description 描述
     * @param projectId 项目ID
     * @param pipelineType 流水线类型
     * @param config 配置
     * @return 创建的流水线
     */
    public Pipeline createPipeline(String name, String description, String projectId,
                                   String pipelineType, String config) {
        // 获取项目信息
        GameProject project = projectManager.getProject(projectId);
        String projectName = project != null ? project.getName() : projectId;

        // 生成流水线编号
        String pipelineNo = generatePipelineNo();

        // 创建流水线
        Pipeline pipeline = new Pipeline();
        pipeline.setPipelineNo(pipelineNo);
        pipeline.setName(name);
        pipeline.setDescription(description);
        pipeline.setProjectId(projectId);
        pipeline.setProjectName(projectName);
        pipeline.setPipelineType(pipelineType);
        pipeline.setConfig(config);
        pipeline.setStatus(Pipeline.Status.IDLE.name());

        // 生产环境部署需要审批
        if ("DEPLOY".equals(pipelineType) || "FULL".equals(pipelineType)) {
            pipeline.setRequiresApproval(true);
            pipeline.setProductionDeploy(true);
        }

        Pipeline saved = pipelineRepository.save(pipeline);

        // 创建默认阶段
        createDefaultStages(saved, pipelineType);

        log.info("Pipeline created: {} ({})", name, pipelineNo);

        return saved;
    }

    /**
     * 创建带自动触发配置的流水线
     */
    public Pipeline createPipelineWithAutoTrigger(String name, String description, String projectId,
                                                   String pipelineType, String config,
                                                   String autoTriggerConfig) {
        Pipeline pipeline = createPipeline(name, description, projectId, pipelineType, config);
        pipeline.setAutoTriggerConfig(autoTriggerConfig);
        return pipelineRepository.save(pipeline);
    }

    /**
     * 创建默认阶段
     */
    private void createDefaultStages(Pipeline pipeline, String pipelineType) {
        List<PipelineStage> stages = new ArrayList<>();

        switch (pipelineType) {
            case "BUILD" -> {
                stages.add(createStage(pipeline.getId(), "代码拉取", "拉取最新代码", "CHECKOUT", 1));
                stages.add(createStage(pipeline.getId(), "代码编译", "编译项目代码", "BUILD", 2));
            }
            case "TEST" -> {
                stages.add(createStage(pipeline.getId(), "代码拉取", "拉取最新代码", "CHECKOUT", 1));
                stages.add(createStage(pipeline.getId(), "单元测试", "运行单元测试", "UNIT_TEST", 2));
                stages.add(createStage(pipeline.getId(), "代码检查", "代码风格和质量检查", "LINT", 3));
            }
            case "DEPLOY" -> {
                stages.add(createStage(pipeline.getId(), "代码拉取", "拉取最新代码", "CHECKOUT", 1));
                stages.add(createStage(pipeline.getId(), "构建打包", "构建项目", "BUILD", 2));
                stages.add(createStage(pipeline.getId(), "部署", "部署到目标环境", "DEPLOY", 3));
            }
            case "FULL" -> {
                stages.add(createStage(pipeline.getId(), "代码拉取", "拉取最新代码", "CHECKOUT", 1));
                stages.add(createStage(pipeline.getId(), "代码检查", "代码风格和质量检查", "LINT", 2));
                stages.add(createStage(pipeline.getId(), "单元测试", "运行单元测试", "UNIT_TEST", 3));
                stages.add(createStage(pipeline.getId(), "构建打包", "构建项目", "BUILD", 4));
                stages.add(createStage(pipeline.getId(), "集成测试", "运行集成测试", "INTEGRATION_TEST", 5));
                stages.add(createStage(pipeline.getId(), "部署", "部署到目标环境", "DEPLOY", 6));
                stages.add(createStage(pipeline.getId(), "通知", "发送执行结果通知", "NOTIFY", 7));
            }
            default -> {
                stages.add(createStage(pipeline.getId(), "执行", "执行自定义任务", "SCRIPT", 1));
            }
        }

        stageRepository.saveAll(stages);
    }

    /**
     * 创建阶段
     */
    private PipelineStage createStage(Long pipelineId, String name, String description,
                                      String stageType, int order) {
        PipelineStage stage = new PipelineStage();
        stage.setPipelineId(pipelineId);
        stage.setName(name);
        stage.setDescription(description);
        stage.setStageType(stageType);
        stage.setStageOrder(order);
        stage.setStatus("PENDING");
        return stage;
    }

    /**
     * 手动触发流水线执行
     *
     * @param pipelineId 流水线ID
     * @param triggeredBy 触发人ID
     * @param triggeredByName 触发人名称
     * @param triggerType 触发方式（MANUAL/FEISHU）
     * @return 执行结果
     */
    public Pipeline triggerPipeline(Long pipelineId, Long triggeredBy, String triggeredByName,
                                    String triggerType) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        // 检查是否可以执行
        if (pipeline.isRunning()) {
            throw new RuntimeException("流水线正在执行中，无法重复触发");
        }

        // 检查是否需要审批
        if (pipeline.needsApproval()) {
            throw new RuntimeException("流水线需要审批，请先提交审批请求");
        }

        // 检查是否可以执行
        if (!pipeline.canExecute()) {
            throw new RuntimeException("流水线当前状态无法执行");
        }

        // 开始执行
        pipeline.startExecution(triggerType, triggeredBy, triggeredByName);
        pipeline.appendLog("流水线开始执行，触发方式: " + triggerType + "，触发人: " + triggeredByName);

        Pipeline saved = pipelineRepository.save(pipeline);

        // 异步执行流水线
        Future<?> future = executorService.submit(() -> executePipeline(saved));
        runningPipelines.put(pipelineId, future);

        log.info("Pipeline triggered: {} by {} ({})", pipeline.getPipelineNo(), triggeredByName, triggerType);

        return saved;
    }

    /**
     * 自动触发构建流水线
     * 当代码提交时自动触发
     *
     * @param projectId 项目ID
     * @param commitId 提交ID
     * @param branch 分支名称
     */
    public void autoTriggerBuild(String projectId, String commitId, String branch) {
        log.info("Auto-triggering build for project: {}, commit: {}, branch: {}", projectId, commitId, branch);

        // 查找项目的构建流水线
        List<Pipeline> pipelines = pipelineRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        Pipeline buildPipeline = pipelines.stream()
            .filter(p -> "BUILD".equals(p.getPipelineType()) || "TEST".equals(p.getPipelineType()))
            .filter(p -> !p.isRunning())
            .findFirst()
            .orElse(null);

        if (buildPipeline == null) {
            log.warn("No build pipeline found for project: {}", projectId);
            return;
        }

        // 设置自动触发标记
        buildPipeline.setAutoTriggered(true);
        buildPipeline.setGitCommit(commitId);
        buildPipeline.setGitBranch(branch);

        // 执行流水线
        buildPipeline.startExecution("AUTO", 0L, "系统自动触发");
        buildPipeline.appendLog("代码提交自动触发，Commit: " + commitId + "，Branch: " + branch);

        Pipeline saved = pipelineRepository.save(buildPipeline);

        // 异步执行
        Future<?> future = executorService.submit(() -> executePipeline(saved));
        runningPipelines.put(saved.getId(), future);

        log.info("Build pipeline auto-triggered: {}", saved.getPipelineNo());
    }

    /**
     * 提交审批请求
     *
     * @param pipelineId 流水线ID
     * @return 更新后的流水线
     */
    public Pipeline submitApproval(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        if (!pipeline.isRequiresApproval()) {
            throw new RuntimeException("该流水线不需要审批");
        }

        if (pipeline.getApprovalStatus() != null &&
            !"REJECTED".equals(pipeline.getApprovalStatus())) {
            throw new RuntimeException("流水线已有审批状态: " + pipeline.getApprovalStatusDescription());
        }

        pipeline.submitApproval();
        pipeline.appendLog("已提交审批请求");

        Pipeline saved = pipelineRepository.save(pipeline);

        // 通知管理员审批
        notifyAdminForApproval(saved);

        log.info("Approval submitted for pipeline: {}", pipeline.getPipelineNo());

        return saved;
    }

    /**
     * 审批流水线
     *
     * @param pipelineId 流水线ID
     * @param approvedBy 审批人ID
     * @param approvedByName 审批人名称
     * @param approved 是否批准
     * @param comment 审批备注
     * @return 更新后的流水线
     */
    public Pipeline approvePipeline(Long pipelineId, Long approvedBy, String approvedByName,
                                    boolean approved, String comment) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        if (!pipeline.isRequiresApproval()) {
            throw new RuntimeException("该流水线不需要审批");
        }

        if (!"PENDING".equals(pipeline.getApprovalStatus())) {
            throw new RuntimeException("流水线当前审批状态不是待审批");
        }

        if (approved) {
            pipeline.approve(approvedBy, approvedByName, comment);
            pipeline.appendLog("审批已批准，审批人: " + approvedByName + "，备注: " + comment);

            log.info("Pipeline approved: {} by {}", pipeline.getPipelineNo(), approvedByName);
        } else {
            pipeline.reject(approvedBy, approvedByName, comment);
            pipeline.appendLog("审批已拒绝，审批人: " + approvedByName + "，原因: " + comment);

            log.info("Pipeline rejected: {} by {}", pipeline.getPipelineNo(), approvedByName);
        }

        Pipeline saved = pipelineRepository.save(pipeline);

        // 如果批准，自动执行
        if (approved) {
            triggerPipeline(pipelineId, approvedBy, approvedByName, "APPROVAL");
        }

        return saved;
    }

    /**
     * 暂停流水线
     *
     * @param pipelineId 流水线ID
     * @return 更新后的流水线
     */
    public Pipeline pausePipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        if (!pipeline.isRunning()) {
            throw new RuntimeException("流水线未在执行中");
        }

        pipeline.pause();
        pipeline.appendLog("流水线已暂停");

        Pipeline saved = pipelineRepository.save(pipeline);

        log.info("Pipeline paused: {}", pipeline.getPipelineNo());

        return saved;
    }

    /**
     * 恢复流水线
     *
     * @param pipelineId 流水线ID
     * @return 更新后的流水线
     */
    public Pipeline resumePipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        if (!"PAUSED".equals(pipeline.getStatus())) {
            throw new RuntimeException("流水线未在暂停状态");
        }

        pipeline.resume();
        pipeline.appendLog("流水线已恢复");

        Pipeline saved = pipelineRepository.save(pipeline);

        // 重新启动执行
        Future<?> future = executorService.submit(() -> executePipeline(saved));
        runningPipelines.put(pipelineId, future);

        log.info("Pipeline resumed: {}", pipeline.getPipelineNo());

        return saved;
    }

    /**
     * 通知管理员审批
     */
    private void notifyAdminForApproval(Pipeline pipeline) {
        // 通知Producer Agent（项目级隔离：使用运行时 ID）
        String producerId = pipeline.getProjectId() != null ?
            pipeline.getProjectId() + ":producer" : "producer";
        Agent producer = agentManager.getAgent(producerId);
        if (producer != null) {
            AgentMessage message = AgentMessage.builder()
                .fromAgentId("pipeline-engine")
                .toAgentId(producerId)
                .type(AgentMessage.MessageType.NOTIFY)
                .content(String.format("流水线 %s 需要审批\n\n项目: %s\n类型: %s\n\n请通知管理员审批",
                    pipeline.getPipelineNo(),
                    pipeline.getProjectName(),
                    pipeline.getPipelineTypeDescription()))
                .build();
            producer.receiveMessage(message);
        }
    }

    /**
     * 执行流水线
     */
    private void executePipeline(Pipeline pipeline) {
        try {
            // 获取所有阶段
            List<PipelineStage> stages = stageRepository.findByPipelineIdOrderByStageOrderAsc(pipeline.getId());

            int totalStages = stages.size();
            int completedStages = 0;

            for (PipelineStage stage : stages) {
                // 检查是否被取消
                if (!pipeline.isRunning()) {
                    log.info("Pipeline {} cancelled", pipeline.getPipelineNo());
                    return;
                }

                // 更新进度
                int progress = (int) ((double) completedStages / totalStages * 100);
                pipeline.updateProgress(progress, stage.getName());
                pipeline.appendLog("开始执行阶段: " + stage.getName());
                pipelineRepository.save(pipeline);

                // 执行阶段
                boolean success = executeStage(pipeline, stage);

                if (success) {
                    stage.complete();
                    completedStages++;
                    pipeline.appendLog("阶段完成: " + stage.getName());
                } else {
                    stage.fail("阶段执行失败");

                    if (!stage.isContinueOnFailure()) {
                        pipeline.completeFailure("阶段执行失败: " + stage.getName());
                        pipeline.appendLog("流水线失败: 阶段 " + stage.getName() + " 执行失败");
                        pipelineRepository.save(pipeline);
                        stageRepository.save(stage);
                        return;
                    }

                    pipeline.appendLog("阶段失败但继续执行: " + stage.getName());
                }

                stageRepository.save(stage);
            }

            // 所有阶段完成
            pipeline.completeSuccess();
            pipeline.appendLog("流水线执行成功");
            pipelineRepository.save(pipeline);

            log.info("Pipeline completed successfully: {}", pipeline.getPipelineNo());

        } catch (Exception e) {
            log.error("Pipeline execution failed: {}", pipeline.getPipelineNo(), e);
            pipeline.completeFailure(e.getMessage());
            pipeline.appendLog("流水线异常: " + e.getMessage());
            pipelineRepository.save(pipeline);
        } finally {
            runningPipelines.remove(pipeline.getId());
        }
    }

    /**
     * 执行单个阶段
     */
    private boolean executeStage(Pipeline pipeline, PipelineStage stage) {
        stage.start();
        stage.appendLog("阶段开始执行");
        stageRepository.save(stage);

        try {
            // 根据阶段类型执行不同的操作
            String result = switch (stage.getStageType()) {
                case "CHECKOUT" -> executeCheckout(pipeline, stage);
                case "LINT" -> executeLint(pipeline, stage);
                case "UNIT_TEST" -> executeUnitTest(pipeline, stage);
                case "BUILD" -> executeBuild(pipeline, stage);
                case "INTEGRATION_TEST" -> executeIntegrationTest(pipeline, stage);
                case "DEPLOY" -> executeDeploy(pipeline, stage);
                case "NOTIFY" -> executeNotify(pipeline, stage);
                case "SCRIPT" -> executeScript(pipeline, stage);
                default -> "未知阶段类型";
            };

            stage.appendLog("执行结果: " + result);
            return true;

        } catch (Exception e) {
            stage.appendLog("执行异常: " + e.getMessage());
            stage.fail(e.getMessage());
            return false;
        }
    }

    /**
     * 执行代码拉取
     */
    private String executeCheckout(Pipeline pipeline, PipelineStage stage) {
        String projectId = pipeline.getProjectId();
        GameProject project = projectManager.getProject(projectId);

        if (project == null) {
            throw new RuntimeException("项目不存在: " + projectId);
        }

        // 构建Agent提示词
        String prompt = String.format(
            "请执行代码拉取操作：\n\n" +
            "项目: %s\n" +
            "工作目录: %s\n" +
            "分支: %s\n\n" +
            "请执行以下操作：\n" +
            "1. 进入项目目录\n" +
            "2. 拉取最新代码\n" +
            "3. 检查代码状态\n\n" +
            "请输出执行结果。",
            project.getName(),
            project.getWorkDir(),
            pipeline.getGitBranch() != null ? pipeline.getGitBranch() : "main"
        );

        // 发送给Agent执行
        return executeByAgent("git-commit", prompt);
    }

    /**
     * 执行代码检查
     */
    private String executeLint(Pipeline pipeline, PipelineStage stage) {
        String prompt = String.format(
            "请执行代码质量检查：\n\n" +
            "项目ID: %s\n\n" +
            "请执行以下检查：\n" +
            "1. 代码风格检查\n" +
            "2. 安全漏洞扫描\n" +
            "3. 代码复杂度分析\n\n" +
            "请输出检查报告。",
            pipeline.getProjectId()
        );

        return executeByAgent("git-commit", prompt);
    }

    /**
     * 执行单元测试
     */
    private String executeUnitTest(Pipeline pipeline, PipelineStage stage) {
        String projectId = pipeline.getProjectId();
        GameProject project = projectManager.getProject(projectId);

        String prompt = String.format(
            "请执行单元测试：\n\n" +
            "项目: %s\n" +
            "工作目录: %s\n\n" +
            "请执行以下操作：\n" +
            "1. 运行单元测试\n" +
            "2. 生成测试报告\n" +
            "3. 检查测试覆盖率\n\n" +
            "请输出测试结果。",
            project != null ? project.getName() : projectId,
            project != null ? project.getWorkDir() : "N/A"
        );

        return executeByAgent("server-dev", prompt);
    }

    /**
     * 执行构建
     */
    private String executeBuild(Pipeline pipeline, PipelineStage stage) {
        String projectId = pipeline.getProjectId();
        GameProject project = projectManager.getProject(projectId);

        String prompt = String.format(
            "请执行项目构建：\n\n" +
            "项目: %s\n" +
            "工作目录: %s\n\n" +
            "请执行以下操作：\n" +
            "1. 清理旧的构建产物\n" +
            "2. 编译项目代码\n" +
            "3. 打包生成可部署文件\n\n" +
            "请输出构建结果。",
            project != null ? project.getName() : projectId,
            project != null ? project.getWorkDir() : "N/A"
        );

        return executeByAgent("server-dev", prompt);
    }

    /**
     * 执行集成测试
     */
    private String executeIntegrationTest(Pipeline pipeline, PipelineStage stage) {
        String prompt = String.format(
            "请执行集成测试：\n\n" +
            "项目ID: %s\n\n" +
            "请执行以下操作：\n" +
            "1. 启动测试环境\n" +
            "2. 运行集成测试用例\n" +
            "3. 生成测试报告\n\n" +
            "请输出测试结果。",
            pipeline.getProjectId()
        );

        return executeByAgent("server-dev", prompt);
    }

    /**
     * 执行部署
     */
    private String executeDeploy(Pipeline pipeline, PipelineStage stage) {
        String prompt = String.format(
            "请执行部署操作：\n\n" +
            "项目ID: %s\n" +
            "流水线: %s\n\n" +
            "请执行以下操作：\n" +
            "1. 准备部署环境\n" +
            "2. 部署应用\n" +
            "3. 验证部署结果\n\n" +
            "请输出部署结果。",
            pipeline.getProjectId(),
            pipeline.getPipelineNo()
        );

        return executeByAgent("server-dev", prompt);
    }

    /**
     * 执行通知
     */
    private String executeNotify(Pipeline pipeline, PipelineStage stage) {
        // 通知制作人
        AgentMessage message = AgentMessage.builder()
            .fromAgentId("pipeline-engine")
            .toAgentId("producer")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("流水线 %s 执行完成\n\n项目: %s\n状态: %s",
                pipeline.getPipelineNo(), pipeline.getProjectName(), pipeline.getStatusDescription()))
            .build();

        // 发送消息（项目级隔离：使用运行时 ID）
        String producerId = pipeline.getProjectId() != null ?
            pipeline.getProjectId() + ":producer" : "producer";
        Agent producer = agentManager.getAgent(producerId);
        if (producer != null) {
            producer.receiveMessage(message);
        }

        return "通知已发送";
    }

    /**
     * 执行自定义脚本
     */
    private String executeScript(Pipeline pipeline, PipelineStage stage) {
        String commands = stage.getCommands();
        if (commands == null || commands.isEmpty()) {
            return "没有配置执行命令";
        }

        // 解析命令列表
        String prompt = String.format(
            "请执行以下命令：\n\n" +
            "```bash\n%s\n```\n\n" +
            "请输出执行结果。",
            commands
        );

        return executeByAgent("server-dev", prompt);
    }

    /**
     * 通过Agent执行任务
     */
    private String executeByAgent(String agentRole, String prompt) {
        // 查找合适的Agent
        Agent agent = findAvailableAgent(agentRole);

        if (agent == null) {
            throw new RuntimeException("没有可用的Agent: " + agentRole);
        }

        // 发送消息给Agent
        if (agent instanceof BaseAgent baseAgent) {
            // 直接调用Agent的sendMessage方法
            return baseAgent.sendMessage(prompt);
        }

        throw new RuntimeException("Agent类型不支持");
    }

    /**
     * 查找可用的Agent
     */
    private Agent findAvailableAgent(String role) {
        List<Agent> agents = agentManager.getAllAgents();
        for (Agent agent : agents) {
            if (agent.getRole().equals(role) && agent.isAlive() && !agent.isBusy()) {
                return agent;
            }
        }
        // 如果没有空闲的，返回第一个匹配的
        for (Agent agent : agents) {
            if (agent.getRole().equals(role) && agent.isAlive()) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 取消流水线执行
     */
    public Pipeline cancelPipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        if (!pipeline.isRunning()) {
            throw new RuntimeException("流水线未在执行中");
        }

        // 取消执行
        pipeline.cancel();
        pipeline.appendLog("流水线已取消");

        // 中断执行线程
        Future<?> future = runningPipelines.get(pipelineId);
        if (future != null) {
            future.cancel(true);
            runningPipelines.remove(pipelineId);
        }

        Pipeline saved = pipelineRepository.save(pipeline);

        log.info("Pipeline cancelled: {}", pipeline.getPipelineNo());

        return saved;
    }

    /**
     * 更新流水线
     */
    public Pipeline updatePipeline(Pipeline pipeline) {
        Pipeline existing = pipelineRepository.findById(pipeline.getId())
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        existing.setName(pipeline.getName());
        existing.setDescription(pipeline.getDescription());
        existing.setConfig(pipeline.getConfig());

        return pipelineRepository.save(existing);
    }

    /**
     * 删除流水线
     */
    public void deletePipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new RuntimeException("流水线不存在"));

        // 删除关联的阶段
        stageRepository.deleteByPipelineId(pipelineId);

        // 删除流水线
        pipelineRepository.delete(pipeline);
        log.info("Pipeline deleted: {}", pipeline.getPipelineNo());
    }

    /**
     * 获取流水线详情
     */
    public Pipeline getPipeline(Long pipelineId) {
        return pipelineRepository.findById(pipelineId).orElse(null);
    }

    /**
     * 获取流水线阶段
     */
    public List<PipelineStage> getPipelineStages(Long pipelineId) {
        return stageRepository.findByPipelineIdOrderByStageOrderAsc(pipelineId);
    }

    /**
     * 获取项目的所有流水线
     */
    public List<Pipeline> getProjectPipelines(String projectId) {
        return pipelineRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 获取所有流水线
     */
    public List<Pipeline> getAllPipelines() {
        return pipelineRepository.findRecentPipelines();
    }

    /**
     * 获取正在运行的流水线
     */
    public List<Pipeline> getRunningPipelines() {
        return pipelineRepository.findRunningPipelines();
    }

    /**
     * 获取流水线统计
     */
    public Map<String, Object> getPipelineStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态统计
        List<Object[]> statusCounts = pipelineRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 各项目统计
        List<Object[]> projectCounts = pipelineRepository.countByProject();
        stats.put("projectCounts", projectCounts);

        // 总数
        stats.put("totalPipelines", pipelineRepository.count());

        // 运行中数量
        stats.put("runningCount", statusMap.getOrDefault("RUNNING", 0L));

        return stats;
    }

    /**
     * 生成流水线编号
     */
    private String generatePipelineNo() {
        String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PL-" + date + "-" + random;
    }
}
