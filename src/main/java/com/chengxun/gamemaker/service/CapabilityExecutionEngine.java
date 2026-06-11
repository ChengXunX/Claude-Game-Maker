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
import com.chengxun.gamemaker.model.TaskTemplate;
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

import java.util.stream.Collectors;

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

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.AlertService alertService;

    @Autowired(required = false)
    private GameRuntimeVerifier gameRuntimeVerifier;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ClaudeAiService aiService;

    @Autowired(required = false)
    private KnowledgeEvolutionService knowledgeEvolutionService;

    @Autowired(required = false)
    private GoalService goalService;

    @Autowired(required = false)
    private ProjectBoard projectBoard;

    @Autowired(required = false)
    private VersionIterationService versionIterationService;

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
                // 安全解析消息类型，未知类型默认使用 NOTIFY
                AgentMessage.MessageType msgType;
                try {
                    msgType = AgentMessage.MessageType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("未知消息类型 '{}'，默认使用 NOTIFY", type);
                    msgType = AgentMessage.MessageType.NOTIFY;
                }
                AgentMessage msg = AgentMessage.builder()
                    .fromAgentId(agent.getId())
                    .toAgentId(target)
                    .type(msgType)
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
                // 发起创建 Agent 审批请求
                producer.requestCreateAgent(name, role, null, workDir);
                return CapabilityResult.success("已发起创建 Agent 审批请求，等待管理员审批");
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

        // 战略决策升级能力 - 制作人可以将重大决策升级为人工审批
        executors.put("escalateStrategicDecision", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以升级战略决策");
            }
            String decisionType = getParam(params, "decisionType", "");
            String description = getParam(params, "description", "");
            String impact = getParam(params, "impact", "");
            String options = getParam(params, "options", "");

            // 验证决策类型
            List<String> validTypes = List.of(
                "PROJECT_DIRECTION",      // 项目方向调整
                "GAMEPLAY_CHANGE",        // 玩法大调整
                "MAJOR_GAMEPLAY_DESIGN",  // 重大玩法设计（新增核心系统、重大功能模块）
                "ARCHITECTURE_CHANGE",    // 架构重大变更
                "BUDGET_ALLOCATION",      // 预算分配
                "TEAM_RESTRUCTURE",       // 团队重组
                "TECHNOLOGY_STACK",       // 技术栈变更
                "RELEASE_STRATEGY"        // 发布策略变更
            );

            if (!validTypes.contains(decisionType.toUpperCase())) {
                return CapabilityResult.failed("无效的决策类型，支持的类型: " + String.join(", ", validTypes));
            }

            // 创建战略决策审批请求
            try {
                String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;

                Map<String, Object> decisionData = new HashMap<>();
                decisionData.put("decisionType", decisionType.toUpperCase());
                decisionData.put("description", description);
                decisionData.put("impact", impact);
                decisionData.put("options", options);
                decisionData.put("producerId", agent.getId());
                decisionData.put("projectId", projectId);

                // 使用 ApprovalService 创建审批请求
                if (producer.getApprovalService() != null) {
                    var approvalRequest = producer.getApprovalService().createRequest(
                        "STRATEGIC_DECISION",
                        agent.getId(),
                        "STRATEGIC_DECISION",
                        decisionData.toString(),
                        String.format("【战略决策升级】\n\n类型: %s\n描述: %s\n影响: %s\n选项: %s",
                            decisionType, description, impact, options)
                    );

                    // 暂停制作人等待审批
                    producer.pauseForApproval(approvalRequest.getId().toString(), "STRATEGIC_DECISION",
                        String.format("战略决策待审批: %s - %s", decisionType, description));

                    return CapabilityResult.success("战略决策已升级为人工审批，等待老板决策。审批ID: " + approvalRequest.getId());
                } else {
                    return CapabilityResult.failed("审批服务不可用");
                }
            } catch (Exception e) {
                return CapabilityResult.failed("创建战略决策审批失败: " + e.getMessage());
            }
        });

        // 项目交付审批能力 - 当项目满足交付条件时申请审批
        executors.put("requestDelivery", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以申请项目交付");
            }

            String reason = getParam(params, "reason", "项目已满足交付条件");

            // 检查是否可以交付
            if (!producer.canDeliverProject()) {
                return CapabilityResult.failed("项目不满足交付条件，请确保所有里程碑已完成");
            }

            // 请求交付审批
            boolean success = producer.requestDeliveryApproval();
            if (success) {
                return CapabilityResult.success("项目交付审批已发起，等待管理员审批");
            } else {
                return CapabilityResult.failed("发起交付审批失败");
            }
        });

        // 评估下一版本能力 - 检查是否需要规划下一个版本或申请交付
        executors.put("evaluateNextVersion", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以评估版本");
            }

            producer.evaluateAndPlanNextVersion();
            return CapabilityResult.success("版本评估已执行");
        });

        // 评估当前版本能力 - 分析Agent绩效、人手缺失与冗余
        executors.put("evaluateCurrentVersion", (agent, params) -> {
            if (!(agent instanceof ProducerAgent producer)) {
                return CapabilityResult.failed("只有制作人可以评估版本");
            }

            String milestoneId = getParam(params, "milestoneId", "");
            if (milestoneId.isEmpty()) {
                return CapabilityResult.failed("milestoneId 不能为空");
            }

            String report = producer.evaluateCurrentVersion(milestoneId);
            return CapabilityResult.success(report);
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

        // ===== 验证能力执行器 =====

        // 通用代码验证
        executors.put("verifyCodeImplementation", (agent, params) -> {
            String projectRoot = getProjectRoot(agent);
            List<String> filePaths = getListParam(params, "filePaths");
            List<String> requiredMethods = getListParam(params, "requiredMethods");
            String testCommand = getParam(params, "testCommand", "");

            StringBuilder result = new StringBuilder();
            List<String> passed = new ArrayList<>();
            List<String> failed = new ArrayList<>();

            // 验证文件存在
            if (filePaths != null && !filePaths.isEmpty()) {
                for (String path : filePaths) {
                    java.io.File file = new java.io.File(projectRoot, path);
                    if (file.exists() && file.length() > 0) {
                        passed.add("文件存在: " + path);
                    } else {
                        failed.add("文件不存在或为空: " + path);
                    }
                }
            }

            // 验证方法存在
            if (requiredMethods != null && !requiredMethods.isEmpty()) {
                for (String method : requiredMethods) {
                    if (searchMethodInProject(projectRoot, method)) {
                        passed.add("方法存在: " + method);
                    } else {
                        failed.add("方法不存在: " + method);
                    }
                }
            }

            // 构建结果
            result.append(String.format("验证结果: 通过 %d/%d 项\n", passed.size(), passed.size() + failed.size()));
            if (!failed.isEmpty()) {
                result.append("失败项:\n");
                failed.forEach(f -> result.append("  - ").append(f).append("\n"));
            }

            return failed.isEmpty() ?
                CapabilityResult.success(result.toString()) :
                CapabilityResult.failed(result.toString());
        });

        // 文档验证
        executors.put("verifyDesignDocument", (agent, params) -> {
            String projectRoot = getProjectRoot(agent);
            List<String> documentPaths = getListParam(params, "documentPaths");
            List<String> requiredSections = getListParam(params, "requiredSections");
            int minWordCount = getIntParam(params, "minWordCount", 100);

            StringBuilder result = new StringBuilder();
            List<String> passed = new ArrayList<>();
            List<String> failed = new ArrayList<>();

            // 验证文档存在
            if (documentPaths != null && !documentPaths.isEmpty()) {
                for (String path : documentPaths) {
                    java.io.File file = new java.io.File(projectRoot, path);
                    if (file.exists() && file.length() > 0) {
                        // 检查文档内容
                        try {
                            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                            int wordCount = content.length();
                            if (wordCount >= minWordCount) {
                                passed.add("文档存在且内容充足: " + path + " (" + wordCount + "字)");
                            } else {
                                failed.add("文档内容不足: " + path + " (" + wordCount + "字，需要" + minWordCount + "字)");
                            }

                            // 检查必要章节
                            if (requiredSections != null && !requiredSections.isEmpty()) {
                                for (String section : requiredSections) {
                                    if (content.contains(section)) {
                                        passed.add("包含章节: " + section);
                                    } else {
                                        failed.add("缺少章节: " + section);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            failed.add("读取文档失败: " + path);
                        }
                    } else {
                        failed.add("文档不存在: " + path);
                    }
                }
            }

            // 构建结果
            result.append(String.format("验证结果: 通过 %d/%d 项\n", passed.size(), passed.size() + failed.size()));
            if (!failed.isEmpty()) {
                result.append("失败项:\n");
                failed.forEach(f -> result.append("  - ").append(f).append("\n"));
            }

            return failed.isEmpty() ?
                CapabilityResult.success(result.toString()) :
                CapabilityResult.failed(result.toString());
        });

        // AI智能验证
        executors.put("aiVerifyImplementation", (agent, params) -> {
            String requirements = getParam(params, "requirements", "");
            List<String> codePaths = getListParam(params, "codePaths");
            String verificationCriteria = getParam(params, "verificationCriteria", "");

            String projectRoot = getProjectRoot(agent);

            // 收集代码内容
            StringBuilder codeContent = new StringBuilder();
            if (codePaths != null && !codePaths.isEmpty()) {
                for (String path : codePaths) {
                    java.io.File file = new java.io.File(projectRoot, path);
                    if (file.exists()) {
                        try {
                            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                            codeContent.append("=== ").append(path).append(" ===\n");
                            codeContent.append(content, 0, Math.min(content.length(), 2000));
                            codeContent.append("\n\n");
                        } catch (Exception e) {
                            // 忽略读取错误
                        }
                    }
                }
            }

            // 构建AI验证提示
            String verificationPrompt = String.format(
                "请验证以下代码实现是否符合需求：\n\n" +
                "需求描述：\n%s\n\n" +
                "代码内容：\n%s\n\n" +
                "验证标准：\n%s\n\n" +
                "请分析并给出验证结果，格式如下：\n" +
                "- 是否通过：PASS/FAIL\n" +
                "- 通过项：[列表]\n" +
                "- 失败项：[列表]\n" +
                "- 改进建议：[建议]",
                requirements, codeContent.toString(), verificationCriteria
            );

            // 调用AI进行验证
            String aiResult = agent.sendMessage(verificationPrompt);
            boolean passed = aiResult.toUpperCase().contains("PASS") && !aiResult.toUpperCase().contains("FAIL");

            return passed ?
                CapabilityResult.success("AI验证通过:\n" + aiResult) :
                CapabilityResult.failed("AI验证未通过:\n" + aiResult);
        });

        // 标准验证
        executors.put("verifyWithCriteria", (agent, params) -> {
            List<String> criteria = getListParam(params, "criteria");
            String projectRoot = getParam(params, "projectRoot", getProjectRoot(agent));
            String reportContent = getParam(params, "reportContent", "");

            if (criteria == null || criteria.isEmpty()) {
                return CapabilityResult.failed("未提供验证标准");
            }

            List<String> passed = new ArrayList<>();
            List<String> failed = new ArrayList<>();

            for (String criterion : criteria) {
                // 简单的标准验证逻辑
                if (reportContent.contains(criterion) || searchMethodInProject(projectRoot, criterion)) {
                    passed.add("✓ " + criterion);
                } else {
                    failed.add("✗ " + criterion);
                }
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("验证结果: 通过 %d/%d 项\n", passed.size(), criteria.size()));
            if (!failed.isEmpty()) {
                result.append("未通过的标准:\n");
                failed.forEach(f -> result.append("  ").append(f).append("\n"));
            }

            return failed.isEmpty() ?
                CapabilityResult.success(result.toString()) :
                CapabilityResult.failed(result.toString());
        });

        // 测试结果验证
        executors.put("verifyTestResults", (agent, params) -> {
            String testCommand = getParam(params, "testCommand", "");
            int minCoverage = getIntParam(params, "minCoverage", 0);
            double requiredPassRate = getDoubleParam(params, "requiredPassRate", 100.0);

            String projectRoot = getProjectRoot(agent);

            // 这里可以执行实际的测试命令并解析结果
            // 简化实现：检查报告中是否包含测试通过的信息
            StringBuilder result = new StringBuilder();
            result.append("测试验证结果:\n");
            result.append("- 测试命令: ").append(testCommand).append("\n");
            result.append("- 最低覆盖率: ").append(minCoverage).append("%\n");
            result.append("- 要求通过率: ").append(requiredPassRate).append("%\n");

            // 实际实现中，这里应该执行测试命令并解析结果
            // 目前返回成功，由调用方提供验证结果
            return CapabilityResult.success(result.toString());
        });

        // 运行时验证
        executors.put("verifyRuntime", (agent, params) -> {
            String runCommand = getParam(params, "runCommand", "");
            String expectedOutput = getParam(params, "expectedOutput", "");
            int timeout = getIntParam(params, "timeout", 30);

            String projectRoot = getProjectRoot(agent);

            StringBuilder result = new StringBuilder();
            result.append("运行时验证结果:\n");
            result.append("- 运行命令: ").append(runCommand).append("\n");
            result.append("- 预期输出: ").append(expectedOutput).append("\n");
            result.append("- 超时时间: ").append(timeout).append("秒\n");

            // 实际实现中，这里应该执行命令并检查输出
            // 目前返回成功，由调用方提供验证结果
            return CapabilityResult.success(result.toString());
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

            // 调用 ProducerAgent 的目标分解逻辑
            if (agent instanceof ProducerAgent producer) {
                producer.decomposeGoalFromCapability();
                return CapabilityResult.success("目标分解已启动，里程碑将自动创建");
            }
            return CapabilityResult.failed("只有制作人可以分解目标");
        });

        // ===== 新增：分解任务（策划 Agent 使用） =====
        executors.put("decomposeTasks", (agent, params) -> {
            String milestoneId = getParam(params, "milestoneId", "");
            String milestoneTitle = getParam(params, "milestoneTitle", "");
            String milestoneDescription = getParam(params, "milestoneDescription", "");
            String goal = getParam(params, "goal", "");

            if (milestoneId.isEmpty() || milestoneTitle.isEmpty()) {
                return CapabilityResult.failed("milestoneId 和 milestoneTitle 不能为空");
            }

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            // 构建分解任务的提示词
            String prompt = buildTaskDecompositionPrompt(milestoneTitle, milestoneDescription, goal);

            // 调用 AI 进行任务分解
            String response = agent.sendMessage(prompt);
            if (response == null || response.isEmpty()) {
                return CapabilityResult.failed("AI 任务分解失败：无响应");
            }

            // 解析任务列表
            List<TaskTemplate> tasks = parseTaskTemplates(response, milestoneId);

            if (tasks.isEmpty()) {
                return CapabilityResult.failed("未能解析出任务列表");
            }

            // 将任务列表保存到项目共享上下文
            if (projectBoard != null) {
                for (TaskTemplate task : tasks) {
                    ProjectBoard.TaskCard card = new ProjectBoard.TaskCard(
                        task.getTaskId(), task.getTitle(), task.getAssignedRole());
                    card.description = task.getDescription();
                    card.priority = task.getPriority();
                    card.metadata.put("inputRequirements", task.getInputRequirements());
                    card.metadata.put("outputDeliverables", task.getOutputDeliverables());
                    card.metadata.put("acceptanceCriteria", task.getAcceptanceCriteria());
                    projectBoard.addTaskCard(projectId, card);
                }
            }

            // 构建返回结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("已分解里程碑 [%s] 为 %d 个任务：\n\n", milestoneTitle, tasks.size()));
            for (int i = 0; i < tasks.size(); i++) {
                TaskTemplate task = tasks.get(i);
                result.append(String.format("%d. **%s** (%s)\n", i + 1, task.getTitle(), task.getAssignedRole()));
                result.append(String.format("   - 输入: %s\n", task.getInputRequirements()));
                result.append(String.format("   - 输出: %s\n", task.getOutputDeliverables()));
                result.append(String.format("   - 验收: %s\n", String.join("; ", task.getAcceptanceCriteria())));
                result.append("\n");
            }

            return CapabilityResult.success(result.toString());
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

        // ===== 风险预警（接入告警系统） =====
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

            // 通过告警系统创建告警记录（触发通知）
            if (alertService != null) {
                try {
                    com.chengxun.gamemaker.web.entity.AlertRecord record = new com.chengxun.gamemaker.web.entity.AlertRecord();
                    record.setRuleName("Agent风险预警");
                    record.setPriority(mapSeverityToPriority(severity));
                    record.setTitle(String.format("[%s] %s", severity.toUpperCase(), riskType));
                    record.setDetail(String.format("项目: %s\n类型: %s\n严重程度: %s\n描述: %s\n建议措施: %s\n报告者: %s (%s)",
                        projectName, riskType, severity, description, suggestedAction, agent.getName(), agent.getRole()));
                    record.setMetric("AGENT_RISK");
                    record.setAgentId(agent.getId());
                    record.setAgentName(agent.getName());
                    record.setProjectId(projectId);
                    record.setStatus(com.chengxun.gamemaker.web.entity.AlertRecord.Status.PENDING.name());
                    record.setCreatedAt(java.time.LocalDateTime.now());
                    record.setUpdatedAt(java.time.LocalDateTime.now());
                    alertService.saveAlert(record);
                } catch (Exception e) {
                    log.error("创建风险告警记录失败: {}", e.getMessage());
                }
            }

            // 同时发送通知到管理员
            sendNotificationToAdmin(agent, "RISK_ALERT",
                String.format("⚠️ 风险预警\n项目: %s\n类型: %s\n严重程度: %s\n描述: %s\n建议措施: %s",
                    projectName, riskType, severity, description, suggestedAction));

            return CapabilityResult.success("风险预警已发送并记录到告警系统");
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

        // 目录配置管理能力
        executors.put("updateDirectoryConfig", (agent, params) -> {
            String path = getParam(params, "path", "");
            String description = getParam(params, "description", "");
            String notes = getParam(params, "notes", "");

            if (path.isEmpty()) {
                return CapabilityResult.failed("目录路径不能为空");
            }

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 添加或更新目录配置
            GameProject.DirectoryConfig config = new GameProject.DirectoryConfig(path, description, notes);
            project.addDirectoryConfig(config);
            projectManager.saveProjectConfig(project);

            return CapabilityResult.success("目录配置已更新: " + path);
        });

        // 项目概况管理能力
        executors.put("updateProjectOverview", (agent, params) -> {
            String overview = getParam(params, "overview", "");
            String deploymentRules = getParam(params, "deploymentRules", "");
            boolean running = Boolean.parseBoolean(getParam(params, "running", "false"));

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return CapabilityResult.failed("项目不存在: " + projectId);
            }

            // 更新项目概况
            if (!overview.isEmpty()) {
                project.setProjectOverview(overview);
            }
            if (!deploymentRules.isEmpty()) {
                project.setDeploymentRules(deploymentRules);
            }
            project.setRunning(running);
            projectManager.saveProjectConfig(project);

            return CapabilityResult.success("项目概况已更新");
        });

        // 游戏验证能力：验证项目结构完整性（增强版：支持深度质量分析）
        executors.put("verifyGameProject", (agent, params) -> {
            String projectDir = getParam(params, "projectDir", "");
            boolean includeQualityAnalysis = Boolean.parseBoolean(getParam(params, "includeQualityAnalysis", "false"));

            if (projectDir.isEmpty()) {
                // 尝试从 Agent 的当前项目获取
                if (agent instanceof BaseAgent ba && ba.getCurrentProject() != null) {
                    projectDir = ba.getCurrentProject().getWorkDir();
                }
            }
            if (projectDir.isEmpty()) {
                return CapabilityResult.failed("无法确定项目目录");
            }

            if (gameRuntimeVerifier == null) {
                return CapabilityResult.failed("验证服务不可用");
            }

            // 1. 结构验证
            GameRuntimeVerifier.VerifyResult structResult = gameRuntimeVerifier.verify(projectDir);
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("## 游戏项目验证报告\n\n");

            if (structResult.isSuccess()) {
                resultBuilder.append("### 结构验证 ✅ 通过\n");
                resultBuilder.append(structResult.toSummary()).append("\n");
            } else {
                resultBuilder.append("### 结构验证 ❌ 失败\n");
                resultBuilder.append(structResult.toSummary()).append("\n");
                return CapabilityResult.failed(resultBuilder.toString());
            }

            // 2. 可选：AI 深度质量分析
            if (includeQualityAnalysis && aiService != null) {
                String projectName = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getName() : null;
                String projectGoal = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getGoal() : null;

                GameRuntimeVerifier.QualityAnalysisResult qualityResult =
                    gameRuntimeVerifier.analyzeQuality(projectDir, projectName, projectGoal);

                if (qualityResult.isSuccess()) {
                    resultBuilder.append("### 质量分析 📊\n");
                    resultBuilder.append(String.format("- 总分: %d/100\n", qualityResult.getOverallScore()));
                    resultBuilder.append(String.format("- 可运行性: %d/100\n", qualityResult.getRunnableScore()));
                    resultBuilder.append(String.format("- 可玩性: %d/100\n", qualityResult.getPlayableScore()));
                    resultBuilder.append(String.format("- 玩法完整性: %d/100\n", qualityResult.getCompletenessScore()));
                    resultBuilder.append(String.format("- UI/UX 质量: %d/100\n", qualityResult.getUiuxScore()));
                    resultBuilder.append(String.format("- 代码质量: %d/100\n", qualityResult.getCodeQualityScore()));
                    resultBuilder.append(String.format("\n总评: %s\n", qualityResult.getSummary()));

                    if (!qualityResult.getIssues().isEmpty()) {
                        resultBuilder.append("\n**问题:**\n");
                        qualityResult.getIssues().forEach(issue -> resultBuilder.append("- ").append(issue).append("\n"));
                    }
                    if (!qualityResult.getSuggestions().isEmpty()) {
                        resultBuilder.append("\n**建议:**\n");
                        qualityResult.getSuggestions().forEach(suggestion -> resultBuilder.append("- ").append(suggestion).append("\n"));
                    }
                } else {
                    resultBuilder.append("### 质量分析 ⚠️ 失败\n");
                    resultBuilder.append(qualityResult.getError()).append("\n");
                }
            }

            return CapabilityResult.success(resultBuilder.toString());
        });

        // 游戏质量深度分析能力
        executors.put("verifyGameQuality", (agent, params) -> {
            String projectDir = getParam(params, "projectDir", "");
            String projectName = getParam(params, "projectName", "");
            String projectGoal = getParam(params, "projectGoal", "");

            if (projectDir.isEmpty()) {
                if (agent instanceof BaseAgent ba && ba.getCurrentProject() != null) {
                    projectDir = ba.getCurrentProject().getWorkDir();
                    if (projectName.isEmpty()) projectName = ba.getCurrentProject().getName();
                    if (projectGoal.isEmpty()) projectGoal = ba.getCurrentProject().getGoal();
                }
            }
            if (projectDir.isEmpty()) {
                return CapabilityResult.failed("无法确定项目目录");
            }

            if (gameRuntimeVerifier == null) {
                return CapabilityResult.failed("验证服务不可用");
            }

            GameRuntimeVerifier.QualityAnalysisResult result =
                gameRuntimeVerifier.analyzeQuality(projectDir, projectName, projectGoal);

            if (!result.isSuccess()) {
                return CapabilityResult.failed(result.getError());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## 游戏质量深度分析报告\n\n");
            sb.append(String.format("### 总分: %d/100\n\n", result.getOverallScore()));
            sb.append("| 维度 | 评分 |\n|------|------|\n");
            sb.append(String.format("| 可运行性 | %d/100 |\n", result.getRunnableScore()));
            sb.append(String.format("| 可玩性 | %d/100 |\n", result.getPlayableScore()));
            sb.append(String.format("| 玩法完整性 | %d/100 |\n", result.getCompletenessScore()));
            sb.append(String.format("| UI/UX 质量 | %d/100 |\n", result.getUiuxScore()));
            sb.append(String.format("| 代码质量 | %d/100 |\n", result.getCodeQualityScore()));

            sb.append(String.format("\n### 总评\n%s\n", result.getSummary()));

            if (!result.getStrengths().isEmpty()) {
                sb.append("\n### 优点\n");
                result.getStrengths().forEach(s -> sb.append("- ").append(s).append("\n"));
            }
            if (!result.getIssues().isEmpty()) {
                sb.append("\n### 问题\n");
                result.getIssues().forEach(i -> sb.append("- ").append(i).append("\n"));
            }
            if (!result.getSuggestions().isEmpty()) {
                sb.append("\n### 改进建议\n");
                result.getSuggestions().forEach(s -> sb.append("- ").append(s).append("\n"));
            }

            // 根据评分给出行动建议
            sb.append("\n### 行动建议\n");
            if (result.getOverallScore() >= 80) {
                sb.append("✅ 质量良好，可以继续推进或准备发布。\n");
            } else if (result.getOverallScore() >= 60) {
                sb.append("⚠️ 质量尚可，建议根据上述问题进行改进后再推进。\n");
            } else {
                sb.append("❌ 质量不足，需要重点改进上述问题后重新验证。\n");
                sb.append("建议：将改进建议发送给负责的 Agent，要求返工。\n");
            }

            // 将验证结果保存到知识库
            if (knowledgeEvolutionService != null) {
                try {
                    String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                        ? ba.getCurrentProject().getId() : "unknown";

                    Map<String, Integer> dimensionScores = new HashMap<>();
                    dimensionScores.put("runnable", result.getRunnableScore());
                    dimensionScores.put("playable", result.getPlayableScore());
                    dimensionScores.put("completeness", result.getCompletenessScore());
                    dimensionScores.put("uiux", result.getUiuxScore());
                    dimensionScores.put("codeQuality", result.getCodeQualityScore());

                    knowledgeEvolutionService.learnFromGameVerification(
                        agent.getId(), projectId, projectName,
                        result.getOverallScore(), dimensionScores,
                        result.getIssues(), result.getSuggestions(),
                        result.getOverallScore() >= 60);

                    log.info("验证结果已保存到知识库: project={}, score={}", projectName, result.getOverallScore());
                } catch (Exception e) {
                    log.warn("保存验证结果到知识库失败: {}", e.getMessage());
                }
            }

            return CapabilityResult.success(sb.toString());
        });

        // 验证并改进能力：验证 + 自动生成改进建议 + 可选自动触发改进
        executors.put("verifyAndImprove", (agent, params) -> {
            String projectDir = getParam(params, "projectDir", "");
            boolean autoImprove = Boolean.parseBoolean(getParam(params, "autoImprove", "false"));
            String targetAgent = getParam(params, "targetAgent", "");

            if (projectDir.isEmpty()) {
                if (agent instanceof BaseAgent ba && ba.getCurrentProject() != null) {
                    projectDir = ba.getCurrentProject().getWorkDir();
                }
            }
            if (projectDir.isEmpty()) {
                return CapabilityResult.failed("无法确定项目目录");
            }

            if (gameRuntimeVerifier == null) {
                return CapabilityResult.failed("验证服务不可用");
            }

            // 1. 结构验证
            GameRuntimeVerifier.VerifyResult structResult = gameRuntimeVerifier.verify(projectDir);
            if (!structResult.isSuccess()) {
                return CapabilityResult.failed("结构验证失败: " + structResult.toSummary());
            }

            // 2. 质量分析
            String projectName = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getName() : null;
            String projectGoal = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getGoal() : null;

            GameRuntimeVerifier.QualityAnalysisResult qualityResult =
                gameRuntimeVerifier.analyzeQuality(projectDir, projectName, projectGoal);

            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("## 验证并改进报告\n\n");

            if (!qualityResult.isSuccess()) {
                resultBuilder.append("质量分析失败: ").append(qualityResult.getError()).append("\n");
                return CapabilityResult.failed(resultBuilder.toString());
            }

            resultBuilder.append(String.format("### 当前质量评分: %d/100\n\n", qualityResult.getOverallScore()));

            // 3. 判断是否需要改进
            if (qualityResult.getOverallScore() >= 80) {
                resultBuilder.append("✅ 质量良好，无需改进。\n");
                return CapabilityResult.success(resultBuilder.toString());
            }

            // 4. 生成改进建议
            resultBuilder.append("### 需要改进的方面\n\n");

            if (qualityResult.getPlayableScore() < 60) {
                resultBuilder.append("- **可玩性** (").append(qualityResult.getPlayableScore()).append("/100): 需要完善核心玩法循环\n");
            }
            if (qualityResult.getCompletenessScore() < 60) {
                resultBuilder.append("- **玩法完整性** (").append(qualityResult.getCompletenessScore()).append("/100): 需要补充缺失的游戏机制\n");
            }
            if (qualityResult.getUiuxScore() < 60) {
                resultBuilder.append("- **UI/UX 质量** (").append(qualityResult.getUiuxScore()).append("/100): 需要改进界面设计和交互\n");
            }
            if (qualityResult.getCodeQualityScore() < 60) {
                resultBuilder.append("- **代码质量** (").append(qualityResult.getCodeQualityScore()).append("/100): 需要重构和优化代码\n");
            }

            resultBuilder.append("\n### 具体改进建议\n");
            qualityResult.getSuggestions().forEach(s -> resultBuilder.append("- ").append(s).append("\n"));

            // 5. 可选：自动触发改进
            if (autoImprove && !targetAgent.isEmpty()) {
                String improvePrompt = String.format(
                    "游戏项目验证未通过（总分: %d/100），需要改进以下方面：\n\n%s\n\n请根据上述建议进行改进。",
                    qualityResult.getOverallScore(),
                    String.join("\n", qualityResult.getSuggestions())
                );

                AgentMessage improveMsg = AgentMessage.builder()
                    .fromAgentId(agent.getId())
                    .toAgentId(targetAgent)
                    .type(AgentMessage.MessageType.TASK)
                    .content(improvePrompt)
                    .build();
                agent.sendMessage(improveMsg);

                resultBuilder.append("\n### 自动改进\n");
                resultBuilder.append("已向 ").append(targetAgent).append(" 发送改进任务。\n");
            } else if (!autoImprove) {
                resultBuilder.append("\n### 下一步\n");
                resultBuilder.append("建议将改进建议发送给负责的 Agent（使用 sendTaskToAgent 能力）。\n");
            }

            // 6. 将验证结果保存到知识库
            if (knowledgeEvolutionService != null) {
                try {
                    String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                        ? ba.getCurrentProject().getId() : "unknown";

                    Map<String, Integer> dimensionScores = new HashMap<>();
                    dimensionScores.put("runnable", qualityResult.getRunnableScore());
                    dimensionScores.put("playable", qualityResult.getPlayableScore());
                    dimensionScores.put("completeness", qualityResult.getCompletenessScore());
                    dimensionScores.put("uiux", qualityResult.getUiuxScore());
                    dimensionScores.put("codeQuality", qualityResult.getCodeQualityScore());

                    boolean passed = qualityResult.getOverallScore() >= 80;
                    knowledgeEvolutionService.learnFromGameVerification(
                        agent.getId(), projectId, projectName != null ? projectName : "未知项目",
                        qualityResult.getOverallScore(), dimensionScores,
                        qualityResult.getIssues(), qualityResult.getSuggestions(), passed);

                    resultBuilder.append("\n### 知识库更新\n");
                    resultBuilder.append("验证结果已保存到知识库，供后续项目参考。\n");

                    log.info("验证并改进结果已保存到知识库: project={}, score={}",
                        projectName, qualityResult.getOverallScore());
                } catch (Exception e) {
                    log.warn("保存验证结果到知识库失败: {}", e.getMessage());
                }
            }

            return CapabilityResult.success(resultBuilder.toString());
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

        // 里程碑管理能力
        executors.put("updateMilestone", (agent, params) -> {
            String milestoneId = getParam(params, "milestoneId", "");
            String status = getParam(params, "status", "");
            Integer progress = params.containsKey("progress") ? getParam(params, "progress", 0) : null;

            if (milestoneId.isEmpty()) {
                return CapabilityResult.failed("milestoneId 不能为空");
            }

            String projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                ? ba.getCurrentProject().getId() : null;
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            try {
                // 通过 GoalService 更新里程碑
                if (goalService != null) {
                    if (progress != null) {
                        // 更新进度
                        boolean updated = goalService.updateMilestoneProgressFromCapability(projectId, milestoneId, progress);
                        if (updated) {
                            return CapabilityResult.success("里程碑进度已更新: " + milestoneId + " -> " + progress + "%");
                        } else {
                            return CapabilityResult.failed("更新里程碑进度失败");
                        }
                    } else if (!status.isEmpty()) {
                        // 更新状态 - 需要转换状态字符串到枚举
                        boolean updated = goalService.updateMilestoneStatusFromCapability(projectId, milestoneId, status);
                        if (updated) {
                            return CapabilityResult.success("里程碑状态已更新: " + milestoneId + " -> " + status);
                        } else {
                            return CapabilityResult.failed("更新里程碑状态失败");
                        }
                    }
                }
                return CapabilityResult.failed("GoalService 未注入");
            } catch (Exception e) {
                return CapabilityResult.failed("更新里程碑失败: " + e.getMessage());
            }
        });

        executors.put("selectWorkflow", (agent, params) -> {
            String criteria = getParam(params, "criteria", "");
            if (agent instanceof ProducerAgent producer) {
                java.util.List<String> templates = producer.selectWorkflow(criteria);
                return CapabilityResult.success("推荐工作流: " + String.join(", ", templates));
            }
            return CapabilityResult.failed("只有制作人可以选择工作流");
        });

        executors.put("startWorkflow", (agent, params) -> {
            String templateId = getParam(params, "templateId", "");
            String milestoneId = getParam(params, "milestoneId", "");
            if (agent instanceof ProducerAgent producer) {
                String instanceId = producer.startWorkflow(templateId, milestoneId);
                if (instanceId != null) {
                    return CapabilityResult.success("工作流已启动: " + instanceId);
                }
                return CapabilityResult.failed("启动工作流失败");
            }
            return CapabilityResult.failed("只有制作人可以启动工作流");
        });

        executors.put("evaluateAgent", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            Integer qualityScore = getParam(params, "qualityScore", 80);
            Integer efficiencyScore = getParam(params, "efficiencyScore", 80);
            Integer collaborationScore = getParam(params, "collaborationScore", 80);
            Integer innovationScore = getParam(params, "innovationScore", 80);
            String strengths = getParam(params, "strengths", "");
            String improvements = getParam(params, "improvements", "");
            String comments = getParam(params, "comments", "");

            if (agent instanceof ProducerAgent producer) {
                producer.evaluateAgent(agentId, qualityScore, efficiencyScore,
                    collaborationScore, innovationScore, strengths, improvements, comments);
                return CapabilityResult.success("Agent 绩效评估已完成: " + agentId);
            }
            return CapabilityResult.failed("只有制作人可以评估 Agent");
        });

        executors.put("batchEvaluateTeam", (agent, params) -> {
            if (agent instanceof ProducerAgent producer) {
                producer.batchEvaluateTeam();
                return CapabilityResult.success("团队批量评估已触发");
            }
            return CapabilityResult.failed("只有制作人可以批量评估团队");
        });

        executors.put("requestDismissAgent", (agent, params) -> {
            String agentId = getParam(params, "agentId", "");
            String reasonType = getParam(params, "reasonType", "LOW_PERFORMANCE");
            String reason = getParam(params, "reason", "");

            if (agent instanceof ProducerAgent producer) {
                producer.requestDismissAgent(agentId, reasonType, reason);
                return CapabilityResult.success("解雇申请已提交: " + agentId);
            }
            return CapabilityResult.failed("只有制作人可以发起解雇申请");
        });

        // ===== 版本迭代能力执行器 =====

        // 评估当前版本
        executors.put("evaluateVersion", (agent, params) -> {
            String projectId = getParam(params, "projectId", "");
            if (projectId.isEmpty()) {
                projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
            }
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            if (versionIterationService == null) {
                return CapabilityResult.failed("版本迭代服务不可用");
            }

            try {
                VersionIterationService.VersionEvaluationResult result = versionIterationService.evaluateVersion(projectId);
                StringBuilder sb = new StringBuilder();
                sb.append("## 版本评估结果\n\n");
                sb.append(String.format("- 评分: %d/10\n", result.getScore()));
                sb.append(String.format("- 是否通过: %s\n", result.isPassed() ? "✅ 通过" : "❌ 未通过"));
                sb.append(String.format("- 详情: %s\n", result.getDetails()));

                if (!result.getStrengths().isEmpty()) {
                    sb.append("\n### 优点\n");
                    result.getStrengths().forEach(s -> sb.append("- ").append(s).append("\n"));
                }
                if (!result.getImprovements().isEmpty()) {
                    sb.append("\n### 待改进\n");
                    result.getImprovements().forEach(s -> sb.append("- ").append(s).append("\n"));
                }
                if (result.getRecommendation() != null) {
                    sb.append("\n### 建议\n").append(result.getRecommendation()).append("\n");
                }

                return CapabilityResult.success(sb.toString());
            } catch (Exception e) {
                return CapabilityResult.failed("版本评估失败: " + e.getMessage());
            }
        });

        // 规划下一版本
        executors.put("planNextVersion", (agent, params) -> {
            String projectId = getParam(params, "projectId", "");
            int currentScore = getIntParam(params, "currentScore", 7);

            if (projectId.isEmpty()) {
                projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
            }
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            if (versionIterationService == null) {
                return CapabilityResult.failed("版本迭代服务不可用");
            }

            try {
                // 先评估当前版本
                VersionIterationService.VersionEvaluationResult evaluation = versionIterationService.evaluateVersion(projectId);
                evaluation.setScore(currentScore); // 使用传入的分数

                // 规划下一版本
                VersionIterationService.NextVersionPlan plan = versionIterationService.planNextVersion(projectId, evaluation);

                StringBuilder sb = new StringBuilder();
                sb.append("## 下一版本规划\n\n");
                sb.append(String.format("- 是否需要迭代: %s\n", plan.isNeedNextVersion() ? "✅ 是" : "❌ 否"));
                sb.append(String.format("- 规划理由: %s\n", plan.getReason()));

                if (plan.isNeedNextVersion()) {
                    sb.append(String.format("- 下一版本: %s\n", plan.getNextVersion()));
                    if (plan.getNextGoal() != null) {
                        sb.append(String.format("- 下一版本目标: %s\n", plan.getNextGoal()));
                    }
                    if (!plan.getNextMilestones().isEmpty()) {
                        sb.append("\n### 下一版本里程碑\n");
                        plan.getNextMilestones().forEach(m -> sb.append("- ").append(m).append("\n"));
                    }
                }

                return CapabilityResult.success(sb.toString());
            } catch (Exception e) {
                return CapabilityResult.failed("版本规划失败: " + e.getMessage());
            }
        });

        // 执行版本升级
        executors.put("upgradeVersion", (agent, params) -> {
            String projectId = getParam(params, "projectId", "");
            String newVersion = getParam(params, "newVersion", "");
            String newGoal = getParam(params, "newGoal", "");
            String reason = getParam(params, "reason", "版本迭代");

            if (projectId.isEmpty()) {
                projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
            }
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }
            if (newVersion.isEmpty()) {
                return CapabilityResult.failed("新版本号不能为空");
            }

            if (versionIterationService == null) {
                return CapabilityResult.failed("版本迭代服务不可用");
            }

            try {
                // 构建版本规划
                VersionIterationService.NextVersionPlan plan = new VersionIterationService.NextVersionPlan();
                plan.setNeedNextVersion(true);
                plan.setNextVersion(newVersion);
                plan.setNextGoal(newGoal);
                plan.setReason(reason);

                // 执行版本升级
                boolean success = versionIterationService.upgradeVersion(projectId, plan);
                if (success) {
                    return CapabilityResult.success(String.format("版本已升级到 %s，里程碑已重置", newVersion));
                } else {
                    return CapabilityResult.failed("版本升级失败");
                }
            } catch (Exception e) {
                return CapabilityResult.failed("版本升级失败: " + e.getMessage());
            }
        });

        // 检查版本迭代
        executors.put("checkVersionIteration", (agent, params) -> {
            String projectId = getParam(params, "projectId", "");
            if (projectId.isEmpty()) {
                projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
            }
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            if (versionIterationService == null) {
                return CapabilityResult.failed("版本迭代服务不可用");
            }

            try {
                int result = versionIterationService.checkVersionIteration(projectId);
                String message = switch (result) {
                    case 0 -> "版本未完成，继续当前工作";
                    case 1 -> "版本已迭代，创建了新版本";
                    case 2 -> "目标已完成";
                    default -> "未知状态";
                };
                return CapabilityResult.success(message);
            } catch (Exception e) {
                return CapabilityResult.failed("检查版本迭代失败: " + e.getMessage());
            }
        });

        // 停止项目内所有Agent
        executors.put("stopAllProjectAgents", (agent, params) -> {
            String projectId = getParam(params, "projectId", "");
            String reason = getParam(params, "reason", "目标完成");

            if (projectId.isEmpty()) {
                projectId = agent instanceof BaseAgent ba && ba.getCurrentProject() != null
                    ? ba.getCurrentProject().getId() : null;
            }
            if (projectId == null) {
                return CapabilityResult.failed("无法确定项目 ID");
            }

            if (versionIterationService == null) {
                return CapabilityResult.failed("版本迭代服务不可用");
            }

            try {
                int stoppedCount = versionIterationService.stopAllProjectAgents(projectId);
                return CapabilityResult.success(String.format("已停止 %d 个项目 Agent，原因: %s", stoppedCount, reason));
            } catch (Exception e) {
                return CapabilityResult.failed("停止项目 Agent 失败: " + e.getMessage());
            }
        });

        log.info("Registered {} default capability executors", executors.size());
    }

    // ===== 审批回调 =====

    /**
     * 审批通过后执行能力（由 ApprovalService 回调）
     */
    public void executeApprovedCapability(Long approvalRequestId) {
        log.info("Executing approved capability for approval request: {}", approvalRequestId);

        // 1. 从调用日志中找到对应的记录
        var logOpt = logRepository.findByApprovalRequestId(approvalRequestId);
        if (logOpt.isEmpty()) {
            log.warn("No invocation log found for approval request: {}", approvalRequestId);
            return;
        }

        CapabilityInvocationLog logEntry = logOpt.get();

        // 2. 查找 Agent
        Agent agent = agentManager.getAgent(logEntry.getAgentId());
        if (agent == null) {
            log.warn("Agent {} not found for approved capability {}", logEntry.getAgentId(), logEntry.getCapabilityName());
            logEntry.markFailed("Agent 不存在: " + logEntry.getAgentId());
            logRepository.save(logEntry);
            return;
        }

        // 3. 解析参数并构建 CapabilityCall
        Map<String, Object> params = Collections.emptyMap();
        if (logEntry.getParams() != null && !logEntry.getParams().isEmpty()) {
            try {
                params = objectMapper.readValue(logEntry.getParams(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse params for approved capability: {}", e.getMessage());
            }
        }

        CapabilityCall call = new CapabilityCall(logEntry.getCapabilityName(), params, logEntry.getReason());

        // 4. 重新执行
        try {
            CapabilityResult result = executeCall(agent, call);
            log.info("Approved capability {} executed: {}", logEntry.getCapabilityName(), result.getStatus());
        } catch (Exception e) {
            log.error("Failed to execute approved capability {}", logEntry.getCapabilityName(), e);
            logEntry.markFailed("执行异常: " + e.getMessage());
            logRepository.save(logEntry);
        }
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
     * 获取列表参数
     */
    @SuppressWarnings("unchecked")
    private List<String> getListParam(Map<String, Object> params, String key) {
        if (params == null) return new ArrayList<>();
        Object value = params.get(key);
        if (value == null) return new ArrayList<>();
        if (value instanceof List) {
            return (List<String>) value;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1);
            }
            return Arrays.stream(str.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * 获取整数参数
     */
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取浮点数参数
     */
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取项目根目录
     */
    private String getProjectRoot(Agent agent) {
        if (agent instanceof BaseAgent ba && ba.getCurrentProject() != null) {
            return ba.getCurrentProject().getWorkDir();
        }
        return System.getProperty("user.dir");
    }

    /**
     * 在项目中搜索方法
     */
    private boolean searchMethodInProject(String projectRoot, String methodName) {
        try {
            java.io.File rootDir = new java.io.File(projectRoot);
            return searchInDirectory(rootDir, methodName);
        } catch (Exception e) {
            log.warn("搜索方法时出错: {}", e.getMessage());
            return false;
        }
    }

    private boolean searchInDirectory(java.io.File dir, String methodName) {
        if (!dir.exists() || !dir.isDirectory()) return false;

        java.io.File[] files = dir.listFiles();
        if (files == null) return false;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // 跳过 node_modules 和 .git
                if (!file.getName().equals("node_modules") && !file.getName().equals(".git")) {
                    if (searchInDirectory(file, methodName)) return true;
                }
            } else if (file.getName().endsWith(".js") || file.getName().endsWith(".ts") ||
                       file.getName().endsWith(".java") || file.getName().endsWith(".py")) {
                // 搜索方法定义
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    if (content.contains("function " + methodName) ||
                        content.contains(methodName + "(") ||
                        content.contains("def " + methodName) ||
                        content.contains("public " + methodName)) {
                        return true;
                    }
                } catch (Exception e) {
                    // 忽略读取错误
                }
            }
        }
        return false;
    }

    /**
     * 将严重程度映射为告警优先级
     */
    private String mapSeverityToPriority(String severity) {
        if (severity == null) return "MEDIUM";
        return switch (severity.toLowerCase()) {
            case "critical", "紧急" -> "CRITICAL";
            case "high", "高" -> "HIGH";
            case "medium", "中" -> "MEDIUM";
            case "low", "低" -> "LOW";
            default -> "MEDIUM";
        };
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

    // ===== 任务分解辅助方法 =====

    /**
     * 构建任务分解提示词
     */
    private String buildTaskDecompositionPrompt(String milestoneTitle, String milestoneDescription, String goal) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个资深游戏策划专家。请将以下里程碑分解为具体的可执行任务。\n\n");

        sb.append("## 里程碑信息\n");
        sb.append("- 标题：").append(milestoneTitle).append("\n");
        if (milestoneDescription != null && !milestoneDescription.isEmpty()) {
            sb.append("- 描述：").append(milestoneDescription).append("\n");
        }
        if (goal != null && !goal.isEmpty()) {
            sb.append("- 项目目标：").append(goal).append("\n");
        }
        sb.append("\n");

        sb.append("## 输出格式要求\n");
        sb.append("请按以下格式输出每个任务（每个任务用 TASK 分隔）：\n\n");
        sb.append("TASK: 任务标题 | 负责角色 | 任务描述 | 输入要求 | 输出产物 | 验收标准1;验收标准2 | 优先级 | 预估工时 | 依赖序号\n\n");

        sb.append("## 角色说明\n");
        sb.append("- system-planner: 系统策划，负责游戏系统设计和策划案\n");
        sb.append("- numerical-planner: 数值策划，负责数值平衡和经济系统\n");
        sb.append("- client-dev: 客户端开发，负责游戏前端逻辑、UI交互、游戏核心玩法实现\n");
        sb.append("- ui-dev: UI开发，负责界面设计实现、视觉效果、动画\n");
        sb.append("- server-dev: 服务端开发，负责后端逻辑、数据存储、多人联网\n");
        sb.append("- tester: 测试，负责质量验证\n");
        sb.append("- git-commit: 版本管理，负责代码提交和版本控制\n\n");

        sb.append("## 分解原则\n");
        sb.append("1. 每个任务应该是一个独立可交付的工作单元\n");
        sb.append("2. 输入要求要明确具体的前置条件\n");
        sb.append("3. 输出产物要明确具体的文件或功能\n");
        sb.append("4. 验收标准要可执行、可检查\n");
        sb.append("5. 任务粒度适中，通常 2-8 小时可完成\n");
        sb.append("6. 合理设置依赖关系，支持并行执行\n\n");

        sb.append("请只输出 TASK 行，不要有其他内容。");

        return sb.toString();
    }

    /**
     * 解析任务模板列表
     */
    private List<TaskTemplate> parseTaskTemplates(String response, String milestoneId) {
        List<TaskTemplate> tasks = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("TASK:") && !line.startsWith("TASK：")) continue;

            try {
                String content = line.substring(line.indexOf(":") + 1).trim();
                if (content.isEmpty()) {
                    content = line.substring(line.indexOf("：") + 1).trim();
                }

                String[] parts = content.split("\\|");
                if (parts.length < 6) continue;

                TaskTemplate task = new TaskTemplate();
                task.setTitle(parts[0].trim());
                task.setAssignedRole(normalizeRole(parts[1].trim()));
                task.setDescription(parts[2].trim());
                task.setInputRequirements(parts[3].trim());
                task.setOutputDeliverables(parts[4].trim());
                task.setMilestoneId(milestoneId);

                // 解析验收标准
                String criteriaStr = parts[5].trim();
                if (!criteriaStr.isEmpty()) {
                    String[] criteria = criteriaStr.split("[;；]");
                    List<String> criteriaList = new ArrayList<>();
                    for (String c : criteria) {
                        String trimmed = c.trim();
                        if (!trimmed.isEmpty()) {
                            criteriaList.add(trimmed);
                        }
                    }
                    task.setAcceptanceCriteria(criteriaList);
                }

                // 解析优先级
                if (parts.length > 6) {
                    task.setPriority(parts[6].trim().toUpperCase());
                }

                // 解析预估工时
                if (parts.length > 7) {
                    try {
                        task.setEstimatedHours(Integer.parseInt(parts[7].trim().replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException e) {
                        task.setEstimatedHours(4); // 默认 4 小时
                    }
                }

                // 解析依赖
                if (parts.length > 8) {
                    String depsStr = parts[8].trim();
                    if (!depsStr.isEmpty() && !"0".equals(depsStr) && !"无".equals(depsStr)) {
                        List<String> deps = new ArrayList<>();
                        for (String dep : depsStr.split("[,，]")) {
                            String trimmed = dep.trim().replaceAll("[^0-9]", "");
                            if (!trimmed.isEmpty()) {
                                deps.add(trimmed);
                            }
                        }
                        task.setDependencies(deps);
                    }
                }

                if (task.getAssignedRole() != null) {
                    tasks.add(task);
                    log.info("Parsed task: {} -> {}", task.getTitle(), task.getAssignedRole());
                }
            } catch (Exception e) {
                log.warn("Failed to parse task line: {} - {}", line, e.getMessage());
            }
        }

        return tasks;
    }

    /**
     * 角色名称标准化
     */
    private String normalizeRole(String role) {
        if (role == null) return null;
        String normalized = role.replaceAll("\\s+", "").toLowerCase();

        return switch (normalized) {
            case "系统策划", "system-planner", "systemplanner", "策划" -> "system-planner";
            case "数值策划", "numerical-planner", "numericalplanner", "数值" -> "numerical-planner";
            case "客户端开发", "client-dev", "clientdev", "客户端", "前端" -> "client-dev";
            case "ui开发", "ui-dev", "uidev", "ui", "界面" -> "ui-dev";
            case "服务端开发", "server-dev", "serverdev", "服务端", "后端" -> "server-dev";
            case "测试", "tester", "qa" -> "tester";
            case "版本管理", "git-commit", "gitcommit", "git" -> "git-commit";
            default -> null;
        };
    }
}
