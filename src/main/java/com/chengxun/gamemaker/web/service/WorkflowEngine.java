package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 工作流引擎
 * 负责Agent协作工作流的管理和执行
 *
 * 主要功能：
 * - 定义工作流模板
 * - 执行工作流实例
 * - 管理任务依赖
 * - 支持并行执行
 * - 协作审批流程
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    @Autowired
    private AgentManager agentManager;

    /** 工作流模板 */
    private final Map<String, WorkflowTemplate> templates = new ConcurrentHashMap<>();

    /** 运行中的工作流实例 */
    private final Map<String, WorkflowInstance> runningInstances = new ConcurrentHashMap<>();

    /** 线程池，用于并行执行任务 */
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 初始化预置工作流模板
     */
    @PostConstruct
    public void init() {
        // ===== 1. 标准游戏开发流程 =====
        WorkflowTemplate standard = new WorkflowTemplate("standard-game-dev", "标准游戏开发流程", "完整的游戏开发流程，包含策划、并行开发、测试审查、部署上线。适用于有客户端和服务端的完整项目");
        WorkflowStep planStep = new WorkflowStep("plan", "系统策划", "system-planner", "分析游戏需求，制定系统架构和技术方案，输出设计文档");
        WorkflowStep devServerStep = new WorkflowStep("dev-server", "服务端开发", "server-dev", "实现服务端逻辑、API接口和数据库设计");
        devServerStep.addDependency("plan");
        WorkflowStep devClientStep = new WorkflowStep("dev-client", "客户端开发", "client-dev", "实现前端界面、交互逻辑和游戏画面");
        devClientStep.addDependency("plan");
        devServerStep.setParallel(true);
        devClientStep.setParallel(true);
        WorkflowStep stdTestStep = new WorkflowStep("test", "测试验证", "tester",
            "执行功能测试、性能测试和兼容性测试。如测试失败，需记录问题并通知开发Agent修复后重新测试，测试通过后方可进入下一步骤");
        stdTestStep.addDependency("dev-server");
        stdTestStep.addDependency("dev-client");
        WorkflowStep stdReviewStep = new WorkflowStep("review", "代码审查", "server-dev", "审查代码质量、注释规范、安全性和性能问题");
        stdReviewStep.addDependency("test");
        WorkflowStep stdDeployStep = new WorkflowStep("deploy", "部署上线", "git-commit", "合并代码并部署到生产环境");
        stdDeployStep.addDependency("review");
        stdDeployStep.setRequiresApproval(true);
        standard.addStep(planStep);
        standard.addStep(devServerStep);
        standard.addStep(devClientStep);
        standard.addStep(stdTestStep);
        standard.addStep(stdReviewStep);
        standard.addStep(stdDeployStep);

        // ===== 2. 服务端开发流程（无客户端） =====
        WorkflowTemplate serverOnly = new WorkflowTemplate("server-only-dev", "服务端开发流程", "纯后端项目开发流程，适用于API服务、微服务、后台管理系统等无客户端的项目");
        WorkflowStep soPlan = new WorkflowStep("plan", "系统策划", "system-planner", "分析需求，设计API接口、数据库模型和系统架构");
        WorkflowStep soDev = new WorkflowStep("dev-server", "服务端开发", "server-dev", "实现业务逻辑、API接口、数据库设计和单元测试");
        soDev.addDependency("plan");
        WorkflowStep soTest = new WorkflowStep("test", "测试验证", "tester",
            "执行接口测试、集成测试和性能测试。如测试失败，需记录问题并通知服务端开发Agent修复后重新测试，测试通过后方可进入下一步骤");
        soTest.addDependency("dev-server");
        WorkflowStep soReview = new WorkflowStep("review", "代码审查", "server-dev", "审查代码质量、API设计合理性、安全性和性能");
        soReview.addDependency("test");
        WorkflowStep soDeploy = new WorkflowStep("deploy", "部署上线", "git-commit", "合并代码并部署到生产环境");
        soDeploy.addDependency("review");
        soDeploy.setRequiresApproval(true);
        serverOnly.addStep(soPlan);
        serverOnly.addStep(soDev);
        serverOnly.addStep(soTest);
        serverOnly.addStep(soReview);
        serverOnly.addStep(soDeploy);

        // ===== 3. 客户端开发流程（无服务端） =====
        WorkflowTemplate clientOnly = new WorkflowTemplate("client-only-dev", "客户端开发流程", "纯前端项目开发流程，适用于H5游戏、小程序、可视化页面等无服务端的项目");
        WorkflowStep coPlan = new WorkflowStep("plan", "交互策划", "system-planner", "分析需求，设计页面结构、交互流程和视觉规范");
        WorkflowStep coDesign = new WorkflowStep("design", "UI开发", "ui-dev", "实现页面布局、组件设计和样式开发");
        coDesign.addDependency("plan");
        WorkflowStep coDev = new WorkflowStep("dev-client", "功能开发", "client-dev", "实现交互逻辑、数据绑定和动画效果");
        coDev.addDependency("design");
        WorkflowStep coTest = new WorkflowStep("test", "测试验证", "tester",
            "执行功能测试、兼容性测试和用户体验测试。如测试失败，需记录问题并通知客户端开发Agent修复后重新测试，测试通过后方可进入下一步骤");
        coTest.addDependency("dev-client");
        WorkflowStep coDeploy = new WorkflowStep("deploy", "发布上线", "git-commit", "合并代码并部署到CDN或静态资源服务器");
        coDeploy.addDependency("test");
        coDeploy.setRequiresApproval(true);
        clientOnly.addStep(coPlan);
        clientOnly.addStep(coDesign);
        clientOnly.addStep(coDev);
        clientOnly.addStep(coTest);
        clientOnly.addStep(coDeploy);

        // ===== 4. 快速原型流程 =====
        WorkflowTemplate rapid = new WorkflowTemplate("rapid-prototype", "快速原型流程", "快速验证游戏创意的轻量流程，跳过审查直接测试，适合Demo和概念验证");
        WorkflowStep rapidPlan = new WorkflowStep("plan", "快速策划", "system-planner", "快速分析需求，确定核心玩法和最小功能集");
        WorkflowStep rapidDev = new WorkflowStep("dev", "快速开发", "server-dev", "实现核心功能的最小可用版本，不做过度设计");
        rapidDev.addDependency("plan");
        WorkflowStep rapidTest = new WorkflowStep("test", "快速测试", "tester",
            "验证核心功能是否可用，记录明显Bug。如核心功能不可用，需通知开发Agent修复后重新测试");
        rapidTest.addDependency("dev");
        rapid.addStep(rapidPlan);
        rapid.addStep(rapidDev);
        rapid.addStep(rapidTest);

        // ===== 5. 紧急修复流程 =====
        WorkflowTemplate hotfix = new WorkflowTemplate("hotfix", "紧急修复流程", "线上问题快速修复流程，精简环节优先恢复服务，适用于紧急Bug和线上故障");
        WorkflowStep hfAnalyze = new WorkflowStep("analyze", "问题分析", "system-planner", "分析线上问题根因，确定影响范围和修复方案");
        WorkflowStep hfFix = new WorkflowStep("fix", "紧急修复", "server-dev", "实施最小化修复，不做无关改动，确保修复精准");
        hfFix.addDependency("analyze");
        WorkflowStep hfTest = new WorkflowStep("test", "验证测试", "tester",
            "验证修复是否生效，确认无回归问题。如修复不彻底，需通知开发Agent补充修复后重新测试");
        hfTest.addDependency("fix");
        WorkflowStep hfDeploy = new WorkflowStep("deploy", "紧急上线", "git-commit", "合并修复代码并紧急部署到生产环境");
        hfDeploy.addDependency("test");
        hfDeploy.setRequiresApproval(true);
        hotfix.addStep(hfAnalyze);
        hotfix.addStep(hfFix);
        hotfix.addStep(hfTest);
        hotfix.addStep(hfDeploy);

        // ===== 6. 功能分支流程 =====
        WorkflowTemplate feature = new WorkflowTemplate("feature-branch", "功能分支流程", "标准的功能开发分支流程，包含设计、实现、测试、审查和合并，适用于常规功能迭代");
        WorkflowStep ftDesign = new WorkflowStep("design", "功能设计", "system-planner", "设计功能方案、接口定义和数据模型");
        WorkflowStep ftImpl = new WorkflowStep("implement", "功能实现", "server-dev", "在功能分支上实现代码，编写单元测试");
        ftImpl.addDependency("design");
        WorkflowStep ftTest = new WorkflowStep("test", "测试验证", "tester",
            "执行功能测试和回归测试。如测试失败，需记录问题并通知开发Agent修复后重新测试，测试通过后方可进入下一步骤");
        ftTest.addDependency("implement");
        WorkflowStep ftReview = new WorkflowStep("review", "代码审查", "server-dev", "审查代码质量、设计合理性和测试覆盖率");
        ftReview.addDependency("test");
        WorkflowStep ftMerge = new WorkflowStep("merge", "合并上线", "git-commit", "审查通过后合并功能分支到主干并部署");
        ftMerge.addDependency("review");
        ftMerge.setRequiresApproval(true);
        feature.addStep(ftDesign);
        feature.addStep(ftImpl);
        feature.addStep(ftTest);
        feature.addStep(ftReview);
        feature.addStep(ftMerge);

        // ===== 7. 代码审查流程 =====
        WorkflowTemplate review = new WorkflowTemplate("code-review", "代码审查流程", "标准化的代码审查流程，确保代码质量，适用于常规代码变更审查");
        WorkflowStep submitReview = new WorkflowStep("submit", "提交审查", "git-commit", "整理代码变更，准备审查材料和变更说明");
        WorkflowStep doReview = new WorkflowStep("review", "执行审查", "server-dev", "审查代码质量、安全性、规范性和潜在问题");
        doReview.addDependency("submit");
        WorkflowStep mergeStep = new WorkflowStep("merge", "合并部署", "git-commit", "审查通过后合并代码并触发部署");
        mergeStep.addDependency("review");
        mergeStep.setRequiresApproval(true);
        review.addStep(submitReview);
        review.addStep(doReview);
        review.addStep(mergeStep);

        // ===== 8. 最小可用流程 =====
        WorkflowTemplate minimal = new WorkflowTemplate("minimal", "最小可用流程", "极简三步流程，适合单人小改动、配置调整和文档更新");
        WorkflowStep minDev = new WorkflowStep("dev", "开发", "server-dev", "完成功能开发或改动");
        WorkflowStep minTest = new WorkflowStep("test", "测试", "tester",
            "快速验证改动是否生效。如测试失败，需通知开发Agent修复后重新测试");
        minTest.addDependency("dev");
        WorkflowStep minDeploy = new WorkflowStep("deploy", "上线", "git-commit", "提交代码并部署");
        minDeploy.addDependency("test");
        minimal.addStep(minDev);
        minimal.addStep(minTest);
        minimal.addStep(minDeploy);

        // 注册所有模板
        registerTemplate(standard);
        registerTemplate(serverOnly);
        registerTemplate(clientOnly);
        registerTemplate(rapid);
        registerTemplate(hotfix);
        registerTemplate(feature);
        registerTemplate(review);
        registerTemplate(minimal);
    }
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Workflow engine executor service shut down");
    }

    /**
     * 工作流模板
     */
    public static class WorkflowTemplate {
        private String id;
        private String name;
        private String description;
        private List<WorkflowStep> steps;

        public WorkflowTemplate(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.steps = new ArrayList<>();
        }

        // Getters and Setters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<WorkflowStep> getSteps() { return steps; }

        public void addStep(WorkflowStep step) {
            steps.add(step);
        }
    }

    /**
     * 工作流步骤
     */
    public static class WorkflowStep {
        private String id;
        private String name;
        private String agentRole;
        private String taskDescription;
        private List<String> dependencies; // 依赖的步骤ID
        private boolean parallel; // 是否可并行执行
        private int timeoutMinutes; // 超时时间（分钟）
        private boolean requiresApproval; // 是否需要审批

        public WorkflowStep(String id, String name, String agentRole, String taskDescription) {
            this.id = id;
            this.name = name;
            this.agentRole = agentRole;
            this.taskDescription = taskDescription;
            this.dependencies = new ArrayList<>();
            this.parallel = false;
            this.timeoutMinutes = 30;
            this.requiresApproval = false;
        }

        // Getters and Setters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getAgentRole() { return agentRole; }
        public String getTaskDescription() { return taskDescription; }
        public List<String> getDependencies() { return dependencies; }
        public boolean isParallel() { return parallel; }
        public int getTimeoutMinutes() { return timeoutMinutes; }
        public boolean isRequiresApproval() { return requiresApproval; }

        public void addDependency(String stepId) {
            dependencies.add(stepId);
        }

        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    }

    /**
     * 工作流实例
     */
    public static class WorkflowInstance {
        private String id;
        private String templateId;
        private String projectId;
        private Map<String, String> parameters;
        private WorkflowStatus status;
        private Map<String, StepExecution> stepExecutions;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;

        public WorkflowInstance(String id, String templateId, String projectId) {
            this.id = id;
            this.templateId = templateId;
            this.projectId = projectId;
            this.parameters = new HashMap<>();
            this.status = WorkflowStatus.CREATED;
            this.stepExecutions = new HashMap<>();
            this.createdAt = LocalDateTime.now();
        }

        // Getters and Setters
        public String getId() { return id; }
        public String getTemplateId() { return templateId; }
        public String getProjectId() { return projectId; }
        public Map<String, String> getParameters() { return parameters; }
        public WorkflowStatus getStatus() { return status; }
        public Map<String, StepExecution> getStepExecutions() { return stepExecutions; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }

        public void setStatus(WorkflowStatus status) { this.status = status; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    /**
     * 步骤执行状态
     */
    public static class StepExecution {
        private String stepId;
        private String agentId;
        private StepStatus status;
        private String result;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String error;

        public StepExecution(String stepId) {
            this.stepId = stepId;
            this.status = StepStatus.PENDING;
        }

        // Getters and Setters
        public String getStepId() { return stepId; }
        public String getAgentId() { return agentId; }
        public StepStatus getStatus() { return status; }
        public String getResult() { return result; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public String getError() { return error; }

        public void setAgentId(String agentId) { this.agentId = agentId; }
        public void setStatus(StepStatus status) { this.status = status; }
        public void setResult(String result) { this.result = result; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        CREATED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    /**
     * 步骤状态枚举
     */
    public enum StepStatus {
        PENDING, WAITING_DEPENDENCIES, READY, RUNNING, COMPLETED, FAILED, SKIPPED
    }

    /**
     * 注册工作流模板
     */
    public void registerTemplate(WorkflowTemplate template) {
        templates.put(template.getId(), template);
        log.info("Workflow template registered: {}", template.getId());
    }

    /**
     * 获取所有工作流模板
     */
    public List<WorkflowTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /**
     * 获取指定模板
     */
    public WorkflowTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /**
     * 创建自定义工作流模板
     */
    public WorkflowTemplate createTemplate(String id, String name, String description, List<WorkflowStep> steps) {
        WorkflowTemplate template = new WorkflowTemplate(id, name, description);
        if (steps != null) {
            for (WorkflowStep step : steps) {
                template.addStep(step);
            }
        }
        templates.put(id, template);
        log.info("Custom workflow template created: {}", id);
        return template;
    }

    /**
     * 删除工作流模板
     */
    public boolean deleteTemplate(String templateId) {
        WorkflowTemplate removed = templates.remove(templateId);
        if (removed != null) {
            log.info("Workflow template deleted: {}", templateId);
            return true;
        }
        return false;
    }

    /**
     * 启动工作流实例
     */
    public WorkflowInstance startWorkflow(String templateId, String projectId, Map<String, String> parameters) {
        WorkflowTemplate template = templates.get(templateId);
        if (template == null) {
            throw new RuntimeException("Workflow template not found: " + templateId);
        }

        String instanceId = UUID.randomUUID().toString();
        WorkflowInstance instance = new WorkflowInstance(instanceId, templateId, projectId);
        instance.getParameters().putAll(parameters);
        instance.setStatus(WorkflowStatus.RUNNING);

        // 初始化所有步骤的执行状态
        for (WorkflowStep step : template.getSteps()) {
            StepExecution execution = new StepExecution(step.getId());
            if (!step.getDependencies().isEmpty()) {
                execution.setStatus(StepStatus.WAITING_DEPENDENCIES);
            }
            instance.getStepExecutions().put(step.getId(), execution);
        }

        runningInstances.put(instanceId, instance);

        // 异步执行工作流
        executorService.submit(() -> executeWorkflow(instance));

        log.info("Workflow started: instanceId={}, templateId={}", instanceId, templateId);

        return instance;
    }

    /**
     * 执行工作流
     */
    private void executeWorkflow(WorkflowInstance instance) {
        WorkflowTemplate template = templates.get(instance.getTemplateId());

        try {
            boolean allCompleted = false;

            while (!allCompleted && instance.getStatus() == WorkflowStatus.RUNNING) {
                allCompleted = true;

                for (WorkflowStep step : template.getSteps()) {
                    StepExecution execution = instance.getStepExecutions().get(step.getId());

                    if (execution.getStatus() == StepStatus.COMPLETED ||
                        execution.getStatus() == StepStatus.SKIPPED) {
                        continue;
                    }

                    // 检查依赖是否完成
                    if (!areDependenciesCompleted(instance, step)) {
                        allCompleted = false;
                        continue;
                    }

                    // 检查是否需要审批
                    if (step.isRequiresApproval() && execution.getStatus() != StepStatus.READY) {
                        execution.setStatus(StepStatus.WAITING_DEPENDENCIES);
                        allCompleted = false;
                        continue;
                    }

                    // 执行步骤
                    allCompleted = false;
                    executeStep(instance, step, execution);
                }

                // 等待一段时间再检查
                Thread.sleep(1000);
            }

            // 检查是否所有步骤都完成
            if (allCompleted) {
                instance.setStatus(WorkflowStatus.COMPLETED);
                instance.setCompletedAt(LocalDateTime.now());
                log.info("Workflow completed: {}", instance.getId());
            }

        } catch (Exception e) {
            instance.setStatus(WorkflowStatus.FAILED);
            log.error("Workflow failed: {}", instance.getId(), e);
        }
    }

    /**
     * 检查依赖是否完成
     */
    private boolean areDependenciesCompleted(WorkflowInstance instance, WorkflowStep step) {
        for (String dependencyId : step.getDependencies()) {
            StepExecution dependencyExecution = instance.getStepExecutions().get(dependencyId);
            if (dependencyExecution == null || dependencyExecution.getStatus() != StepStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行单个步骤
     */
    private void executeStep(WorkflowInstance instance, WorkflowStep step, StepExecution execution) {
        log.info("Executing step: {} in workflow: {}", step.getId(), instance.getId());

        execution.setStatus(StepStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());

        try {
            // 查找合适的Agent
            Agent agent = findAgentForStep(step);
            if (agent == null) {
                throw new RuntimeException("No available agent for role: " + step.getAgentRole());
            }

            execution.setAgentId(agent.getId());

            // 创建任务
            TaskAssignment task = new TaskAssignment();
            task.setId(instance.getId() + ":" + step.getId());
            task.setTitle(step.getName());
            task.setDescription(step.getTaskDescription());
            task.setAssignerId("workflow-engine");

            // 分配任务给Agent
            agent.assignTask(task);

            // 等待任务完成（简化实现，实际应该监听任务完成事件）
            waitForTaskCompletion(agent, task.getId());

            execution.setStatus(StepStatus.COMPLETED);
            execution.setResult("Task completed successfully");
            execution.setCompletedAt(LocalDateTime.now());

            log.info("Step completed: {} in workflow: {}", step.getId(), instance.getId());

        } catch (Exception e) {
            execution.setStatus(StepStatus.FAILED);
            execution.setError(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());

            log.error("Step failed: {} in workflow: {}", step.getId(), instance.getId(), e);
        }
    }

    /**
     * 查找合适的Agent
     */
    private Agent findAgentForStep(WorkflowStep step) {
        List<Agent> agents = agentManager.getAllAgents();
        for (Agent agent : agents) {
            if (agent.getRole().equals(step.getAgentRole()) && agent.isAlive() && !agent.isBusy()) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 等待任务完成
     * 通过轮询检查任务状态来等待任务完成，支持超时控制
     *
     * @param agent 执行任务的Agent
     * @param taskId 任务ID
     * @throws InterruptedException 当等待被中断时抛出
     * @throws RuntimeException 当任务超时或失败时抛出
     */
    private void waitForTaskCompletion(Agent agent, String taskId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMs = 30 * 60 * 1000; // 30分钟超时
        long pollIntervalMs = 2000; // 每2秒轮询一次

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // 检查Agent是否还活着
            if (!agent.isAlive()) {
                throw new RuntimeException("Agent已停止: " + agent.getId());
            }

            // 查找任务状态
            TaskAssignment task = findTaskById(agent, taskId);
            if (task != null) {
                TaskAssignment.TaskStatus status = task.getStatus();
                if (status == TaskAssignment.TaskStatus.COMPLETED) {
                    log.debug("Task {} completed successfully", taskId);
                    return;
                } else if (status == TaskAssignment.TaskStatus.FAILED) {
                    throw new RuntimeException("任务执行失败: " + task.getError());
                } else if (status == TaskAssignment.TaskStatus.CANCELLED) {
                    throw new RuntimeException("任务已被取消");
                }
            }

            // 等待一段时间再轮询
            Thread.sleep(pollIntervalMs);
        }

        // 超时处理
        throw new RuntimeException("任务执行超时（超过30分钟）");
    }

    /**
     * 在Agent的任务列表中查找指定任务
     *
     * @param agent Agent实例
     * @param taskId 任务ID
     * @return 任务对象，如果未找到返回null
     */
    private TaskAssignment findTaskById(Agent agent, String taskId) {
        List<TaskAssignment> tasks = agent.getTasks();
        if (tasks == null) {
            return null;
        }
        for (TaskAssignment task : tasks) {
            if (taskId.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    /**
     * 获取工作流实例
     */
    public WorkflowInstance getInstance(String instanceId) {
        return runningInstances.get(instanceId);
    }

    /**
     * 获取所有运行中的工作流实例
     */
    public List<WorkflowInstance> getRunningInstances() {
        return new ArrayList<>(runningInstances.values());
    }

    /**
     * 取消工作流
     */
    public void cancelWorkflow(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null) {
            instance.setStatus(WorkflowStatus.CANCELLED);
            log.info("Workflow cancelled: {}", instanceId);
        }
    }

    /**
     * 暂停工作流
     */
    public void pauseWorkflow(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null) {
            instance.setStatus(WorkflowStatus.PAUSED);
            log.info("Workflow paused: {}", instanceId);
        }
    }

    /**
     * 恢复工作流
     */
    public void resumeWorkflow(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null && instance.getStatus() == WorkflowStatus.PAUSED) {
            instance.setStatus(WorkflowStatus.RUNNING);
            executorService.submit(() -> executeWorkflow(instance));
            log.info("Workflow resumed: {}", instanceId);
        }
    }
}
