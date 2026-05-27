package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ProducerAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ProducerAgent.class);

    private final AgentManager agentManager;
    private final FeishuBotService feishuService;

    public ProducerAgent(AgentDefinition definition,
                        ClaudeCliEngine cliEngine,
                        MessageBus messageBus,
                        ContextManager contextManager,
                        MemoryManager memoryManager,
                        SkillManager skillManager,
                        ProjectManager projectManager,
                        AgentManager agentManager,
                        FeishuBotService feishuService) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        this.agentManager = agentManager;
        this.feishuService = feishuService;
    }

    @Override
    protected void doWork() {
        log.info("Producer working...");

        String teamStatus = getTeamStatus();
        processApprovals();
        generateWorkInstructions();
        reportToUser(teamStatus);

        saveProjectStatus(teamStatus);
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case REPORT -> handleReport(message);
            case QUERY -> handleQuery(message);
            case APPROVAL -> handleApprovalRequest(message);
            case RESPONSE -> handleResponse(message);
            default -> log.info("Producer received message: {}", message.getType());
        }
    }

    private String getTeamStatus() {
        List<Agent> agents = agentManager.getAllAgents();
        StringBuilder sb = new StringBuilder("**团队状态报告**\n\n");

        if (currentProject != null) {
            sb.append("**项目**: ").append(currentProject.getName()).append("\n");
            sb.append("**工作目录**: ").append(currentProject.getWorkDir()).append("\n\n");
        }

        for (Agent agent : agents) {
            sb.append(String.format("- **%s** (%s): %s\n",
                agent.getName(),
                agent.getRole(),
                agent.isBusy() ? "忙碌" : "空闲"));
        }

        return sb.toString();
    }

    private void processApprovals() {
        List<AgentMessage> approvals = pendingMessages.stream()
            .filter(m -> m.getType() == AgentMessage.MessageType.APPROVAL)
            .collect(Collectors.toList());

        for (AgentMessage approval : approvals) {
            feishuService.sendApprovalRequest(approval.getContent());
        }
    }

    private void generateWorkInstructions() {
        log.info("Generating work instructions...");
        String skillPrompt = buildSkillPrompt("制定工作计划和任务分配");
        if (!skillPrompt.isEmpty()) {
            String instructions = sendMessage(skillPrompt);
            saveKnowledge("work_instructions", instructions);
        }
    }

    private void reportToUser(String status) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(status);
        }
    }

    private void saveProjectStatus(String status) {
        agentContext.setProjectSummary(status);
        saveContext();
    }

    private void handleReport(AgentMessage message) {
        log.info("Received report from {}: {}", message.getFromAgentId(), message.getContent());
        saveMemory("last_report_" + message.getFromAgentId(), message.getContent());
        saveKnowledge("team_report_" + System.currentTimeMillis(), message.getContent());
    }

    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());
        String response = processQuery(message.getContent());
        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);
    }

    private void handleApprovalRequest(AgentMessage message) {
        log.info("Approval request from {}: {}", message.getFromAgentId(), message.getContent());
        feishuService.sendApprovalRequest(
            String.format("来自 %s 的审批请求:\n%s",
                message.getFromAgentId(),
                message.getContent())
        );
    }

    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
    }

    private String processQuery(String query) {
        String skillPrompt = buildSkillPrompt("回答问题和查询");
        String fullPrompt = skillPrompt + "\n\n请回答以下问题: " + query;
        return sendMessage(fullPrompt);
    }

    public String requestHiring(String role, String requirements) {
        String hiringPlan = String.format(
            "**招聘请求**\n\n" +
            "角色: %s\n" +
            "要求:\n%s\n\n" +
            "请确认是否批准此次招聘。",
            role, requirements
        );

        feishuService.sendApprovalRequest(hiringPlan);
        saveExperience("hiring_" + role, hiringPlan);
        return "招聘请求已发送，等待批准";
    }

    public Agent createAgent(String name, String role, String agentsFile, String workDir) {
        AgentDefinition newDef = AgentDefinition.builder()
            .id(role.toLowerCase().replace(" ", "-") + "-" + System.currentTimeMillis())
            .name(name)
            .role(role)
            .agentsFile(agentsFile)
            .workDir(workDir)
            .status(AgentDefinition.AgentStatus.IDLE)
            .build();

        saveExperience("create_agent_" + newDef.getId(),
            String.format("Created agent: %s (%s) for workDir: %s", name, role, workDir));

        return agentManager.createAgent(newDef);
    }

    public void assignApiConfig(String agentId, String apiKey, String apiUrl, String model) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            agent.getDefinition().setApiKey(apiKey);
            agent.getDefinition().setApiUrl(apiUrl);
            agent.getDefinition().setModel(model);

            agentContext.addApiConfig(apiUrl, model);
            agent.saveContext();

            log.info("Assigned API config to agent: {}", agentId);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.recoverContext();
            }
        }
    }

    public void assignWorkDir(String agentId, String workDir) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            agent.getDefinition().setWorkDir(workDir);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.initProject();
                baseAgent.loadProjectSkills();
                baseAgent.recoverContext();
            }

            agent.saveContext();
            log.info("Assigned workDir to agent: {} -> {}", agentId, workDir);
        }
    }

    public void notifyQuotaExhausted(String agentId) {
        feishuService.sendMessage(
            String.format("⚠️ Agent %s 的 API 配额不足，请及时更换或重置。", agentId)
        );
        saveExperience("quota_exhausted_" + agentId, "API quota exhausted at " + java.time.LocalDateTime.now());
    }
}
