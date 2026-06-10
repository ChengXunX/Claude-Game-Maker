package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务模板
 * 由策划 Agent 分解生成，包含明确的输入/输出/验收标准
 *
 * @author chengxun
 * @since 2.0.0
 */
public class TaskTemplate {

    /** 任务 ID */
    private String taskId;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 输入要求 - 需要什么前置条件 */
    private String inputRequirements;

    /** 输出产物 - 交付什么 */
    private String outputDeliverables;

    /** 验收标准 - 如何判断完成 */
    private List<String> acceptanceCriteria;

    /** 负责角色 */
    private String assignedRole;

    /** 预估工时（小时） */
    private int estimatedHours;

    /** 优先级：HIGH, MEDIUM, LOW */
    private String priority;

    /** 依赖的任务 ID 列表 */
    private List<String> dependencies;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 所属里程碑 ID */
    private String milestoneId;

    public TaskTemplate() {
        this.taskId = UUID.randomUUID().toString();
        this.acceptanceCriteria = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.priority = "MEDIUM";
        this.createdAt = LocalDateTime.now();
    }

    public TaskTemplate(String title, String assignedRole) {
        this();
        this.title = title;
        this.assignedRole = assignedRole;
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputRequirements() { return inputRequirements; }
    public void setInputRequirements(String inputRequirements) { this.inputRequirements = inputRequirements; }

    public String getOutputDeliverables() { return outputDeliverables; }
    public void setOutputDeliverables(String outputDeliverables) { this.outputDeliverables = outputDeliverables; }

    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getAssignedRole() { return assignedRole; }
    public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }

    public int getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(int estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }

    /**
     * 转换为 Map（用于 API 返回和能力调用）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", taskId);
        map.put("title", title);
        map.put("description", description);
        map.put("inputRequirements", inputRequirements);
        map.put("outputDeliverables", outputDeliverables);
        map.put("acceptanceCriteria", acceptanceCriteria);
        map.put("assignedRole", assignedRole);
        map.put("estimatedHours", estimatedHours);
        map.put("priority", priority);
        map.put("dependencies", dependencies);
        map.put("milestoneId", milestoneId);
        return map;
    }

    /**
     * 从 Map 构建（用于解析 AI 输出）
     */
    public static TaskTemplate fromMap(Map<String, Object> map) {
        TaskTemplate template = new TaskTemplate();
        template.title = (String) map.get("title");
        template.description = (String) map.get("description");
        template.inputRequirements = (String) map.get("inputRequirements");
        template.outputDeliverables = (String) map.get("outputDeliverables");
        template.assignedRole = (String) map.get("assignedRole");
        template.priority = (String) map.getOrDefault("priority", "MEDIUM");
        template.milestoneId = (String) map.get("milestoneId");

        Object hours = map.get("estimatedHours");
        if (hours instanceof Number) {
            template.estimatedHours = ((Number) hours).intValue();
        }

        Object criteria = map.get("acceptanceCriteria");
        if (criteria instanceof List<?> list) {
            template.acceptanceCriteria = new ArrayList<>();
            for (Object item : list) {
                template.acceptanceCriteria.add(item.toString());
            }
        }

        Object deps = map.get("dependencies");
        if (deps instanceof List<?> list) {
            template.dependencies = new ArrayList<>();
            for (Object item : list) {
                template.dependencies.add(item.toString());
            }
        }

        return template;
    }

    @Override
    public String toString() {
        return String.format("TaskTemplate[%s] %s -> %s", assignedRole, title, outputDeliverables);
    }
}
