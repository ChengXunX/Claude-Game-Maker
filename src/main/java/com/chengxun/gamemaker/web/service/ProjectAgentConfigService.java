package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ProjectAgentConfig;
import com.chengxun.gamemaker.web.repository.ProjectAgentConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目级Agent配置服务
 * 管理项目中Agent的自定义配置，支持AI优化提示词
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class ProjectAgentConfigService {

    private static final Logger log = LoggerFactory.getLogger(ProjectAgentConfigService.class);

    private final ProjectAgentConfigRepository configRepository;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;

    @Autowired(required = false)
    private ClaudeAiService aiService;

    public ProjectAgentConfigService(ProjectAgentConfigRepository configRepository,
                                      AgentManager agentManager,
                                      ProjectManager projectManager) {
        this.configRepository = configRepository;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
    }

    /**
     * 获取项目的Agent配置列表
     */
    public List<ProjectAgentConfig> getProjectConfigs(String projectId) {
        return configRepository.findByProjectIdAndIsActiveTrue(projectId);
    }

    /**
     * 获取项目的指定角色Agent配置
     */
    public ProjectAgentConfig getConfig(String projectId, String agentRole) {
        return configRepository.findByProjectIdAndAgentRoleAndIsActiveTrue(projectId, agentRole)
            .orElse(null);
    }

    /**
     * 创建或更新Agent配置
     */
    public ProjectAgentConfig saveConfig(String projectId, String agentRole, ProjectAgentConfig config) {
        ProjectAgentConfig existing = getConfig(projectId, agentRole);

        if (existing != null) {
            // 更新现有配置
            existing.setCustomSystemPrompt(config.getCustomSystemPrompt());
            existing.setCustomCapabilityPrompt(config.getCustomCapabilityPrompt());
            existing.setResponsibilityWeights(config.getResponsibilityWeights());
            existing.setProjectContext(config.getProjectContext());
            existing.setVersion(existing.getVersion() + 1);
            return configRepository.save(existing);
        } else {
            // 创建新配置
            config.setProjectId(projectId);
            config.setAgentRole(agentRole);
            config.setActive(true);
            config.setVersion(1);
            return configRepository.save(config);
        }
    }

    /**
     * 获取Agent的完整提示词（默认 + 自定义）
     */
    public String getFullPrompt(String projectId, String agentRole) {
        ProjectAgentConfig config = getConfig(projectId, agentRole);
        if (config == null) {
            return null;
        }

        StringBuilder fullPrompt = new StringBuilder();

        // 添加自定义系统提示词
        if (config.getCustomSystemPrompt() != null && !config.getCustomSystemPrompt().isEmpty()) {
            fullPrompt.append("## 项目特定配置\n\n");
            fullPrompt.append(config.getCustomSystemPrompt());
            fullPrompt.append("\n\n");
        }

        // 添加自定义能力提示词
        if (config.getCustomCapabilityPrompt() != null && !config.getCustomCapabilityPrompt().isEmpty()) {
            fullPrompt.append("## 项目职责\n\n");
            fullPrompt.append(config.getCustomCapabilityPrompt());
            fullPrompt.append("\n\n");
        }

        // 添加项目上下文
        if (config.getProjectContext() != null && !config.getProjectContext().isEmpty()) {
            fullPrompt.append("## 项目上下文\n\n");
            fullPrompt.append(config.getProjectContext());
            fullPrompt.append("\n\n");
        }

        return fullPrompt.toString();
    }

    /**
     * AI优化Agent提示词
     * 针对当前项目需求，AI分析并优化Agent的提示词
     *
     * @param projectId 项目ID
     * @param agentRole Agent角色
     * @return 优化建议
     */
    public Map<String, Object> optimizeAgentPrompt(String projectId, String agentRole, String direction) {
        Map<String, Object> result = new HashMap<>();

        // 获取项目信息
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            result.put("success", false);
            result.put("message", "项目不存在");
            return result;
        }

        // 获取Agent信息
        Agent agent = agentManager.getAgent(projectId, agentRole);
        String agentName = agent != null ? agent.getName() : agentRole;

        // 获取当前配置
        ProjectAgentConfig currentConfig = getConfig(projectId, agentRole);

        // 构建优化提示词
        String optimizePrompt = buildOptimizePrompt(project, agentRole, agentName, currentConfig, direction);

        // 调用AI进行优化
        if (aiService == null) {
            result.put("success", false);
            result.put("message", "AI服务不可用");
            return result;
        }

        try {
            String aiResponse = aiService.sendMessage(optimizePrompt);

            // 解析AI响应
            Map<String, String> suggestions = parseOptimizationResponse(aiResponse);

            // 保存优化建议
            ProjectAgentConfig config = currentConfig != null ? currentConfig : new ProjectAgentConfig();
            config.setProjectId(projectId);
            config.setAgentRole(agentRole);
            config.setOptimizationSuggestions(aiResponse);

            // 如果AI返回了优化后的提示词，更新配置
            if (suggestions.containsKey("systemPrompt")) {
                config.setCustomSystemPrompt(suggestions.get("systemPrompt"));
            }
            if (suggestions.containsKey("capabilityPrompt")) {
                config.setCustomCapabilityPrompt(suggestions.get("capabilityPrompt"));
            }
            if (suggestions.containsKey("responsibilityWeights")) {
                config.setResponsibilityWeights(suggestions.get("responsibilityWeights"));
            }
            if (suggestions.containsKey("projectContext")) {
                config.setProjectContext(suggestions.get("projectContext"));
            }

            configRepository.save(config);

            result.put("success", true);
            result.put("message", "优化完成");
            result.put("suggestions", suggestions);
            result.put("fullResponse", aiResponse);

        } catch (Exception e) {
            log.error("AI优化失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "AI优化失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 构建优化提示词
     */
    private String buildOptimizePrompt(GameProject project, String agentRole, String agentName,
                                         ProjectAgentConfig currentConfig, String direction) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个AI Agent配置专家。请分析以下项目和Agent信息，优化Agent的提示词配置。\n\n");

        if (direction != null && !direction.trim().isEmpty()) {
            prompt.append("## 用户指定的优化方向\n");
            prompt.append("**重要：请优先按照以下方向进行优化：**\n");
            prompt.append(direction.trim()).append("\n\n");
        }

        prompt.append("## 项目信息\n");
        prompt.append("- 项目名称: ").append(project.getName()).append("\n");
        prompt.append("- 项目目标: ").append(project.getGoal() != null ? project.getGoal() : "未设置").append("\n");
        prompt.append("- 项目类型: ").append(project.getGoalType() != null ? project.getGoalType().name() : "CUSTOM").append("\n");
        prompt.append("- 项目描述: ").append(project.getDescription() != null ? project.getDescription() : "无").append("\n\n");

        prompt.append("## Agent信息\n");
        prompt.append("- Agent角色: ").append(agentRole).append("\n");
        prompt.append("- Agent名称: ").append(agentName).append("\n\n");

        if (currentConfig != null) {
            prompt.append("## 当前配置\n");
            if (currentConfig.getCustomSystemPrompt() != null) {
                prompt.append("### 系统提示词\n").append(currentConfig.getCustomSystemPrompt()).append("\n\n");
            }
            if (currentConfig.getCustomCapabilityPrompt() != null) {
                prompt.append("### 能力提示词\n").append(currentConfig.getCustomCapabilityPrompt()).append("\n\n");
            }
            if (currentConfig.getProjectContext() != null) {
                prompt.append("### 项目上下文\n").append(currentConfig.getProjectContext()).append("\n\n");
            }
        }

        prompt.append("## 优化要求\n");
        prompt.append("请根据项目需求，优化Agent的提示词配置。输出格式如下：\n\n");
        prompt.append("```\n");
        prompt.append("[SYSTEM_PROMPT]\n");
        prompt.append("优化后的系统提示词\n\n");
        prompt.append("[CAPABILITY_PROMPT]\n");
        prompt.append("优化后的能力提示词（描述Agent在项目中的具体职责）\n\n");
        prompt.append("[RESPONSIBILITY_WEIGHTS]\n");
        prompt.append("{\"职责1\": 0.5, \"职责2\": 0.3, \"职责3\": 0.2}\n\n");
        prompt.append("[PROJECT_CONTEXT]\n");
        prompt.append("项目特定上下文信息\n");
        prompt.append("```\n\n");

        prompt.append("请确保：\n");
        prompt.append("1. 提示词要符合项目需求\n");
        prompt.append("2. 职责权重总和为1.0\n");
        prompt.append("3. 上下文信息要有助于Agent理解项目\n");

        return prompt.toString();
    }

    /**
     * 解析AI优化响应
     */
    private Map<String, String> parseOptimizationResponse(String response) {
        Map<String, String> result = new HashMap<>();

        if (response == null || response.isEmpty()) {
            return result;
        }

        // 解析各个部分
        String[] sections = {"SYSTEM_PROMPT", "CAPABILITY_PROMPT", "RESPONSIBILITY_WEIGHTS", "PROJECT_CONTEXT"};

        for (int i = 0; i < sections.length; i++) {
            String startTag = "[" + sections[i] + "]";
            String endTag = i < sections.length - 1 ? "[" + sections[i + 1] + "]" : "```";

            int startIdx = response.indexOf(startTag);
            if (startIdx >= 0) {
                startIdx += startTag.length();
                int endIdx = response.indexOf(endTag, startIdx);
                if (endIdx < 0) {
                    endIdx = response.length();
                }

                String content = response.substring(startIdx, endIdx).trim();

                switch (sections[i]) {
                    case "SYSTEM_PROMPT" -> result.put("systemPrompt", content);
                    case "CAPABILITY_PROMPT" -> result.put("capabilityPrompt", content);
                    case "RESPONSIBILITY_WEIGHTS" -> result.put("responsibilityWeights", content);
                    case "PROJECT_CONTEXT" -> result.put("projectContext", content);
                }
            }
        }

        return result;
    }

    /**
     * 获取Agent的职责权重
     */
    public Map<String, Double> getResponsibilityWeights(String projectId, String agentRole) {
        ProjectAgentConfig config = getConfig(projectId, agentRole);
        if (config == null || config.getResponsibilityWeights() == null) {
            return getDefaultWeights(agentRole);
        }

        try {
            // 解析JSON格式的权重
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(config.getResponsibilityWeights(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("解析职责权重失败: {}", e.getMessage());
            return getDefaultWeights(agentRole);
        }
    }

    /**
     * 获取Agent的绩效评分权重（质量/效率/协作/创新）
     *
     * @param projectId 项目ID
     * @param agentRole Agent角色
     * @return 绩效评分权重，格式: {"quality":1.4,"efficiency":1.2,"collaboration":1.0,"innovation":0.8}
     */
    public Map<String, Double> getPerformanceWeights(String projectId, String agentRole) {
        ProjectAgentConfig config = getConfig(projectId, agentRole);
        if (config == null || config.getPerformanceWeights() == null || config.getPerformanceWeights().isEmpty()) {
            return getDefaultPerformanceWeights(agentRole);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(config.getPerformanceWeights(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("解析绩效评分权重失败: {}", e.getMessage());
            return getDefaultPerformanceWeights(agentRole);
        }
    }

    /**
     * 保存Agent的绩效评分权重
     *
     * @param projectId 项目ID
     * @param agentRole Agent角色
     * @param weights 权重Map
     */
    public void savePerformanceWeights(String projectId, String agentRole, Map<String, Double> weights) {
        ProjectAgentConfig config = getConfig(projectId, agentRole);
        if (config == null) {
            config = new ProjectAgentConfig();
            config.setProjectId(projectId);
            config.setAgentRole(agentRole);
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            config.setPerformanceWeights(mapper.writeValueAsString(weights));
            configRepository.save(config);
            log.info("保存绩效评分权重: project={}, role={}, weights={}", projectId, agentRole, weights);
        } catch (Exception e) {
            throw new RuntimeException("保存权重失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取默认绩效评分权重（按角色区分侧重）
     */
    private Map<String, Double> getDefaultPerformanceWeights(String agentRole) {
        Map<String, Double> weights = new HashMap<>();
        switch (agentRole) {
            case "server-dev", "client-dev", "ui-dev" -> {
                weights.put("quality", 1.4);
                weights.put("efficiency", 1.2);
                weights.put("collaboration", 1.0);
                weights.put("innovation", 0.8);
            }
            case "system-planner", "numerical-planner" -> {
                weights.put("quality", 1.0);
                weights.put("efficiency", 0.8);
                weights.put("collaboration", 1.2);
                weights.put("innovation", 1.4);
            }
            case "tester" -> {
                weights.put("quality", 1.6);
                weights.put("efficiency", 1.0);
                weights.put("collaboration", 1.0);
                weights.put("innovation", 0.6);
            }
            case "git-commit" -> {
                weights.put("quality", 1.0);
                weights.put("efficiency", 1.4);
                weights.put("collaboration", 1.0);
                weights.put("innovation", 0.8);
            }
            case "producer" -> {
                weights.put("quality", 1.0);
                weights.put("efficiency", 1.0);
                weights.put("collaboration", 1.2);
                weights.put("innovation", 1.0);
            }
            default -> {
                weights.put("quality", 1.0);
                weights.put("efficiency", 1.0);
                weights.put("collaboration", 1.0);
                weights.put("innovation", 1.0);
            }
        }
        return weights;
    }

    /**
     * 获取默认职责权重
     */
    private Map<String, Double> getDefaultWeights(String agentRole) {
        Map<String, Double> weights = new HashMap<>();

        switch (agentRole) {
            case "producer" -> {
                weights.put("项目管理", 0.3);
                weights.put("团队协调", 0.3);
                weights.put("质量把控", 0.2);
                weights.put("进度跟踪", 0.2);
            }
            case "server-dev" -> {
                weights.put("后端开发", 0.5);
                weights.put("API设计", 0.3);
                weights.put("代码审查", 0.2);
            }
            case "client-dev" -> {
                weights.put("前端开发", 0.5);
                weights.put("UI实现", 0.3);
                weights.put("交互优化", 0.2);
            }
            case "system-planner" -> {
                weights.put("系统设计", 0.5);
                weights.put("需求分析", 0.3);
                weights.put("文档编写", 0.2);
            }
            case "numerical-planner" -> {
                weights.put("数值设计", 0.5);
                weights.put("平衡调整", 0.3);
                weights.put("配置管理", 0.2);
            }
            case "tester" -> {
                weights.put("功能测试", 0.4);
                weights.put("自动化测试", 0.3);
                weights.put("Bug跟踪", 0.3);
            }
            default -> {
                weights.put("核心任务", 0.6);
                weights.put("协作配合", 0.4);
            }
        }

        return weights;
    }
}
