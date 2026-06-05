package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 应用关闭时关闭线程池
     */
    @PreDestroy
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
