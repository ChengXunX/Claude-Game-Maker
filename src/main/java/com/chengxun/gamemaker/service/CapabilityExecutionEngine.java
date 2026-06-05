package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog;
import com.chengxun.gamemaker.web.entity.CapabilityInvocationLog.InvocationStatus;
import com.chengxun.gamemaker.web.repository.CapabilityInvocationLogRepository;
import com.chengxun.gamemaker.web.service.ApprovalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 能力执行引擎
 * 接收解析后的 CapabilityCall 列表，分派到实际的方法执行
 *
 * 执行流程：
 * 1. 从 CapabilityRegistry 查找能力定义
 * 2. 检查能力是否启用
 * 3. 参数验证
 * 4. 检查冷却时间
 * 5. 如果需要审批 → 创建审批请求
 * 6. 否则 → 执行实际操作
 * 7. 记录调用日志
 * 8. 返回执行结果
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CapabilityExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(CapabilityExecutionEngine.class);

    private final CapabilityRegistry capabilityRegistry;
    private final CapabilityInvocationLogRepository logRepository;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final SkillManager skillManager;
    private final MessageBus messageBus;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ApprovalService approvalService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.BusinessMetricsService metricsService;

    /**
     * 能力执行器注册表
     * key: capabilityName
     * value: 执行函数 (agent, params) → result
     */
    private final Map<String, BiFunction<Agent, Map<String, Object>, CapabilityResult>> executors = new ConcurrentHashMap<>();

    /**
     * 冷却时间记录
     * key: agentId:capabilityName
     * value: 上次调用时间
     */
    private final ConcurrentHashMap<String, LocalDateTime> cooldownMap = new ConcurrentHashMap<>();

    public CapabilityExecutionEngine(CapabilityRegistry capabilityRegistry,
                                      CapabilityInvocationLogRepository logRepository,
                                      AgentManager agentManager,
                                      ProjectManager projectManager,
                                      SkillManager skillManager,
                                      MessageBus messageBus) {
        this.capabilityRegistry = capabilityRegistry;
        this.logRepository = logRepository;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.skillManager = skillManager;
        this.messageBus = messageBus;
        registerDefaultExecutors();
    }

    /**
     * 注册审批服务（延迟注入）
     */
    public void setApprovalService(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // ===== 核心执行方法 =====

    /**
     * 执行一组能力调用
     *
     * @param agent  调用者 Agent
     * @param calls  能力调用列表
     * @return 每个调用的结果
     */
    public List<CapabilityResult> executeCalls(Agent agent, List<CapabilityCall> calls) {
        List<CapabilityResult> results = new ArrayList<>();
        for (CapabilityCall call : calls) {
            results.add(executeCall(agent, call));
        }
        return results;
    }

    /**
     * 执行单个能力调用
     */
    public CapabilityResult executeCall(Agent agent, CapabilityCall call) {
        String agentId = agent.getId();
        String capName = call.getCapabilityName();
        String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
            ? ba.getCurrentProject().getId() : null;

        // 1. 查找能力定义
        AgentCapability capDef = capabilityRegistry.getCapability(agent.getRole(), capName, projectId);
        if (capDef == null) {
            log.warn("Capability not found: {} for role {}", capName, agent.getRole());
            recordLog(agentId, projectId, capName, call, CapabilityResult.notFound(capName));
            return CapabilityResult.notFound(capName);
        }

        // 2. 检查是否启用
        if (!capDef.isEnabled()) {
            log.warn("Capability disabled: {} for role {}", capName, agent.getRole());
            recordLog(agentId, projectId, capName, call, CapabilityResult.disabled(capName));
            return CapabilityResult.disabled(capName);
        }

        // 3. 参数验证
        CapabilityRegistry.ValidationResult validation = capabilityRegistry.validateParams(
            agent.getRole(), capName, call.getParams());
        if (!validation.isValid()) {
            log.warn("Invalid params for capability {}: {}", capName, validation.getErrorMessage());
            recordLog(agentId, projectId, capName, call, CapabilityResult.invalidParams(validation.getErrorMessage()));
            return CapabilityResult.invalidParams(validation.getErrorMessage());
        }

        // 4. 检查冷却时间
        if (capDef.getCooldownSeconds() > 0) {
            String cooldownKey = agentId + ":" + capName;
            LocalDateTime lastCall = cooldownMap.get(cooldownKey);
            if (lastCall != null && lastCall.plusSeconds(capDef.getCooldownSeconds()).isAfter(LocalDateTime.now())) {
                log.info("Capability {} in cooldown for agent {}", capName, agentId);
                recordLog(agentId, projectId, capName, call, CapabilityResult.cooldown(capName));
                return CapabilityResult.cooldown(capName);
            }
        }

        // 5. 审批检查
        if (capDef.isRequiresApproval() && approvalService != null) {
            return handleApproval(agent, capDef, call, projectId);
        }

        // 6. 执行
        CapabilityResult result = doExecute(agent, capDef, call.getParams());

        // 7. 更新冷却时间
        if (capDef.getCooldownSeconds() > 0) {
            cooldownMap.put(agentId + ":" + capName, LocalDateTime.now());
        }

        // 8. 记录日志
        recordLog(agentId, projectId, capName, call, result);

        log.info("Capability {} executed for agent {}: {}", capName, agentId, result.getStatus());

        // 记录指标
        if (metricsService != null) {
            metricsService.recordCapabilityCall();
        }

        return result;
    }

    /**
     * 处理需要审批的能力调用
     */
    private CapabilityResult handleApproval(Agent agent, AgentCapability capDef,
                                             CapabilityCall call, String projectId) {
        try {
            String paramsJson = objectMapper.writeValueAsString(call.getParams());
            var approvalRequest = approvalService.createRequest(
                projectId,
                agent.getId(),
                capDef.getApprovalType() != null ? capDef.getApprovalType() : capDef.getCapabilityName(),
                paramsJson,
                call.getReason() != null ? call.getReason()
                    : String.format("Agent %s 请求执行: %s", agent.getId(), capDef.getDisplayName())
            );

            log.info("Approval request created for capability {}: requestId={}",
                capDef.getCapabilityName(), approvalRequest.getId());

            return CapabilityResult.pendingApproval(approvalRequest.getId());

        } catch (Exception e) {
            log.error("Failed to create approval request for capability {}", capDef.getCapabilityName(), e);
            return CapabilityResult.failed("创建审批请求失败: " + e.getMessage());
        }
    }

    /**
     * 实际执行能力操作
     * 支持三种执行方式：
     * 1. Java 执行器（已注册的硬编码执行器）
     * 2. Prompt 模板执行（纯 AI 能力，动态生成 prompt 调用 Claude）
     * 3. 消息发送（向目标 Agent 发送消息）
     */
    private CapabilityResult doExecute(Agent agent, AgentCapability capDef, Map<String, Object> params) {
        String capabilityName = capDef.getCapabilityName();

        // 1. 优先使用已注册的 Java 执行器
        BiFunction<Agent, Map<String, Object>, CapabilityResult> executor = executors.get(capabilityName);
        if (executor != null) {
            try {
                return executor.apply(agent, params);
            } catch (Exception e) {
                log.error("Capability execution failed: {}", capabilityName, e);
                return CapabilityResult.failed("执行失败: " + e.getMessage());
            }
        }

        // 2. 根据 executionType 执行
        String executionType = capDef.getExecutionType();
        if ("prompt".equals(executionType)) {
            return executeWithPrompt(agent, capDef, params);
        } else if ("message".equals(executionType)) {
            return executeWithMessage(agent, capDef, params);
        }

        // 3. 尝试通用执行（兼容旧的通用能力）
        return executeGeneric(agent, capabilityName, params);
    }

    /**
     * 使用 Prompt 模板执行（纯 AI 能力）
     * 将 promptTemplate 中的 {paramName} 占位符替换为实际参数，然后调用 Claude
     *
     * 适用场景：不需要 Java 代码的纯 AI 任务，如：
     * - UI 设计审查
     * - 数值平衡分析
     * - 文档生成
     * - 任意自定义 AI 任务
     */
    private CapabilityResult executeWithPrompt(Agent agent, AgentCapability capDef, Map<String, Object> params) {
        String promptTemplate = capDef.getPromptTemplate();
        if (promptTemplate == null || promptTemplate.isEmpty()) {
            return CapabilityResult.failed("能力 " + capDef.getCapabilityName() + " 未配置 promptTemplate");
        }

        // 替换占位符 {paramName} → 实际值
        String prompt = promptTemplate;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (prompt.contains(placeholder)) {
                    prompt = prompt.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
        }

        // 添加能力上下文
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(String.format(
            "## 能力调用: %s\n\n%s\n\n%s",
            capDef.getDisplayName(),
            capDef.getDescription() != null ? capDef.getDescription() : "",
            prompt
        ));

        // 加载关联的技能内容并注入到上下文中
        String relatedSkillIds = capDef.getRelatedSkillIds();
        if (relatedSkillIds != null && !relatedSkillIds.isEmpty()) {
            String[] skillIds = relatedSkillIds.split(",");
            StringBuilder skillContext = new StringBuilder();
            for (String skillId : skillIds) {
                skillId = skillId.trim();
                if (skillId.isEmpty()) continue;

                Skill skill = skillManager.getGlobalSkill(skillId);
                if (skill == null) {
                    // 尝试从项目技能中查找
                    String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                        ? ba.getCurrentProject().getId() : null;
                    if (projectId != null) {
                        Map<String, Skill> projectSkills = skillManager.getProjectSkills(projectId);
                        skill = projectSkills.get(skillId);
                    }
                }

                if (skill != null) {
                    skillContext.append("\n\n").append(skill.toPromptSection());
                    log.debug("Loaded related skill {} for capability {}", skillId, capDef.getCapabilityName());
                } else {
                    log.warn("Related skill not found: {} for capability {}", skillId, capDef.getCapabilityName());
                }
            }

            if (skillContext.length() > 0) {
                fullPrompt.append("\n\n## 关联技能参考").append(skillContext);
            }
        }

        try {
            String result = agent.sendMessage(fullPrompt.toString());
            return CapabilityResult.success(result);
        } catch (Exception e) {
            log.error("Prompt execution failed for capability {}", capDef.getCapabilityName(), e);
            return CapabilityResult.failed("执行失败: " + e.getMessage());
        }
    }

    /**
     * 使用消息发送执行（向目标 Agent 发送任务）
     *
     * 适用场景：需要其他 Agent 协作完成的任务，如：
     * - 制作人向 UI 设计师发送设计任务
     * - 向测试员发送测试任务
     */
    private CapabilityResult executeWithMessage(Agent agent, AgentCapability capDef, Map<String, Object> params) {
        String targetRole = capDef.getTargetAgentRole();
        if (targetRole == null || targetRole.isEmpty()) {
            return CapabilityResult.failed("能力 " + capDef.getCapabilityName() + " 未配置 targetAgentRole");
        }

        // 构建消息内容
        String content = buildMessageContent(capDef, params);
        if (content.isEmpty()) {
            return CapabilityResult.failed("消息内容为空");
        }

        // 确定目标 Agent ID
        String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
            ? ba.getCurrentProject().getId() : null;
        String targetAgentId = projectId != null ? projectId + ":" + targetRole : targetRole;

        // 发送消息
        AgentMessage msg = AgentMessage.builder()
            .fromAgentId(agent.getId())
            .toAgentId(targetAgentId)
            .type(AgentMessage.MessageType.TASK)
            .content(content)
            .build();
        agent.sendMessage(msg);

        return CapabilityResult.success("任务已发送给 " + targetRole);
    }

    /**
     * 构建消息内容
     * 优先使用 promptTemplate，否则使用 params 自动生成
     */
    private String buildMessageContent(AgentCapability capDef, Map<String, Object> params) {
        // 如果有 promptTemplate，使用模板
        if (capDef.getPromptTemplate() != null && !capDef.getPromptTemplate().isEmpty()) {
            String content = capDef.getPromptTemplate();
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    content = content.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                }
            }
            return content;
        }

        // 否则使用 params 自动生成
        if (params != null && !params.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("任务：").append(capDef.getDisplayName()).append("\n\n");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }

        return capDef.getDescription() != null ? capDef.getDescription() : "";
    }

    /**
     * 通用执行（处理通用能力如 sendMessage, saveKnowledge 等）
     */
    private CapabilityResult executeGeneric(Agent agent, String capabilityName, Map<String, Object> params) {
        return switch (capabilityName) {
            case "sendMessage" -> {
                String target = getParam(params, "targetAgent", "");
                String content = getParam(params, "content", "");
                String type = getParam(params, "type", "TASK");
                AgentMessage msg = AgentMessage.builder()
                    .fromAgentId(agent.getId())
                    .toAgentId(target)
                    .type(AgentMessage.MessageType.valueOf(type.toUpperCase()))
                    .content(content)
                    .build();
                agent.sendMessage(msg);
                yield CapabilityResult.success("消息已发送");
            }
            case "saveKnowledge" -> {
                String key = getParam(params, "key", "");
                String value = getParam(params, "value", "");
                agent.saveKnowledge(key, value);
                yield CapabilityResult.success("知识已保存");
            }
            case "compactContext" -> {
                if (agent instanceof BaseAgent ba) {
                    ba.compactContext();
                    yield CapabilityResult.success("上下文已压缩");
                }
                yield CapabilityResult.failed("不支持上下文压缩");
            }
            case "reportStatus" -> {
                String status = getParam(params, "status", "");
                String details = getParam(params, "details", "");
                String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
                String producerId = projectId != null ? projectId + ":producer" : "producer";
                AgentMessage report = AgentMessage.createReport(agent.getId(),
                    String.format("状态汇报: %s\n%s", status, details));
                report.setToAgentId(producerId);
                agent.sendMessage(report);
                yield CapabilityResult.success("状态已汇报");
            }
            default -> {
                log.warn("No executor registered for capability: {}", capabilityName);
                yield CapabilityResult.notFound(capabilityName);
            }
        };
    }

    // ===== 默认执行器注册 =====

    private void registerDefaultExecutors() {
        // Producer 能力
        executors.put("createAgent", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以创建 Agent");
            }
            String name = getParam(params, "name", "");
            String role = getParam(params, "role", "");
            String workDir = getParam(params, "workDir", "");
            try {
                var newAgent = producer.createAgent(name, role, null, workDir);
                return CapabilityResult.success(newAgent != null ? "Agent 已创建: " + newAgent.getId() : "已提交审批");
            } catch (Exception e) {
                return CapabilityResult.failed(e.getMessage());
            }
        });

        executors.put("deleteAgent", (agent, params) -> {
            if (!(agent instanceof ProducerAgent)) {
                return CapabilityResult.failed("只有制作人可以删除 Agent");
            }
            String agentId = getParam(params, "agentId", "");
            agentManager.removeAgent(agentId);
            return CapabilityResult.success("Agent 已删除: " + agentId);
        });

        executors.put("assignApiConfig", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以分配 API 配置");
            }
            String agentId = getParam(params, "agentId", "");
            String apiUrl = getParam(params, "apiUrl", "");
            String model = getParam(params, "model", "");
            producer.assignApiConfig(agentId, null, apiUrl, model);
            return CapabilityResult.success("API 配置已分配");
        });

        executors.put("assignWorkDir", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以分配工作目录");
            }
            String agentId = getParam(params, "agentId", "");
            String workDir = getParam(params, "workDir", "");
            producer.assignWorkDir(agentId, workDir);
            return CapabilityResult.success("工作目录已分配: " + workDir);
        });

        executors.put("sendTaskToAgent", (agent, params) -> {
            String target = getParam(params, "targetAgent", "");
            String content = getParam(params, "taskContent", "");
            AgentMessage taskMsg = AgentMessage.createTask(agent.getId(), target, content);
            agent.sendMessage(taskMsg);
            return CapabilityResult.success("任务已发送给 " + target);
        });

        executors.put("queryAgentStatus", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            Agent target = agentManager.getAgent(agentId);
            if (target == null) return CapabilityResult.failed("Agent 不存在: " + agentId);
            return CapabilityResult.success(Map.of(
                "id", target.getId(),
                "name", target.getName(),
                "role", target.getRole(),
                "busy", target.isBusy(),
                "alive", target.isAlive()
            ));
        });

        executors.put("broadcastMessage", (agent, params) -> {
            String content = getParam(params, "content", "");
            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) return CapabilityResult.failed("无法确定项目 ID");
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            for (Agent a : agents) {
                if (!a.getId().equals(agent.getId())) {
                    AgentMessage msg = AgentMessage.builder()
                        .fromAgentId(agent.getId())
                        .toAgentId(a.getId())
                        .type(AgentMessage.MessageType.NOTIFY)
                        .content(content)
                        .build();
                    agent.sendMessage(msg);
                }
            }
            return CapabilityResult.success("消息已广播给 " + agents.size() + " 个 Agent");
        });

        executors.put("requestReview", (agent, params) -> {
            String target = getParam(params, "targetAgent", "");
            String content = getParam(params, "reviewContent", "");
            AgentMessage reviewMsg = AgentMessage.builder()
                .fromAgentId(agent.getId())
                .toAgentId(target)
                .type(AgentMessage.MessageType.REVIEW)
                .content(content)
                .build();
            agent.sendMessage(reviewMsg);
            return CapabilityResult.success("审查请求已发送给 " + target);
        });

        executors.put("notifyUser", (agent, params) -> {
            String message = getParam(params, "message", "");
            // 通过飞书或其他渠道通知用户
            log.info("User notification from {}: {}", agent.getId(), message);
            return CapabilityResult.success("通知已发送");
        });

        executors.put("requestHiring", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以请求招聘");
            }
            String role = getParam(params, "role", "");
            String requirements = getParam(params, "requirements", "");
            String result = producer.requestHiring(role, requirements);
            return CapabilityResult.success(result);
        });

        executors.put("manageAgentCapabilities", (agent, params) -> {
            if (!(agent instanceof ProducerAgent)) {
                return CapabilityResult.failed("只有制作人可以管控 Agent 能力");
            }
            String targetRole = getParam(params, "targetAgentRole", "");
            String action = getParam(params, "action", "");
            String capabilityNames = getParam(params, "capabilityNames", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            String[] names = capabilityNames.split("[,，]");
            int successCount = 0;
            for (String name : names) {
                name = name.trim();
                if (name.isEmpty()) continue;

                try {
                    if ("enable".equalsIgnoreCase(action)) {
                        enableCapabilityForRole(targetRole, name, projectId);
                        successCount++;
                    } else if ("disable".equalsIgnoreCase(action)) {
                        disableCapabilityForRole(targetRole, name, projectId);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to {} capability {} for role {}: {}",
                        action, name, targetRole, e.getMessage());
                }
            }

            return CapabilityResult.success(String.format("已%s %d 个能力给 %s",
                "enable".equalsIgnoreCase(action) ? "启用" : "禁用", successCount, targetRole));
        });

        executors.put("setAgentCapabilitySet", (agent, params) -> {
            if (!(agent instanceof ProducerAgent)) {
                return CapabilityResult.failed("只有制作人可以设置 Agent 能力集");
            }
            String agentRole = getParam(params, "agentRole", "");
            String capabilityNames = getParam(params, "capabilityNames", "");
            String reason = getParam(params, "reason", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            // 获取该角色的所有能力
            List<AgentCapability> allCaps = capabilityRegistry.getCapabilities(agentRole);
            String[] enabledNames = capabilityNames.split("[,，]");

            int enabledCount = 0;
            for (AgentCapability cap : allCaps) {
                boolean shouldEnable = false;
                for (String name : enabledNames) {
                    if (cap.getCapabilityName().equalsIgnoreCase(name.trim())) {
                        shouldEnable = true;
                        break;
                    }
                }

                // 创建项目级覆盖
                AgentCapability override = new AgentCapability();
                override.setAgentRole(agentRole);
                override.setCapabilityName(cap.getCapabilityName());
                override.setDisplayName(cap.getDisplayName());
                override.setDescription(cap.getDescription());
                override.setCategory(cap.getCategory());
                override.setParamSchema(cap.getParamSchema());
                override.setMethodName(cap.getMethodName());
                override.setExecutionType(cap.getExecutionType());
                override.setPromptTemplate(cap.getPromptTemplate());
                override.setTargetAgentRole(cap.getTargetAgentRole());
                override.setPriority(cap.getPriority());
                override.setProjectId(projectId);
                override.setEnabled(shouldEnable);
                override.setRequiresApproval(cap.isRequiresApproval());
                override.setApprovalType(cap.getApprovalType());

                capabilityRegistry.save(override);
                if (shouldEnable) enabledCount++;
            }

            log.info("Set capability set for role {} in project {}: {} enabled",
                agentRole, projectId, enabledCount);

            return CapabilityResult.success(String.format("已为 %s 设置能力集：%d 个启用，原因：%s",
                agentRole, enabledCount, reason));
        });

        // ===== 新增：优化角色配置 =====
        executors.put("optimizeAgentRole", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            String optimizationType = getParam(params, "optimizationType", "capabilities");
            String description = getParam(params, "description", "");

            Agent targetAgent = agentManager.getAgent(agentId);
            if (targetAgent == null) {
                return CapabilityResult.failed("Agent 不存在: " + agentId);
            }

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;

            // 构建优化请求
            String optimizePrompt = String.format(
                "请为 Agent [%s] (%s) 进行 %s 方面的优化。\n\n" +
                "当前状态：\n" +
                "- 角色: %s\n" +
                "- 忙碌: %s\n\n" +
                "优化需求: %s\n\n" +
                "请提供具体的优化建议和执行方案。",
                targetAgent.getName(), agentId, optimizationType,
                targetAgent.getRole(), targetAgent.isBusy(),
                description
            );

            String result = agent.sendMessage(optimizePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：总结游戏方向 =====
        executors.put("summarizeGameDirection", (agent, params) -> {
            String scope = getParam(params, "scope", "current");
            boolean includeRecommendations = Boolean.parseBoolean(getParam(params, "includeRecommendations", "true"));

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            // 获取项目信息
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 获取团队状态
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            StringBuilder teamInfo = new StringBuilder();
            for (Agent a : agents) {
                teamInfo.append(String.format("- %s (%s): %s\n", a.getName(), a.getRole(), a.isBusy() ? "忙碌" : "空闲"));
            }

            String summaryPrompt = String.format(
                "请为项目 [%s] 进行游戏方向总结。\n\n" +
                "项目信息：\n" +
                "- 名称: %s\n" +
                "- 目标: %s\n" +
                "- 状态: %s\n" +
                "- 进度: %d%%\n\n" +
                "团队成员：\n%s\n" +
                "分析范围: %s\n\n" +
                "请总结：\n" +
                "1. 当前游戏特征和核心玩法\n" +
                "2. 已完成的工作\n" +
                "3. 待完成的工作\n" +
                "%s",
                project.getName(), project.getName(),
                project.getGoal() != null ? project.getGoal() : "未设置",
                project.getGoalStatus(), project.getGoalProgress(),
                teamInfo, scope,
                includeRecommendations ? "4. 优化建议和方向调整建议\n" : ""
            );

            String result = agent.sendMessage(summaryPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：统筹项目全局 =====
        executors.put("coordinateProject", (agent, params) -> {
            String action = getParam(params, "action", "status");
            String targetMilestone = getParam(params, "targetMilestone", "");
            String reason = getParam(params, "reason", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("## 项目统筹报告 [%s]\n\n", project.getName()));

            switch (action) {
                case "status" -> {
                    result.append("### 项目状态\n");
                    result.append(String.format("- 目标: %s\n", project.getGoal() != null ? project.getGoal() : "未设置"));
                    result.append(String.format("- 状态: %s\n", project.getGoalStatus()));
                    result.append(String.format("- 进度: %d%%\n\n", project.getGoalProgress()));

                    result.append("### 团队状态\n");
                    List<Agent> agents = agentManager.getAgentsByProject(projectId);
                    for (Agent a : agents) {
                        result.append(String.format("- %s (%s): %s\n", a.getName(), a.getRole(), a.isBusy() ? "忙碌" : "空闲"));
                    }
                }
                case "adjust" -> {
                    if (targetMilestone.isEmpty()) {
                        return CapabilityResult.failed("调整操作需要指定目标里程碑");
                    }
                    result.append(String.format("### 项目调整\n- 目标里程碑: %s\n- 原因: %s\n", targetMilestone, reason));
                    // 这里可以添加实际的调整逻辑
                    result.append("\n调整建议已生成，请根据实际情况执行。");
                }
                case "replan" -> {
                    result.append("### 重新规划\n");
                    result.append("请基于当前进展重新规划项目里程碑和任务分配。\n");
                    // 这里可以调用 AI 重新规划
                    String replanPrompt = String.format(
                        "请为项目 [%s] 重新规划里程碑。当前目标: %s, 进度: %d%%",
                        project.getName(), project.getGoal(), project.getGoalProgress()
                    );
                    String replanResult = agent.sendMessage(replanPrompt);
                    result.append(replanResult);
                }
                default -> {
                    return CapabilityResult.failed("未知操作: " + action);
                }
            }

            return CapabilityResult.success(result.toString());
        });

        // ===== 新增：调整项目目标 =====
        executors.put("adjustProjectGoal", (agent, params) -> {
            String newGoal = getParam(params, "newGoal", "");
            String reason = getParam(params, "reason", "");
            boolean adjustMilestones = Boolean.parseBoolean(getParam(params, "adjustMilestones", "true"));

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            String oldGoal = project.getGoal();
            project.setGoal(newGoal);
            project.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            project.setGoalProgress(0);
            projectManager.saveProjectConfig(project);

            StringBuilder result = new StringBuilder();
            result.append("### 项目目标已调整\n\n");
            result.append(String.format("- 原目标: %s\n", oldGoal != null ? oldGoal : "未设置"));
            result.append(String.format("- 新目标: %s\n", newGoal));
            result.append(String.format("- 原因: %s\n\n", reason));

            if (adjustMilestones) {
                result.append("里程碑将重新分解...\n");
                // 触发目标重新分解
                if (agent instanceof ProducerAgent producer) {
                    // 这里可以调用目标分解逻辑
                    result.append("请等待制作人重新分解目标。");
                }
            }

            return CapabilityResult.success(result.toString());
        });

        // ===== 新增：评估 Agent 绩效 =====
        executors.put("evaluateAgentPerformance", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            String evaluationCriteria = getParam(params, "evaluationCriteria", "综合评估");
            String period = getParam(params, "period", "当前项目");

            Agent targetAgent = agentManager.getAgent(agentId);
            if (targetAgent == null) {
                return CapabilityResult.failed("Agent 不存在: " + agentId);
            }

            String evaluatePrompt = String.format(
                "请对 Agent [%s] (%s) 进行绩效评估。\n\n" +
                "评估信息：\n" +
                "- 角色: %s\n" +
                "- 评估标准: %s\n" +
                "- 评估周期: %s\n" +
                "- 当前状态: %s\n\n" +
                "请从以下维度进行评估：\n" +
                "1. 任务完成质量\n" +
                "2. 工作效率\n" +
                "3. 协作能力\n" +
                "4. 问题解决能力\n" +
                "5. 改进建议",
                targetAgent.getName(), agentId,
                targetAgent.getRole(), evaluationCriteria, period,
                targetAgent.isBusy() ? "忙碌" : "空闲"
            );

            String result = agent.sendMessage(evaluatePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：设定项目目标 =====
        executors.put("setProjectGoal", (agent, params) -> {
            String goal = getParam(params, "goal", "");
            String goalType = getParam(params, "goalType", "CUSTOM");
            String deadline = getParam(params, "deadline", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            project.setGoal(goal);
            try {
                project.setGoalType(GameProject.GoalType.valueOf(goalType));
            } catch (IllegalArgumentException e) {
                project.setGoalType(GameProject.GoalType.CUSTOM);
            }
            project.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            project.setGoalProgress(0);
            if (!deadline.isEmpty()) {
                project.setGoalDeadline(java.time.LocalDateTime.parse(deadline));
            }
            projectManager.saveProjectConfig(project);

            // 通知管理员
            sendNotificationToAdmin(agent, "PROJECT_GOAL_SET",
                String.format("项目 [%s] 已设定新目标: %s", project.getName(), goal));

            return CapabilityResult.success(String.format("项目目标已设定: %s", goal));
        });

        // ===== 新增：分解项目目标 =====
        executors.put("decomposeGoal", (agent, params) -> {
            String milestones = getParam(params, "milestones", "");
            String assignRoles = getParam(params, "assignRoles", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            // 通知管理员目标分解开始
            sendNotificationToAdmin(agent, "GOAL_DECOMPOSITION_START",
                String.format("项目 [%s] 开始分解目标", projectId));

            // 这里可以调用 ProducerAgent 的目标分解逻辑
            return CapabilityResult.success("目标分解已启动，里程碑将自动创建");
        });

        // ===== 新增：调整项目计划 =====
        executors.put("adjustProjectPlan", (agent, params) -> {
            String adjustmentType = getParam(params, "adjustmentType", "");
            String description = getParam(params, "description", "");
            String reason = getParam(params, "reason", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 通知管理员项目计划调整
            sendNotificationToAdmin(agent, "PROJECT_PLAN_ADJUSTED",
                String.format("项目 [%s] 计划已调整\n类型: %s\n描述: %s\n原因: %s",
                    project.getName(), adjustmentType, description, reason));

            return CapabilityResult.success(String.format("项目计划已调整: %s", description));
        });

        // ===== 新增：管理项目风险 =====
        executors.put("manageProjectRisk", (agent, params) -> {
            String riskType = getParam(params, "riskType", "");
            String severity = getParam(params, "severity", "medium");
            String description = getParam(params, "description", "");
            String mitigation = getParam(params, "mitigation", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 高风险需要通知管理员
            if ("high".equals(severity) || "critical".equals(severity)) {
                sendNotificationToAdmin(agent, "PROJECT_RISK_ALERT",
                    String.format("⚠️ 项目 [%s] 风险预警\n类型: %s\n严重程度: %s\n描述: %s\n建议措施: %s",
                        project.getName(), riskType, severity, description, mitigation));
            }

            return CapabilityResult.success(String.format("风险已记录: %s (%s)", description, severity));
        });

        // ===== 新增：协调团队协作 =====
        executors.put("coordinateTeamWork", (agent, params) -> {
            String agents = getParam(params, "agents", "");
            String collaborationType = getParam(params, "collaborationType", "parallel");
            String description = getParam(params, "description", "");

            String[] agentIds = agents.split("[,，]");
            StringBuilder result = new StringBuilder();
            result.append(String.format("团队协作已启动\n类型: %s\n参与成员: %s\n描述: %s\n\n",
                collaborationType, agents, description));

            // 向参与的 Agent 发送协作通知
            for (String agentId : agentIds) {
                agentId = agentId.trim();
                if (!agentId.isEmpty()) {
                    AgentMessage msg = AgentMessage.builder()
                        .fromAgentId(agent.getId())
                        .toAgentId(agentId)
                        .type(AgentMessage.MessageType.NOTIFY)
                        .content(String.format("团队协作通知\n类型: %s\n描述: %s", collaborationType, description))
                        .build();
                    agent.sendMessage(msg);
                    result.append(String.format("- 已通知 %s\n", agentId));
                }
            }

            return CapabilityResult.success(result.toString());
        });

        // ===== 新增：定义游戏概念 =====
        executors.put("defineGameConcept", (agent, params) -> {
            String gameName = getParam(params, "gameName", "");
            String genre = getParam(params, "genre", "");
            String coreGameplay = getParam(params, "coreGameplay", "");
            String targetAudience = getParam(params, "targetAudience", "");
            String uniqueFeatures = getParam(params, "uniqueFeatures", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 保存游戏概念到项目元数据
            Map<String, String> metadata = project.getMetadata();
            metadata.put("gameName", gameName);
            metadata.put("genre", genre);
            metadata.put("coreGameplay", coreGameplay);
            metadata.put("targetAudience", targetAudience);
            metadata.put("uniqueFeatures", uniqueFeatures);
            projectManager.saveProjectConfig(project);

            // 通知管理员
            sendNotificationToAdmin(agent, "GAME_CONCEPT_DEFINED",
                String.format("项目 [%s] 已定义游戏概念\n游戏名称: %s\n类型: %s\n核心玩法: %s",
                    project.getName(), gameName, genre, coreGameplay));

            return CapabilityResult.success(String.format("游戏概念已定义: %s", gameName));
        });

        // ===== 新增：设定设计方向 =====
        executors.put("setDesignDirection", (agent, params) -> {
            String artStyle = getParam(params, "artStyle", "");
            String gameplayStyle = getParam(params, "gameplayStyle", "");
            String technicalRequirements = getParam(params, "technicalRequirements", "");
            String designPrinciples = getParam(params, "designPrinciples", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 保存设计方向到项目元数据
            Map<String, String> metadata = project.getMetadata();
            metadata.put("artStyle", artStyle);
            metadata.put("gameplayStyle", gameplayStyle);
            metadata.put("technicalRequirements", technicalRequirements);
            metadata.put("designPrinciples", designPrinciples);
            projectManager.saveProjectConfig(project);

            // 通知所有 Agent 设计方向
            List<Agent> projectAgents = agentManager.getAgentsByProject(projectId);
            for (Agent a : projectAgents) {
                if (!a.getId().equals(agent.getId())) {
                    AgentMessage msg = AgentMessage.builder()
                        .fromAgentId(agent.getId())
                        .toAgentId(a.getId())
                        .type(AgentMessage.MessageType.NOTIFY)
                        .content(String.format("设计方向已更新\n美术风格: %s\n玩法风格: %s\n技术要求: %s\n设计原则: %s",
                            artStyle, gameplayStyle, technicalRequirements, designPrinciples))
                        .build();
                    agent.sendMessage(msg);
                }
            }

            return CapabilityResult.success("设计方向已设定并通知团队");
        });

        // ===== 新增：特性优先级 =====
        executors.put("prioritizeFeatures", (agent, params) -> {
            String features = getParam(params, "features", "");
            String prioritizationCriteria = getParam(params, "prioritizationCriteria", "impact");

            // 使用 AI 分析特性优先级
            String prioritizePrompt = String.format(
                "请对以下游戏特性进行优先级排序：\n\n特性列表：\n%s\n\n排序标准：%s\n\n" +
                "请输出排序结果，格式为：\n1. 特性名称 - 优先级(高/中/低) - 原因",
                features, prioritizationCriteria
            );

            String result = agent.sendMessage(prioritizePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：审查设计方案 =====
        executors.put("reviewDesign", (agent, params) -> {
            String designType = getParam(params, "designType", "");
            String designName = getParam(params, "designName", "");

            String reviewPrompt = String.format(
                "请审查以下设计方案：\n类型: %s\n名称: %s\n\n请从完整性、可行性、创新性等方面进行评审。",
                designType, designName
            );

            String result = agent.sendMessage(reviewPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：验收交付物 =====
        executors.put("acceptDeliverable", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            String deliverableType = getParam(params, "deliverableType", "");
            String acceptanceCriteria = getParam(params, "acceptanceCriteria", "");

            Agent targetAgent = agentManager.getAgent(agentId);
            if (targetAgent == null) {
                return CapabilityResult.failed("Agent 不存在: " + agentId);
            }

            String reviewPrompt = String.format(
                "请验收 %s 提交的 %s 类型交付物。\n验收标准: %s\n\n请评估是否满足验收条件。",
                agentId, deliverableType, acceptanceCriteria
            );

            String result = agent.sendMessage(reviewPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：获取项目状态 =====
        executors.put("getProjectStatus", (agent, params) -> {
            String detailLevel = getParam(params, "detailLevel", "normal");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            StringBuilder status = new StringBuilder();
            status.append(String.format("## 项目状态报告 [%s]\n\n", project.getName()));
            status.append(String.format("- 目标: %s\n", project.getGoal() != null ? project.getGoal() : "未设置"));
            status.append(String.format("- 状态: %s\n", project.getGoalStatus()));
            status.append(String.format("- 进度: %d%%\n\n", project.getGoalProgress()));

            if ("detailed".equals(detailLevel)) {
                status.append("### 团队成员\n");
                List<Agent> agents = agentManager.getAgentsByProject(projectId);
                for (Agent a : agents) {
                    status.append(String.format("- %s (%s): %s\n", a.getName(), a.getRole(), a.isBusy() ? "忙碌" : "空闲"));
                }
            }

            return CapabilityResult.success(status.toString());
        });

        // ===== 新增：生成进度报告 =====
        executors.put("generateProgressReport", (agent, params) -> {
            String reportType = getParam(params, "reportType", "weekly");
            String recipients = getParam(params, "recipients", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 生成报告
            String reportPrompt = String.format(
                "请生成项目 [%s] 的 %s 进度报告。\n\n" +
                "项目目标: %s\n" +
                "当前进度: %d%%\n" +
                "项目状态: %s\n\n" +
                "请包含以下内容：\n" +
                "1. 本周完成的工作\n" +
                "2. 下周计划\n" +
                "3. 风险和问题\n" +
                "4. 需要的资源或支持",
                project.getName(), reportType, project.getGoal(), project.getGoalProgress(), project.getGoalStatus()
            );

            String report = agent.sendMessage(reportPrompt);

            // 通知接收者
            if (!recipients.isEmpty()) {
                sendNotificationToAdmin(agent, "PROGRESS_REPORT",
                    String.format("项目 [%s] 进度报告已生成\n类型: %s\n\n%s", project.getName(), reportType, report));
            }

            return CapabilityResult.success(report);
        });

        // ===== 新增：风险预警 =====
        executors.put("alertRisk", (agent, params) -> {
            String riskType = getParam(params, "riskType", "");
            String severity = getParam(params, "severity", "medium");
            String description = getParam(params, "description", "");
            String suggestedAction = getParam(params, "suggestedAction", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            String projectName = "未知项目";
            if (projectId != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectName = project.getName();
                }
            }

            // 发送风险预警到管理员
            sendNotificationToAdmin(agent, "RISK_ALERT",
                String.format("⚠️ 风险预警\n项目: %s\n类型: %s\n严重程度: %s\n描述: %s\n建议措施: %s",
                    projectName, riskType, severity, description, suggestedAction));

            return CapabilityResult.success("风险预警已发送");
        });

        // ===== 新增：向管理员汇报 =====
        executors.put("reportToAdmin", (agent, params) -> {
            String reportType = getParam(params, "reportType", "progress");
            String content = getParam(params, "content", "");
            String urgency = getParam(params, "urgency", "normal");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            String projectName = "未知项目";
            if (projectId != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectName = project.getName();
                }
            }

            // 发送汇报到管理员
            String notificationType = switch (reportType) {
                case "progress" -> "PROGRESS_REPORT";
                case "decision" -> "DECISION_REQUEST";
                case "risk" -> "RISK_ALERT";
                case "achievement" -> "ACHIEVEMENT_REPORT";
                default -> "GENERAL_REPORT";
            };

            sendNotificationToAdmin(agent, notificationType,
                String.format("[%s] 项目汇报\n项目: %s\n紧急程度: %s\n\n%s",
                    reportType, projectName, urgency, content));

            return CapabilityResult.success("汇报已发送给管理员");
        });

        // ===== 新增：请求决策 =====
        executors.put("requestDecision", (agent, params) -> {
            String decisionType = getParam(params, "decisionType", "");
            String options = getParam(params, "options", "");
            String recommendation = getParam(params, "recommendation", "");
            String rationale = getParam(params, "rationale", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            String projectName = "未知项目";
            if (projectId != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectName = project.getName();
                }
            }

            // 发送决策请求到管理员
            sendNotificationToAdmin(agent, "DECISION_REQUEST",
                String.format("🔔 决策请求\n项目: %s\n决策类型: %s\n\n可选方案：\n%s\n\n推荐方案：\n%s\n\n理由：\n%s",
                    projectName, decisionType, options, recommendation, rationale));

            return CapabilityResult.success("决策请求已发送给管理员");
        });

        // ===== 新增：制定测试计划 =====
        executors.put("createTestPlan", (agent, params) -> {
            String testScope = getParam(params, "testScope", "");
            String testTypes = getParam(params, "testTypes", "functional");
            String priority = getParam(params, "priority", "medium");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            String projectName = "未知项目";
            if (projectId != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectName = project.getName();
                }
            }

            String planPrompt = String.format(
                "请为项目 [%s] 制定测试计划。\n\n" +
                "测试范围：%s\n" +
                "测试类型：%s\n" +
                "优先级：%s\n\n" +
                "请包含以下内容：\n" +
                "1. 测试目标\n" +
                "2. 测试范围\n" +
                "3. 测试策略\n" +
                "4. 测试用例\n" +
                "5. 资源需求\n" +
                "6. 时间计划",
                projectName, testScope, testTypes, priority
            );

            String result = agent.sendMessage(planPrompt);

            // 通知管理员
            sendNotificationToAdmin(agent, "TEST_PLAN_CREATED",
                String.format("项目 [%s] 测试计划已制定", projectName));

            return CapabilityResult.success(result);
        });

        // ===== 新增：审查测试结果 =====
        executors.put("reviewTestResults", (agent, params) -> {
            String testReportId = getParam(params, "testReportId", "");
            String focus = getParam(params, "focus", "");

            String reviewPrompt = String.format(
                "请审查测试结果。\n\n" +
                "测试报告ID：%s\n" +
                "关注点：%s\n\n" +
                "请分析：\n" +
                "1. 测试覆盖率\n" +
                "2. 通过率\n" +
                "3. 主要问题\n" +
                "4. 质量评估\n" +
                "5. 发布建议",
                testReportId, focus
            );

            String result = agent.sendMessage(reviewPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：管理缺陷列表 =====
        executors.put("manageBugList", (agent, params) -> {
            String action = getParam(params, "action", "list");
            String bugId = getParam(params, "bugId", "");
            String assignee = getParam(params, "assignee", "");

            String managePrompt = String.format(
                "请管理缺陷列表。\n\n" +
                "操作：%s\n" +
                "Bug ID：%s\n" +
                "负责人：%s\n\n" +
                "请执行相应操作并汇报结果。",
                action, bugId, assignee
            );

            String result = agent.sendMessage(managePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：创建构建 =====
        executors.put("createBuild", (agent, params) -> {
            String buildType = getParam(params, "buildType", "debug");
            String environment = getParam(params, "environment", "dev");
            String version = getParam(params, "version", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            String buildPrompt = String.format(
                "请为项目 [%s] 创建构建。\n\n" +
                "构建类型：%s\n" +
                "目标环境：%s\n" +
                "版本号：%s\n\n" +
                "请执行构建命令并汇报结果。",
                project.getName(), buildType, environment, version
            );

            String result = agent.sendMessage(buildPrompt);

            // 通知管理员
            sendNotificationToAdmin(agent, "BUILD_CREATED",
                String.format("项目 [%s] 构建已创建\n类型: %s\n环境: %s\n版本: %s",
                    project.getName(), buildType, environment, version));

            return CapabilityResult.success(result);
        });

        // ===== 新增：部署到环境 =====
        executors.put("deployToEnvironment", (agent, params) -> {
            String environment = getParam(params, "environment", "dev");
            String version = getParam(params, "version", "");
            String rollbackVersion = getParam(params, "rollbackVersion", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            String deployPrompt = String.format(
                "请将项目 [%s] 部署到 %s 环境。\n\n" +
                "版本：%s\n" +
                "回滚版本：%s\n\n" +
                "请执行部署命令并汇报结果。",
                project.getName(), environment, version, rollbackVersion
            );

            String result = agent.sendMessage(deployPrompt);

            // 通知管理员
            sendNotificationToAdmin(agent, "DEPLOYMENT_STARTED",
                String.format("项目 [%s] 开始部署\n环境: %s\n版本: %s",
                    project.getName(), environment, version));

            return CapabilityResult.success(result);
        });

        // ===== 新增：发布版本 =====
        executors.put("publishRelease", (agent, params) -> {
            String version = getParam(params, "version", "");
            String releaseNotes = getParam(params, "releaseNotes", "");
            String channels = getParam(params, "channels", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 通知管理员
            sendNotificationToAdmin(agent, "RELEASE_PUBLISHED",
                String.format("🎉 项目 [%s] 新版本已发布\n版本: %s\n渠道: %s\n\n更新说明：\n%s",
                    project.getName(), version, channels, releaseNotes));

            return CapabilityResult.success(String.format("版本 %s 已发布", version));
        });

        // ===== 新增：分析用户行为 =====
        executors.put("analyzeUserBehavior", (agent, params) -> {
            String metrics = getParam(params, "metrics", "retention");
            String period = getParam(params, "period", "7d");
            String segment = getParam(params, "segment", "");

            String analyzePrompt = String.format(
                "请分析用户行为数据。\n\n" +
                "分析指标：%s\n" +
                "时间周期：%s\n" +
                "用户分群：%s\n\n" +
                "请提供：\n" +
                "1. 数据概览\n" +
                "2. 趋势分析\n" +
                "3. 洞察发现\n" +
                "4. 优化建议",
                metrics, period, segment
            );

            String result = agent.sendMessage(analyzePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：生成数据报告 =====
        executors.put("generateDataReport", (agent, params) -> {
            String reportType = getParam(params, "reportType", "weekly");
            String metrics = getParam(params, "metrics", "");

            String reportPrompt = String.format(
                "请生成数据分析报告。\n\n" +
                "报告类型：%s\n" +
                "关注指标：%s\n\n" +
                "请包含：\n" +
                "1. 核心指标\n" +
                "2. 趋势分析\n" +
                "3. 问题诊断\n" +
                "4. 行动建议",
                reportType, metrics
            );

            String result = agent.sendMessage(reportPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：收集用户反馈 =====
        executors.put("collectFeedback", (agent, params) -> {
            String channels = getParam(params, "channels", "appstore");
            String period = getParam(params, "period", "7d");

            String collectPrompt = String.format(
                "请收集用户反馈。\n\n" +
                "渠道：%s\n" +
                "时间范围：%s\n\n" +
                "请整理：\n" +
                "1. 反馈数量\n" +
                "2. 主要问题\n" +
                "3. 用户建议\n" +
                "4. 情感分析",
                channels, period
            );

            String result = agent.sendMessage(collectPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：分析用户反馈 =====
        executors.put("analyzeFeedback", (agent, params) -> {
            String feedbackId = getParam(params, "feedbackId", "");
            String focus = getParam(params, "focus", "");

            String analyzePrompt = String.format(
                "请分析用户反馈。\n\n" +
                "反馈ID：%s\n" +
                "关注点：%s\n\n" +
                "请提供：\n" +
                "1. 问题归类\n" +
                "2. 影响评估\n" +
                "3. 优先级建议\n" +
                "4. 解决方案",
                feedbackId, focus
            );

            String result = agent.sendMessage(analyzePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：创建反馈响应 =====
        executors.put("createFeedbackResponse", (agent, params) -> {
            String feedbackId = getParam(params, "feedbackId", "");
            String responseType = getParam(params, "responseType", "fix");
            String description = getParam(params, "description", "");

            String responsePrompt = String.format(
                "请创建用户反馈响应方案。\n\n" +
                "反馈ID：%s\n" +
                "响应类型：%s\n" +
                "描述：%s\n\n" +
                "请制定具体的响应方案。",
                feedbackId, responseType, description
            );

            String result = agent.sendMessage(responsePrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：设置预算 =====
        executors.put("setBudget", (agent, params) -> {
            String totalBudget = getParam(params, "totalBudget", "0");
            String breakdown = getParam(params, "breakdown", "");
            String period = getParam(params, "period", "");

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 保存预算信息到项目元数据
            project.getMetadata().put("totalBudget", totalBudget);
            project.getMetadata().put("budgetBreakdown", breakdown);
            project.getMetadata().put("budgetPeriod", period);
            projectManager.saveProjectConfig(project);

            // 通知管理员
            sendNotificationToAdmin(agent, "BUDGET_SET",
                String.format("项目 [%s] 预算已设置\n总额: %s\n周期: %s",
                    project.getName(), totalBudget, period));

            return CapabilityResult.success(String.format("预算已设置: %s", totalBudget));
        });

        // ===== 新增：跟踪支出 =====
        executors.put("trackExpenses", (agent, params) -> {
            String period = getParam(params, "period", "monthly");
            String category = getParam(params, "category", "");

            String trackPrompt = String.format(
                "请跟踪项目支出情况。\n\n" +
                "时间周期：%s\n" +
                "支出类别：%s\n\n" +
                "请提供：\n" +
                "1. 支出概览\n" +
                "2. 分类统计\n" +
                "3. 预算对比\n" +
                "4. 优化建议",
                period, category
            );

            String result = agent.sendMessage(trackPrompt);
            return CapabilityResult.success(result);
        });

        // ===== 新增：优化资源使用 =====
        executors.put("optimizeResourceUsage", (agent, params) -> {
            String resourceType = getParam(params, "resourceType", "api");
            String analysisPeriod = getParam(params, "analysisPeriod", "7d");

            String optimizePrompt = String.format(
                "请分析资源使用情况并提供优化建议。\n\n" +
                "资源类型：%s\n" +
                "分析周期：%s\n\n" +
                "请提供：\n" +
                "1. 使用情况\n" +
                "2. 瓶颈分析\n" +
                "3. 优化方案\n" +
                "4. 预期效果",
                resourceType, analysisPeriod
            );

            String result = agent.sendMessage(optimizePrompt);
            return CapabilityResult.success(result);
        });

        // ServerDev 能力
        executors.put("executeCode", (agent, params) -> {
            String taskDesc = getParam(params, "taskDescription", "");
            String result = agent.sendMessage("请执行以下代码任务: " + taskDesc);
            return CapabilityResult.success(result);
        });

        executors.put("reviewCode", (agent, params) -> {
            String targetPath = getParam(params, "targetPath", "");
            String result = agent.sendMessage("请审查代码: " + targetPath);
            return CapabilityResult.success(result);
        });

        executors.put("reportProgress", (agent, params) -> {
            String taskId = getParam(params, "taskId", "");
            String progress = getParam(params, "progress", "");
            agent.reportProgress(taskId, progress);
            return CapabilityResult.success("进度已汇报");
        });

        executors.put("commitCode", (agent, params) -> {
            String message = getParam(params, "message", "Auto commit");
            String result = agent.sendMessage("请提交代码，提交信息: " + message);
            return CapabilityResult.success(result);
        });

        // SystemPlanner 能力
        executors.put("createDesign", (agent, params) -> {
            String systemName = getParam(params, "systemName", "");
            String requirements = getParam(params, "requirements", "");
            String result = agent.sendMessage(String.format(
                "请为系统 [%s] 创建设计方案，需求: %s", systemName, requirements));
            return CapabilityResult.success(result);
        });

        executors.put("reviewDesign", (agent, params) -> {
            String designName = getParam(params, "designName", "");
            String result = agent.sendMessage("请评审设计: " + designName);
            return CapabilityResult.success(result);
        });

        executors.put("coordinateWithAgent", (agent, params) -> {
            String target = getParam(params, "targetAgent", "");
            String topic = getParam(params, "topic", "");
            AgentMessage msg = AgentMessage.builder()
                .fromAgentId(agent.getId())
                .toAgentId(target)
                .type(AgentMessage.MessageType.QUERY)
                .content("协调事项: " + topic)
                .build();
            agent.sendMessage(msg);
            return CapabilityResult.success("协调消息已发送");
        });

        executors.put("updateDocuments", (agent, params) -> {
            String docType = getParam(params, "documentType", "design");
            String content = getParam(params, "content", "");
            agent.saveKnowledge("document_" + docType, content);
            return CapabilityResult.success("文档已更新");
        });

        // GitCommit 能力
        executors.put("reviewRecentCommits", (agent, params) -> {
            String count = getParam(params, "count", "10");
            String result = agent.sendMessage("请审查最近 " + count + " 条 Git 提交");
            return CapabilityResult.success(result);
        });

        executors.put("checkAuthorInfo", (agent, params) -> {
            String commitId = getParam(params, "commitId", "HEAD");
            String result = agent.sendMessage("请检查提交 " + commitId + " 的作者信息");
            return CapabilityResult.success(result);
        });

        executors.put("sendAlert", (agent, params) -> {
            String alertType = getParam(params, "alertType", "");
            String content = getParam(params, "content", "");
            log.warn("Git Alert [{}] from {}: {}", alertType, agent.getId(), content);
            return CapabilityResult.success("警报已发送");
        });

        // NumericalPlanner 能力
        executors.put("createNumericalDesign", (agent, params) -> {
            String systemName = getParam(params, "systemName", "");
            String requirements = getParam(params, "requirements", "");
            String result = agent.sendMessage(String.format(
                "请为 [%s] 创建数值设计方案，需求: %s", systemName, requirements));
            return CapabilityResult.success(result);
        });

        executors.put("reviewNumerical", (agent, params) -> {
            String target = getParam(params, "targetDesign", "");
            String result = agent.sendMessage("请评审数值设计: " + target);
            return CapabilityResult.success(result);
        });

        executors.put("balanceCheck", (agent, params) -> {
            String scope = getParam(params, "scope", "all");
            String result = agent.sendMessage("请进行平衡性检查，范围: " + scope);
            return CapabilityResult.success(result);
        });

        // UiDev 能力
        executors.put("createUI", (agent, params) -> {
            String componentName = getParam(params, "componentName", "");
            String requirements = getParam(params, "requirements", "");
            String result = agent.sendMessage(String.format(
                "请创建 UI 组件 [%s]，需求: %s", componentName, requirements));
            return CapabilityResult.success(result);
        });

        executors.put("reviewUI", (agent, params) -> {
            String targetPath = getParam(params, "targetPath", "");
            String result = agent.sendMessage("请审查 UI: " + targetPath);
            return CapabilityResult.success(result);
        });

        executors.put("optimizeResponsive", (agent, params) -> {
            String targetPath = getParam(params, "targetPath", "");
            String result = agent.sendMessage("请优化响应式布局: " + targetPath);
            return CapabilityResult.success(result);
        });

        log.info("Registered {} default capability executors", executors.size());
    }

    // ===== 审批回调 =====

    /**
     * 审批通过后执行能力（由 ApprovalService 回调）
     */
    public void executeApprovedCapability(Long approvalRequestId) {
        // 从日志中找到对应的调用记录
        // 这个方法在审批通过后被调用
        log.info("Executing approved capability for approval request: {}", approvalRequestId);
        // TODO: 从 CapabilityInvocationLog 中找到对应的调用并重新执行
    }

    // ===== 工具方法 =====

    @SuppressWarnings("unchecked")
    private <T> T getParam(Map<String, Object> params, String key, T defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * 记录调用日志
     */
    private void recordLog(String agentId, String projectId, String capabilityName,
                           CapabilityCall call, CapabilityResult result) {
        try {
            CapabilityInvocationLog logEntry = new CapabilityInvocationLog(agentId, capabilityName,
                result.isPendingApproval() ? InvocationStatus.PENDING_APPROVAL :
                result.isSuccess() ? InvocationStatus.EXECUTED : InvocationStatus.FAILED);
            logEntry.setProjectId(projectId);
            logEntry.setParams(call.getParams() != null ? objectMapper.writeValueAsString(call.getParams()) : null);
            logEntry.setReason(call.getReason());
            if (result.isPendingApproval()) {
                logEntry.setApprovalRequestId(result.getApprovalRequestId());
            }
            if (result.isFailed()) {
                logEntry.setErrorMessage(result.getError());
            }
            if (result.isSuccess() && result.getData() != null) {
                logEntry.setResult(result.getData().toString());
            }
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record capability invocation log: {}", e.getMessage());
        }
    }

    /**
     * 发送通知给管理员
     *
     * @param agent 发送通知的 Agent
     * @param notificationType 通知类型
     * @param content 通知内容
     */
    private void sendNotificationToAdmin(Agent agent, String notificationType, String content) {
        try {
            // 记录日志
            log.info("Notification to admin [{}] from {}: {}", notificationType, agent.getId(), content);

            // 通过消息总线发送通知到管理员
            AgentMessage notification = AgentMessage.builder()
                .fromAgentId(agent.getId())
                .toAgentId("admin")  // 发送给管理员
                .type(AgentMessage.MessageType.NOTIFY)
                .content(String.format("## 系统通知\n\n**类型**: %s\n**来源**: %s (%s)\n\n%s",
                    notificationType, agent.getName(), agent.getRole(), content))
                .build();
            messageBus.send(notification);

            // 保存通知记录
            agent.saveKnowledge("notification_" + System.currentTimeMillis(),
                String.format("[%s] %s", notificationType, content));

        } catch (Exception e) {
            log.warn("Failed to send notification to admin: {}", e.getMessage());
        }
    }

    // ===== 能力管控辅助方法 =====

    /**
     * 为指定角色在指定项目下启用能力
     * 通过创建项目级覆盖记录实现
     */
    private void enableCapabilityForRole(String agentRole, String capabilityName, String projectId) {
        AgentCapability existing = capabilityRegistry.getCapability(agentRole, capabilityName, projectId);
        if (existing != null) {
            // 已有项目级覆盖，更新启用状态
            if (!existing.isEnabled()) {
                existing.setEnabled(true);
                capabilityRegistry.save(existing);
            }
        } else {
            // 创建项目级覆盖
            AgentCapability global = capabilityRegistry.getCapability(agentRole, capabilityName, null);
            if (global == null) {
                throw new RuntimeException("全局能力不存在: " + capabilityName + " (角色: " + agentRole + ")");
            }

            AgentCapability override = new AgentCapability();
            override.setAgentRole(agentRole);
            override.setCapabilityName(capabilityName);
            override.setDisplayName(global.getDisplayName());
            override.setDescription(global.getDescription());
            override.setCategory(global.getCategory());
            override.setParamSchema(global.getParamSchema());
            override.setMethodName(global.getMethodName());
            override.setExecutionType(global.getExecutionType());
            override.setPromptTemplate(global.getPromptTemplate());
            override.setTargetAgentRole(global.getTargetAgentRole());
            override.setPriority(global.getPriority());
            override.setProjectId(projectId);
            override.setEnabled(true);
            override.setRequiresApproval(global.isRequiresApproval());
            override.setApprovalType(global.getApprovalType());

            capabilityRegistry.save(override);
        }
    }

    /**
     * 为指定角色在指定项目下禁用能力
     * 通过创建项目级覆盖记录实现
     */
    private void disableCapabilityForRole(String agentRole, String capabilityName, String projectId) {
        AgentCapability existing = capabilityRegistry.getCapability(agentRole, capabilityName, projectId);
        if (existing != null) {
            // 已有项目级覆盖，更新禁用状态
            if (existing.isEnabled()) {
                existing.setEnabled(false);
                capabilityRegistry.save(existing);
            }
        } else {
            // 创建项目级覆盖（禁用状态）
            AgentCapability global = capabilityRegistry.getCapability(agentRole, capabilityName, null);
            if (global == null) {
                throw new RuntimeException("全局能力不存在: " + capabilityName + " (角色: " + agentRole + ")");
            }

            AgentCapability override = new AgentCapability();
            override.setAgentRole(agentRole);
            override.setCapabilityName(capabilityName);
            override.setDisplayName(global.getDisplayName());
            override.setDescription(global.getDescription());
            override.setCategory(global.getCategory());
            override.setParamSchema(global.getParamSchema());
            override.setMethodName(global.getMethodName());
            override.setExecutionType(global.getExecutionType());
            override.setPromptTemplate(global.getPromptTemplate());
            override.setTargetAgentRole(global.getTargetAgentRole());
            override.setPriority(global.getPriority());
            override.setProjectId(projectId);
            override.setEnabled(false);
            override.setRequiresApproval(global.isRequiresApproval());
            override.setApprovalType(global.getApprovalType());

            capabilityRegistry.save(override);
        }
    }

    /**
     * 注册自定义执行器
     */
    public void registerExecutor(String capabilityName, BiFunction<Agent, Map<String, Object>, CapabilityResult> executor) {
        executors.put(capabilityName, executor);
        log.info("Registered custom executor for capability: {}", capabilityName);
    }
}
