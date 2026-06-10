package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.TaskAssignment;
import com.chengxun.gamemaker.web.entity.*;
import com.chengxun.gamemaker.web.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 工作流引擎（重构版）
 * 基于事件驱动的工作流执行引擎，支持真正并行、审批、超时、重试、持久化
 *
 * 核心设计：
 * 1. 事件驱动：通过Spring ApplicationEventPublisher发布步骤完成/失败事件，替代轮询循环
 * 2. 真正并行：使用CompletableFuture.allOf()并行执行标记为parallel的步骤
 * 3. 审批机制：需要审批的步骤暂停等待，通过API审批/拒绝后继续
 * 4. 超时机制：每个步骤支持可配置超时，使用CompletableFuture.orTimeout实现
 * 5. 重试机制：步骤失败后自动重试，直到达到最大重试次数
 * 6. 实例持久化：所有状态写入数据库，支持崩溃恢复
 * 7. Agent匹配：通过AgentMatchStrategy综合评分选择最优Agent
 * 8. 数据传递：步骤输出数据自动传递给下游依赖步骤
 * 9. 审计日志：所有关键操作记录审计日志
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowStepExecutionRepository stepExecutionRepository;

    @Autowired
    private WorkflowApprovalRepository approvalRepository;

    @Autowired
    private WorkflowAuditService auditService;

    @Autowired
    private AgentMatchStrategy agentMatchStrategy;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.manager.ProjectManager projectManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 工作流模板（内存缓存） */
    private final Map<String, WorkflowTemplate> templates = new ConcurrentHashMap<>();

    /** 默认步骤超时时间（分钟），可通过系统配置覆盖 */
    private int defaultStepTimeoutMinutes = 60;

    /** 默认最大重试次数，可通过系统配置覆盖 */
    private int defaultMaxRetries = 3;

    /** 运行中的工作流实例ID集合（防止重复启动） */
    private final Set<String> runningInstanceIds = ConcurrentHashMap.newKeySet();

    /** 工作流实例内存缓存（支持快速查询） */
    private final Map<String, WorkflowInstance> instanceCache = new ConcurrentHashMap<>();

    /** 线程池，用于并行执行任务 */
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // ===== 事件类 =====

    /** 步骤完成事件 */
    public static class StepCompletedEvent {
        private final String instanceId;
        private final String stepId;
        private final String result;
        private final String outputData;

        public StepCompletedEvent(String instanceId, String stepId, String result, String outputData) {
            this.instanceId = instanceId;
            this.stepId = stepId;
            this.result = result;
            this.outputData = outputData;
        }

        public String getInstanceId() { return instanceId; }
        public String getStepId() { return stepId; }
        public String getResult() { return result; }
        public String getOutputData() { return outputData; }
    }

    /** 步骤失败事件 */
    public static class StepFailedEvent {
        private final String instanceId;
        private final String stepId;
        private final String error;

        public StepFailedEvent(String instanceId, String stepId, String error) {
            this.instanceId = instanceId;
            this.stepId = stepId;
            this.error = error;
        }

        public String getInstanceId() { return instanceId; }
        public String getStepId() { return stepId; }
        public String getError() { return error; }
    }

    /** 审批请求事件 */
    public static class ApprovalRequiredEvent {
        private final String instanceId;
        private final String stepId;

        public ApprovalRequiredEvent(String instanceId, String stepId) {
            this.instanceId = instanceId;
            this.stepId = stepId;
        }

        public String getInstanceId() { return instanceId; }
        public String getStepId() { return stepId; }
    }

    // ===== 初始化 =====

    @PostConstruct
    public void init() {
        // 加载系统配置
        loadSystemConfig();

        // ===== 1. 标准游戏开发流程 =====
        // 完整的游戏开发流程：系统策划→数值审批→技术评审→服务端设计→三方并行开发→自测验收→测试→bug修复→回归→部署
        WorkflowTemplate standard = new WorkflowTemplate("standard-game-dev", "标准游戏开发流程",
            "完整的游戏开发流程，包含策划审批、技术评审、并行开发、测试验收、bug修复和部署。适用于有客户端和服务端的完整项目");

        // 步骤1：系统策划 — 输出策划案
        WorkflowStep planStep = new WorkflowStep("plan", "系统策划", "system-planner",
            "分析游戏需求，撰写完整的系统策划案，包括核心玩法、系统架构、功能模块划分、交互流程和数据模型设计。输出策划文档供数值策划审批");

        // 步骤2：数值策划审批 — 审查策划案的数值可行性和平衡性
        WorkflowStep numericalReview = new WorkflowStep("numerical-review", "数值策划审批", "numerical-planner",
            "审查系统策划案的数值可行性、平衡性和完整性。检查数值框架是否合理、成长曲线是否平滑、经济系统是否平衡。审批通过后进入技术评审，不通过则打回修改");
        numericalReview.addDependency("plan");
        numericalReview.setRequiresApproval(true);
        numericalReview.setImportance("HIGH");  // 数值策划审批是高重要程度

        // 步骤3：技术评审 — 服务端和客户端并行评审
        WorkflowStep serverTechReview = new WorkflowStep("server-tech-review", "服务端技术评审", "server-dev",
            "从服务端角度评审策划案：评估技术可行性、性能瓶颈、数据库设计难度、接口复杂度。输出技术评审意见和风险点");
        serverTechReview.addDependency("numerical-review");
        serverTechReview.setParallel(true);

        WorkflowStep clientTechReview = new WorkflowStep("client-tech-review", "客户端技术评审", "client-dev",
            "从客户端角度评审策划案：评估前端实现难度、渲染性能、交互体验、兼容性问题。输出技术评审意见和风险点");
        clientTechReview.addDependency("numerical-review");
        clientTechReview.setParallel(true);

        // 步骤4：服务端设计 — 输出接口文档和配置表
        WorkflowStep serverDesign = new WorkflowStep("server-design", "服务端设计", "server-dev",
            "根据策划案和技术评审意见，完成以下设计：\n" +
            "1. 数据库设计：用户表、角色表、背包表等核心业务表结构\n" +
            "2. 配置表设计：装备配置、技能配置、关卡配置等策划配置表\n" +
            "3. 接口文档：RESTful API 接口定义，包含请求/响应格式、错误码\n" +
            "输出：数据库DDL、配置表模板、接口文档（Swagger格式）\n" +
            "接口文档传递给客户端开发，配置表传递给数值策划");
        serverDesign.addDependency("server-tech-review");
        serverDesign.addDependency("client-tech-review");

        // 步骤5：三方并行开发
        WorkflowStep devServer = new WorkflowStep("dev-server", "服务端开发", "server-dev",
            "根据接口文档和数据库设计，实现服务端业务逻辑：\n" +
            "1. 实现数据库表和ORM映射\n" +
            "2. 实现 RESTful API 接口\n" +
            "3. 实现核心业务逻辑（战斗、背包、任务等）\n" +
            "4. 编写单元测试\n" +
            "5. 输出接口联调文档给客户端");
        devServer.addDependency("server-design");
        devServer.setParallel(true);

        WorkflowStep devClient = new WorkflowStep("dev-client", "客户端开发", "client-dev",
            "根据接口文档和策划案，实现客户端功能：\n" +
            "1. 实现 UI 界面和交互逻辑\n" +
            "2. 对接服务端 API 接口\n" +
            "3. 实现游戏核心玩法的前端逻辑\n" +
            "4. 实现动画、音效等多媒体资源集成\n" +
            "5. 编写自动化测试脚本");
        devClient.addDependency("server-design");
        devClient.setParallel(true);

        WorkflowStep devNumerical = new WorkflowStep("dev-numerical", "数值配置", "numerical-planner",
            "根据配置表模板和策划案，完成数值配置：\n" +
            "1. 填写装备、技能、怪物等配置表数据\n" +
            "2. 设计数值成长曲线和平衡参数\n" +
            "3. 配置关卡难度和奖励数值\n" +
            "4. 输出数值配置文件供服务端加载");
        devNumerical.addDependency("server-design");
        devNumerical.setParallel(true);

        // 步骤6：自测 — 三方各自验证
        WorkflowStep serverSelfTest = new WorkflowStep("server-self-test", "服务端自测", "server-dev",
            "服务端自测：验证API接口正确性、数据一致性、性能指标。确保所有接口可正常调用，无明显bug");
        serverSelfTest.addDependency("dev-server");
        serverSelfTest.setParallel(true);

        WorkflowStep clientSelfTest = new WorkflowStep("client-self-test", "客户端自测", "client-dev",
            "客户端自测：验证UI展示正确性、交互流畅性、接口对接无误。确保所有页面可正常访问和操作");
        clientSelfTest.addDependency("dev-client");
        clientSelfTest.setParallel(true);

        WorkflowStep numericalSelfTest = new WorkflowStep("numerical-self-test", "数值自测", "numerical-planner",
            "数值自测：验证配置表数据正确性、数值平衡性、成长曲线合理性。确保配置表可正确加载，数值无明显异常");
        numericalSelfTest.addDependency("dev-numerical");
        numericalSelfTest.setParallel(true);

        // 步骤7：策划验收 — 系统策划确认功能完整性
        WorkflowStep acceptance = new WorkflowStep("acceptance", "策划验收", "system-planner",
            "系统策划验收：对照策划案逐项检查功能实现完整性，确认核心玩法、系统逻辑、交互流程是否符合设计预期。验收不通过则记录问题并打回对应开发方修改");
        acceptance.addDependency("server-self-test");
        acceptance.addDependency("client-self-test");
        acceptance.addDependency("numerical-self-test");
        acceptance.setRequiresApproval(true);
        acceptance.setImportance("HIGH");  // 策划验收是高重要程度

        // 步骤8：测试 — 测试团队全面测试
        WorkflowStep testing = new WorkflowStep("test", "测试验证", "tester",
            "全面测试：\n" +
            "1. 功能测试：验证所有功能模块是否正常工作\n" +
            "2. 集成测试：验证前后端联调是否正确\n" +
            "3. 性能测试：验证响应时间、并发能力、资源占用\n" +
            "4. 兼容性测试：验证不同设备和浏览器的兼容性\n" +
            "5. 记录所有bug并分配给对应开发方（服务端/客户端/数值）\n" +
            "测试不通过则进入bug修复阶段");
        testing.addDependency("acceptance");

        // 步骤9：Bug修复 — 三方并行修复
        WorkflowStep fixServer = new WorkflowStep("fix-server", "服务端Bug修复", "server-dev",
            "修复测试阶段发现的服务端bug，确保修复后不影响其他功能");
        fixServer.addDependency("test");
        fixServer.setParallel(true);

        WorkflowStep fixClient = new WorkflowStep("fix-client", "客户端Bug修复", "client-dev",
            "修复测试阶段发现的客户端bug，确保修复后不影响其他功能");
        fixClient.addDependency("test");
        fixClient.setParallel(true);

        WorkflowStep fixNumerical = new WorkflowStep("fix-numerical", "数值Bug修复", "numerical-planner",
            "修复测试阶段发现的数值配置bug，调整不合理的数值参数");
        fixNumerical.addDependency("test");
        fixNumerical.setParallel(true);

        // 步骤10：回归测试 — 验证bug修复
        WorkflowStep regression = new WorkflowStep("regression", "回归测试", "tester",
            "回归测试：重新验证所有已修复的bug，确认修复有效且未引入新问题。执行核心功能回归测试套件，确保系统稳定性");
        regression.addDependency("fix-server");
        regression.addDependency("fix-client");
        regression.addDependency("fix-numerical");

        // 步骤11：部署上线
        WorkflowStep deployStep = new WorkflowStep("deploy", "部署上线", "git-commit",
            "合并所有代码分支，执行构建流水线，部署到生产环境。进行线上冒烟测试，确认服务正常运行");
        deployStep.addDependency("regression");
        deployStep.setRequiresApproval(true);
        deployStep.setImportance("CRITICAL");  // 部署上线是最高重要程度

        standard.addStep(planStep);
        standard.addStep(numericalReview);
        standard.addStep(serverTechReview);
        standard.addStep(clientTechReview);
        standard.addStep(serverDesign);
        standard.addStep(devServer);
        standard.addStep(devClient);
        standard.addStep(devNumerical);
        standard.addStep(serverSelfTest);
        standard.addStep(clientSelfTest);
        standard.addStep(numericalSelfTest);
        standard.addStep(acceptance);
        standard.addStep(testing);
        standard.addStep(fixServer);
        standard.addStep(fixClient);
        standard.addStep(fixNumerical);
        standard.addStep(regression);
        standard.addStep(deployStep);

        // ===== 2. 服务端开发流程 =====
        WorkflowTemplate serverOnly = new WorkflowTemplate("server-only-dev", "服务端开发流程", "纯后端项目开发流程，适用于API服务、微服务、后台管理系统等无客户端的项目");
        WorkflowStep soPlan = new WorkflowStep("plan", "系统策划", "system-planner", "分析需求，设计API接口、数据库模型和系统架构");
        WorkflowStep soDev = new WorkflowStep("dev-server", "服务端开发", "server-dev", "实现业务逻辑、API接口、数据库设计和单元测试");
        soDev.addDependency("plan");
        WorkflowStep soTest = new WorkflowStep("test", "测试验证", "tester", "执行接口测试、集成测试和性能测试。如测试失败，需记录问题并通知服务端开发Agent修复后重新测试");
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

        // ===== 3. 客户端开发流程 =====
        WorkflowTemplate clientOnly = new WorkflowTemplate("client-only-dev", "客户端开发流程", "纯前端项目开发流程，适用于H5游戏、小程序、可视化页面等无服务端的项目");
        WorkflowStep coPlan = new WorkflowStep("plan", "交互策划", "system-planner", "分析需求，设计页面结构、交互流程和视觉规范");
        WorkflowStep coDesign = new WorkflowStep("design", "UI开发", "ui-dev", "实现页面布局、组件设计和样式开发");
        coDesign.addDependency("plan");
        WorkflowStep coDev = new WorkflowStep("dev-client", "功能开发", "client-dev", "实现交互逻辑、数据绑定和动画效果");
        coDev.addDependency("design");
        WorkflowStep coTest = new WorkflowStep("test", "测试验证", "tester", "执行功能测试、兼容性测试和用户体验测试");
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
        WorkflowStep rapidTest = new WorkflowStep("test", "快速测试", "tester", "验证核心功能是否可用，记录明显Bug");
        rapidTest.addDependency("dev");
        rapid.addStep(rapidPlan);
        rapid.addStep(rapidDev);
        rapid.addStep(rapidTest);

        // ===== 5. 紧急修复流程 =====
        WorkflowTemplate hotfix = new WorkflowTemplate("hotfix", "紧急修复流程", "线上问题快速修复流程，精简环节优先恢复服务");
        WorkflowStep hfAnalyze = new WorkflowStep("analyze", "问题分析", "system-planner", "分析线上问题根因，确定影响范围和修复方案");
        WorkflowStep hfFix = new WorkflowStep("fix", "紧急修复", "server-dev", "实施最小化修复，不做无关改动");
        hfFix.addDependency("analyze");
        WorkflowStep hfTest = new WorkflowStep("test", "验证测试", "tester", "验证修复是否生效，确认无回归问题");
        hfTest.addDependency("fix");
        WorkflowStep hfDeploy = new WorkflowStep("deploy", "紧急上线", "git-commit", "合并修复代码并紧急部署到生产环境");
        hfDeploy.addDependency("test");
        hfDeploy.setRequiresApproval(true);
        hotfix.addStep(hfAnalyze);
        hotfix.addStep(hfFix);
        hotfix.addStep(hfTest);
        hotfix.addStep(hfDeploy);

        // ===== 6. 功能分支流程 =====
        WorkflowTemplate feature = new WorkflowTemplate("feature-branch", "功能分支流程", "标准的功能开发分支流程");
        WorkflowStep ftDesign = new WorkflowStep("design", "功能设计", "system-planner", "设计功能方案、接口定义和数据模型");
        WorkflowStep ftImpl = new WorkflowStep("implement", "功能实现", "server-dev", "在功能分支上实现代码，编写单元测试");
        ftImpl.addDependency("design");
        WorkflowStep ftTest = new WorkflowStep("test", "测试验证", "tester", "执行功能测试和回归测试");
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
        WorkflowTemplate review = new WorkflowTemplate("code-review", "代码审查流程", "标准化的代码审查流程");
        WorkflowStep submitReview = new WorkflowStep("submit", "提交审查", "git-commit", "整理代码变更，准备审查材料");
        WorkflowStep doReview = new WorkflowStep("review", "执行审查", "server-dev", "审查代码质量、安全性、规范性");
        doReview.addDependency("submit");
        WorkflowStep mergeStep = new WorkflowStep("merge", "合并部署", "git-commit", "审查通过后合并代码并触发部署");
        mergeStep.addDependency("review");
        mergeStep.setRequiresApproval(true);
        review.addStep(submitReview);
        review.addStep(doReview);
        review.addStep(mergeStep);

        // ===== 8. 最小可用流程 =====
        WorkflowTemplate minimal = new WorkflowTemplate("minimal", "最小可用流程", "极简三步流程");
        WorkflowStep minDev = new WorkflowStep("dev", "开发", "server-dev", "完成功能开发或改动");
        WorkflowStep minTest = new WorkflowStep("test", "测试", "tester", "快速验证改动是否生效");
        minTest.addDependency("dev");
        WorkflowStep minDeploy = new WorkflowStep("deploy", "上线", "git-commit", "提交代码并部署");
        minDeploy.addDependency("test");
        minimal.addStep(minDev);
        minimal.addStep(minTest);
        minimal.addStep(minDeploy);

        // ===== 9. 策划案全流程 =====
        // 策划案从撰写到落实的完整流程，包含审核和打回机制
        WorkflowTemplate planningFull = new WorkflowTemplate("planning-full", "策划案全流程",
            "策划案从撰写、分析、审核到落实的完整流程，支持审核打回和修改迭代");
        // 步骤1：策划案撰写
        WorkflowStep planWrite = new WorkflowStep("plan-write", "策划案撰写", "system-planner",
            "根据项目需求撰写完整的游戏策划案，包括核心玩法、系统设计、数值框架、关卡设计等");
        // 步骤2：策划案分析
        WorkflowStep planAnalyze = new WorkflowStep("plan-analyze", "策划案分析", "numerical-planner",
            "深入分析策划案的可行性、完整性、平衡性和风险点，输出分析报告");
        planAnalyze.addDependency("plan-write");
        // 步骤3：策划案审核（需要审批）
        WorkflowStep planReview = new WorkflowStep("plan-review", "策划案审核", "system-planner",
            "对策划案进行专业审核，给出审核意见和是否通过的建议。审核不通过则打回修改");
        planReview.addDependency("plan-analyze");
        planReview.setRequiresApproval(true);
        planReview.setImportance("HIGH");  // 策划案审核是高重要程度
        // 步骤4：策划案修改（如果被打回）
        WorkflowStep planRevise = new WorkflowStep("plan-revise", "策划案修改", "system-planner",
            "根据审核意见修改策划案，解决审核中发现的问题");
        planRevise.addDependency("plan-review");
        // 步骤5：策划案落实
        WorkflowStep planImplement = new WorkflowStep("plan-implement", "策划案落实", "system-planner",
            "将审核通过的策划案转化为可执行的开发任务和技术方案，制定开发计划");
        planImplement.addDependency("plan-revise");
        planningFull.addStep(planWrite);
        planningFull.addStep(planAnalyze);
        planningFull.addStep(planReview);
        planningFull.addStep(planRevise);
        planningFull.addStep(planImplement);

        // ===== 10. 代码开发全流程 =====
        // 代码从编写到部署的完整流程，包含测试和审查
        WorkflowTemplate codeDevFull = new WorkflowTemplate("code-dev-full", "代码开发全流程",
            "代码从编写、测试、审查到部署的完整流程，确保代码质量");
        // 步骤1：需求分析
        WorkflowStep codeAnalyze = new WorkflowStep("analyze", "需求分析", "system-planner",
            "分析开发需求，确定技术方案和实现路径");
        // 步骤2：代码编写
        WorkflowStep codeWrite = new WorkflowStep("code-write", "代码编写", "server-dev",
            "根据需求和设计文档编写高质量、可维护的代码，遵循编码规范");
        codeWrite.addDependency("analyze");
        // 步骤3：单元测试
        WorkflowStep codeUnitTest = new WorkflowStep("unit-test", "单元测试", "tester",
            "编写和执行单元测试，确保代码逻辑正确，测试覆盖率达标");
        codeUnitTest.addDependency("code-write");
        // 步骤4：集成测试
        WorkflowStep codeIntegrationTest = new WorkflowStep("integration-test", "集成测试", "tester",
            "执行集成测试，验证模块间交互和系统整体功能");
        codeIntegrationTest.addDependency("unit-test");
        // 步骤5：代码审查
        WorkflowStep codeReviewStep = new WorkflowStep("code-review", "代码审查", "server-dev",
            "对代码进行专业审查，发现潜在问题，提供改进建议。审查不通过则返回修改");
        codeReviewStep.addDependency("integration-test");
        codeReviewStep.setRequiresApproval(true);
        codeReviewStep.setImportance("HIGH");  // 代码审查是高重要程度
        // 步骤6：代码修改（如果审查不通过）
        WorkflowStep codeRevise = new WorkflowStep("code-revise", "代码修改", "server-dev",
            "根据审查意见修改代码，解决审查中发现的问题");
        codeRevise.addDependency("code-review");
        // 步骤7：部署上线
        WorkflowStep codeDeploy = new WorkflowStep("deploy", "部署上线", "git-commit",
            "合并代码并部署到生产环境，进行线上验证");
        codeDeploy.addDependency("code-revise");
        codeDeploy.setRequiresApproval(true);
        codeDeploy.setImportance("CRITICAL");  // 部署上线是最高重要程度
        codeDevFull.addStep(codeAnalyze);
        codeDevFull.addStep(codeWrite);
        codeDevFull.addStep(codeUnitTest);
        codeDevFull.addStep(codeIntegrationTest);
        codeDevFull.addStep(codeReviewStep);
        codeDevFull.addStep(codeRevise);
        codeDevFull.addStep(codeDeploy);

        // ===== 11. 策划案快速评审 =====
        // 策划案快速评审流程，适用于小型或紧急策划
        WorkflowTemplate planningQuick = new WorkflowTemplate("planning-quick", "策划案快速评审",
            "策划案快速评审流程，精简环节快速完成审核，适用于小型或紧急策划");
        WorkflowStep pqWrite = new WorkflowStep("write", "策划撰写", "system-planner",
            "快速撰写策划案核心内容，聚焦关键设计点");
        WorkflowStep pqReview = new WorkflowStep("review", "快速审核", "system-planner",
            "快速审核策划案，给出通过或打回意见");
        pqReview.addDependency("write");
        pqReview.setRequiresApproval(true);
        pqReview.setImportance("LOW");  // 快速审核是低重要程度
        WorkflowStep pqImplement = new WorkflowStep("implement", "快速落实", "system-planner",
            "将策划案快速转化为开发任务");
        pqImplement.addDependency("review");
        planningQuick.addStep(pqWrite);
        planningQuick.addStep(pqReview);
        planningQuick.addStep(pqImplement);

        // ===== 12. 代码快速修复 =====
        // 代码快速修复流程，适用于小Bug修复
        WorkflowTemplate codeQuickFix = new WorkflowTemplate("code-quick-fix", "代码快速修复",
            "代码快速修复流程，适用于小Bug修复和紧急改动");
        WorkflowStep qfFix = new WorkflowStep("fix", "问题修复", "server-dev",
            "快速定位并修复代码问题，做最小化改动");
        WorkflowStep qfTest = new WorkflowStep("test", "快速测试", "tester",
            "快速验证修复是否生效，确认无回归问题");
        qfTest.addDependency("fix");
        WorkflowStep qfDeploy = new WorkflowStep("deploy", "快速上线", "git-commit",
            "提交修复代码并快速部署");
        qfDeploy.addDependency("test");
        codeQuickFix.addStep(qfFix);
        codeQuickFix.addStep(qfTest);
        codeQuickFix.addStep(qfDeploy);

        // 注册所有模板
        registerTemplate(standard);
        registerTemplate(serverOnly);
        registerTemplate(clientOnly);
        registerTemplate(rapid);
        registerTemplate(hotfix);
        registerTemplate(feature);
        registerTemplate(review);
        registerTemplate(minimal);
        registerTemplate(planningFull);
        registerTemplate(codeDevFull);
        registerTemplate(planningQuick);
        registerTemplate(codeQuickFix);

        // 持久化内置模板到数据库（如果不存在）
        persistBuiltinTemplates();

        // 从数据库加载用户自定义模板
        loadCustomTemplatesFromDatabase();

        // 从数据库恢复未完成的实例
        recoverUnfinishedInstances();

        log.info("工作流引擎初始化完成，已注册 {} 个模板", templates.size());
    }

    // ===== 模板管理 =====

    /** 注册工作流模板 */
    public void registerTemplate(WorkflowTemplate template) {
        templates.put(template.getId(), template);
    }

    /** 获取所有工作流模板 */
    public List<WorkflowTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /** 获取指定模板 */
    public WorkflowTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /** 创建自定义工作流模板 */
    public WorkflowTemplate createTemplate(String id, String name, String description, List<WorkflowStep> steps) {
        WorkflowTemplate template = new WorkflowTemplate(id, name, description);
        if (steps != null) {
            for (WorkflowStep step : steps) {
                template.addStep(step);
            }
        }
        templates.put(id, template);

        // 持久化到数据库
        try {
            WorkflowTemplateEntity entity = new WorkflowTemplateEntity();
            entity.setId(id);
            entity.setName(name);
            entity.setDescription(description);
            entity.setBuiltin(false);
            entity.setStepsJson(objectMapper.writeValueAsString(steps != null ? steps : new ArrayList<>()));
            workflowTemplateRepository.save(entity);
            log.info("自定义工作流模板创建并持久化: {}", id);
        } catch (Exception e) {
            log.error("持久化工作流模板失败: {}", id, e);
        }

        return template;
    }

    /** 删除工作流模板 */
    public boolean deleteTemplate(String templateId) {
        WorkflowTemplate removed = templates.remove(templateId);
        if (removed != null) {
            try {
                workflowTemplateRepository.deleteById(templateId);
            } catch (Exception e) {
                log.debug("模板在数据库中不存在或删除失败: {}", templateId);
            }
            return true;
        }
        return false;
    }

    // ===== 实例管理 =====

    /**
     * 启动工作流实例
     * 创建持久化记录并异步执行
     */
    public WorkflowInstance startWorkflow(String templateId, String projectId, Map<String, String> parameters) {
        if (templateId == null || templateId.isEmpty()) {
            throw new RuntimeException("工作流模板ID不能为空");
        }
        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("项目ID不能为空");
        }

        WorkflowTemplate template = templates.get(templateId);
        if (template == null) {
            throw new RuntimeException("工作流模板不存在: " + templateId);
        }

        String instanceId = UUID.randomUUID().toString();

        // 创建内存实例
        WorkflowInstance instance = new WorkflowInstance(instanceId, templateId, projectId);
        if (parameters != null) {
            instance.getParameters().putAll(parameters);
        }
        instance.setStatus(WorkflowStatus.RUNNING);

        // 初始化所有步骤的执行状态
        for (WorkflowStep step : template.getSteps()) {
            StepExecution execution = new StepExecution(step.getId());
            if (!step.getDependencies().isEmpty()) {
                execution.setStatus(StepStatus.WAITING_DEPENDENCIES);
            }
            instance.getStepExecutions().put(step.getId(), execution);
        }

        // 持久化到数据库
        persistInstance(instance, template);
        runningInstanceIds.add(instanceId);

        // 保存到内存缓存
        instanceCache.put(instanceId, instance);

        // 记录审计日志
        auditService.logSystem(instanceId, null, WorkflowAuditService.ACTION_INSTANCE_CREATED, null);
        auditService.logSystem(instanceId, null, WorkflowAuditService.ACTION_INSTANCE_STARTED, null);

        // 异步执行工作流
        executorService.submit(() -> executeWorkflow(instance));

        log.info("工作流启动: instanceId={}, templateId={}", instanceId, templateId);
        return instance;
    }

    /**
     * 持久化工作流实例到数据库
     */
    private void persistInstance(WorkflowInstance instance, WorkflowTemplate template) {
        try {
            // 保存实例
            WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
            entity.setId(instance.getId());
            entity.setTemplateId(instance.getTemplateId());
            entity.setProjectId(instance.getProjectId());
            entity.setStatus(instance.getStatus().name());
            entity.setStartedAt(LocalDateTime.now());
            if (instance.getParameters() != null && !instance.getParameters().isEmpty()) {
                entity.setParametersJson(objectMapper.writeValueAsString(instance.getParameters()));
            }
            instanceRepository.save(entity);

            // 保存步骤执行记录
            for (WorkflowStep step : template.getSteps()) {
                WorkflowStepExecutionEntity stepEntity = new WorkflowStepExecutionEntity();
                stepEntity.setInstanceId(instance.getId());
                stepEntity.setStepId(step.getId());
                stepEntity.setAgentRole(step.getAgentRole());
                stepEntity.setMaxRetries(3);
                stepEntity.setTimeoutMinutes(step.getTimeoutMinutes());
                stepEntity.setStatus(instance.getStepExecutions().get(step.getId()).getStatus().name());
                stepExecutionRepository.save(stepEntity);
            }
        } catch (Exception e) {
            log.error("持久化工作流实例失败: {}", instance.getId(), e);
        }
    }

    /**
     * 更新实例状态到数据库
     */
    private void updateInstanceStatus(String instanceId, String status, String errorMessage) {
        try {
            instanceRepository.findById(instanceId).ifPresent(entity -> {
                entity.setStatus(status);
                entity.setErrorMessage(errorMessage);
                if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                    entity.setCompletedAt(LocalDateTime.now());
                }
                instanceRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("更新实例状态失败: {}", instanceId, e);
        }
    }

    /**
     * 更新步骤执行状态到数据库
     */
    private void updateStepExecution(String instanceId, String stepId, String status,
                                      String agentId, String outputData, String errorMessage,
                                      Integer retryCount) {
        try {
            stepExecutionRepository.findByInstanceIdAndStepId(instanceId, stepId).ifPresent(entity -> {
                entity.setStatus(status);
                if (agentId != null) entity.setAgentId(agentId);
                if (outputData != null) entity.setOutputDataJson(outputData);
                if (errorMessage != null) entity.setErrorMessage(errorMessage);
                if (retryCount != null) entity.setRetryCount(retryCount);
                if ("RUNNING".equals(status)) entity.setStartedAt(LocalDateTime.now());
                if ("COMPLETED".equals(status) || "FAILED".equals(status)) entity.setCompletedAt(LocalDateTime.now());
                stepExecutionRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("更新步骤执行状态失败: instance={}, step={}", instanceId, stepId, e);
        }
    }

    // ===== 工作流执行 =====

    /**
     * 执行工作流（事件驱动）
     * 通过监听步骤完成/失败事件来推进工作流
     */
    private void executeWorkflow(WorkflowInstance instance) {
        try {
            // 执行所有就绪的步骤
            executeReadySteps(instance);

            // 如果所有步骤都完成了，标记工作流完成
            if (isAllStepsCompleted(instance)) {
                completeWorkflow(instance);
            }
        } catch (Exception e) {
            log.error("工作流执行异常: {}", instance.getId(), e);
            failWorkflow(instance, e.getMessage());
        }
    }

    /**
     * 执行所有就绪的步骤
     * 就绪条件：依赖已完成 + 非等待审批 + 未在运行中
     */
    private void executeReadySteps(WorkflowInstance instance) {
        WorkflowTemplate template = templates.get(instance.getTemplateId());
        if (template == null) return;

        // 收集所有就绪的步骤
        List<WorkflowStep> readySteps = new ArrayList<>();
        for (WorkflowStep step : template.getSteps()) {
            StepExecution execution = instance.getStepExecutions().get(step.getId());
            if (execution == null) continue;

            // 跳过已完成/已跳过/运行中的步骤
            if (execution.getStatus() == StepStatus.COMPLETED ||
                execution.getStatus() == StepStatus.SKIPPED ||
                execution.getStatus() == StepStatus.RUNNING) {
                continue;
            }

            // 检查依赖是否完成
            if (!areDependenciesCompleted(instance, step)) {
                continue;
            }

            // 检查是否需要审批且未审批
            if (step.isRequiresApproval() && execution.getStatus() != StepStatus.READY) {
                // 如果依赖已满足，先将状态更新为 READY，再发起审批请求
                if (areDependenciesCompleted(instance, step)) {
                    execution.setStatus(StepStatus.READY);
                    updateStepExecution(instance.getId(), step.getId(), "READY", null, null, null, null);
                    requestApproval(instance, step, execution);
                }
                continue;
            }

            readySteps.add(step);
        }

        if (readySteps.isEmpty()) return;

        // 分离并行步骤和串行步骤
        List<WorkflowStep> parallelSteps = new ArrayList<>();
        List<WorkflowStep> serialSteps = new ArrayList<>();
        for (WorkflowStep step : readySteps) {
            if (step.isParallel()) {
                parallelSteps.add(step);
            } else {
                serialSteps.add(step);
            }
        }

        // 并行执行标记为parallel的步骤
        if (!parallelSteps.isEmpty()) {
            executeParallelSteps(instance, parallelSteps);
        }

        // 串行执行其他步骤
        for (WorkflowStep step : serialSteps) {
            executeSingleStep(instance, step);
        }
    }

    /**
     * 并行执行多个步骤
     * 使用CompletableFuture.allOf等待所有步骤完成
     */
    private void executeParallelSteps(WorkflowInstance instance, List<WorkflowStep> steps) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (WorkflowStep step : steps) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> executeSingleStep(instance, step), executorService
            );
            futures.add(future);
        }

        // 等待所有并行步骤完成（或失败）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("并行步骤执行中有异常: {}", e.getMessage());
        }

        // 并行步骤完成后，检查是否有新的就绪步骤
        if (!isAllStepsCompleted(instance) && instance.getStatus() == WorkflowStatus.RUNNING) {
            executeReadySteps(instance);
        }
    }

    /**
     * 执行单个步骤
     * 包含超时、重试、Agent匹配、数据传递逻辑
     */
    private void executeSingleStep(WorkflowInstance instance, WorkflowStep step) {
        StepExecution execution = instance.getStepExecutions().get(step.getId());
        if (execution == null) return;

        log.info("执行步骤: {} (实例: {})", step.getId(), instance.getId());

        // 更新状态为运行中
        execution.setStatus(StepStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        updateStepExecution(instance.getId(), step.getId(), "RUNNING", null, null, null, null);
        auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_STEP_STARTED, null);

        // 收集依赖步骤的输出数据作为输入
        String inputData = collectDependencyOutputs(instance, step);
        execution.setInputData(inputData);
        updateStepExecutionInputData(instance.getId(), step.getId(), inputData);

        // 选择Agent
        Agent agent = agentMatchStrategy.selectBestAgent(step.getAgentRole(), instance.getProjectId());
        if (agent == null) {
            handleStepFailure(instance, step, execution, "没有可用的Agent: " + step.getAgentRole());
            return;
        }
        execution.setAgentId(agent.getId());
        updateStepExecution(instance.getId(), step.getId(), "RUNNING", agent.getId(), null, null, null);

        // 执行步骤（带超时）
        // 查找项目的制作人Agent作为任务分配者，避免 workflow-engine 在 MessageBus 中不存在的警告
        String assignerId = findProducerId(instance.getProjectId());
        try {
            String result = executeStepWithTimeout(agent, step, inputData, step.getTimeoutMinutes(), assignerId);
            handleStepSuccess(instance, step, execution, result);
        } catch (TimeoutException e) {
            log.warn("步骤超时: {} (实例: {})", step.getId(), instance.getId());
            auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_STEP_TIMEOUT, null);
            handleStepFailure(instance, step, execution, "步骤执行超时（" + step.getTimeoutMinutes() + "分钟）");
        } catch (Exception e) {
            log.error("步骤执行异常: {} (实例: {})", step.getId(), instance.getId(), e);
            handleStepFailure(instance, step, execution, e.getMessage());
        }
    }

    /**
     * 查找项目中的制作人Agent ID
     * 用于工作流任务分配时设置 assignerId，确保 Agent 汇报能正确路由
     *
     * @param projectId 项目ID
     * @return 制作人Agent的ID，未找到时返回 "system"
     */
    private String findProducerId(String projectId) {
        if (projectId == null || agentManager == null) return "system";
        Agent producer = agentManager.getAgent(projectId + ":producer");
        return producer != null ? producer.getId() : "system";
    }

    /**
     * 带超时的步骤执行
     * 使用CompletableFuture.orTimeout实现
     *
     * @param agent 执行任务的Agent
     * @param step 工作流步骤
     * @param inputData 输入数据
     * @param timeoutMinutes 超时时间（分钟）
     * @param assignerId 任务分配者ID（通常是制作人Agent）
     */
    private String executeStepWithTimeout(Agent agent, WorkflowStep step, String inputData, int timeoutMinutes, String assignerId)
            throws TimeoutException, Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // 创建任务
            TaskAssignment task = new TaskAssignment();
            task.setId(UUID.randomUUID().toString());
            task.setTitle(step.getName());
            task.setDescription(buildTaskDescription(step, inputData));
            task.setAssignerId(assignerId);
            task.setStatus(TaskAssignment.TaskStatus.PENDING);

            // 分配任务给Agent
            agent.assignTask(task);

            // 等待任务完成
            try {
                waitForTaskCompletion(agent, task.getId(), timeoutMinutes);
                return task.getResult() != null ? task.getResult() : "步骤完成";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        try {
            return future.get(timeoutMinutes, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw new Exception(e.getCause());
        }
    }

    /**
     * 构建任务描述（包含输入数据）
     */
    private String buildTaskDescription(WorkflowStep step, String inputData) {
        StringBuilder desc = new StringBuilder();
        desc.append(step.getTaskDescription());
        if (inputData != null && !inputData.isEmpty()) {
            desc.append("\n\n## 上游步骤输出数据\n");
            desc.append(inputData);
        }
        return desc.toString();
    }

    /**
     * 收集依赖步骤的输出数据
     * 包含AI文本响应和实际产出的文件内容
     */
    private String collectDependencyOutputs(WorkflowInstance instance, WorkflowStep step) {
        if (step.getDependencies().isEmpty()) return null;

        StringBuilder inputData = new StringBuilder();
        for (String depId : step.getDependencies()) {
            StepExecution depExecution = instance.getStepExecutions().get(depId);
            if (depExecution != null && depExecution.getResult() != null) {
                inputData.append("### ").append(depId).append(" 的输出:\n");
                inputData.append(depExecution.getResult()).append("\n\n");
            }
        }

        // 新增：收集依赖步骤在项目目录中产出的文件内容
        String projectDir = getProjectWorkDir(instance);
        if (projectDir != null) {
            String fileOutputs = collectRecentFileOutputs(projectDir);
            if (!fileOutputs.isEmpty()) {
                inputData.append("### 项目中已有的文件产出:\n");
                inputData.append(fileOutputs).append("\n");
            }
        }

        return inputData.length() > 0 ? inputData.toString() : null;
    }

    /**
     * 获取项目工作目录
     */
    private String getProjectWorkDir(WorkflowInstance instance) {
        if (projectManager == null || instance.getProjectId() == null) return null;
        try {
            com.chengxun.gamemaker.model.GameProject project = projectManager.getProject(instance.getProjectId());
            return project != null ? project.getWorkDir() : null;
        } catch (Exception e) {
            log.debug("获取项目工作目录失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 收集项目目录中最近产出的文件内容
     * 扫描项目目录中的文档、配置和代码文件，供下游步骤参考
     */
    private String collectRecentFileOutputs(String projectDir) {
        java.io.File dir = new java.io.File(projectDir);
        if (!dir.exists() || !dir.isDirectory()) return "";

        StringBuilder outputs = new StringBuilder();
        // 扫描常见文件类型（通用，不限定具体技术栈）
        String[] extensions = {
            ".md", ".txt", ".json", ".yaml", ".yml", ".toml", ".ini", ".xml",
            ".js", ".ts", ".jsx", ".tsx", ".vue", ".svelte",
            ".py", ".java", ".cs", ".c", ".cpp", ".h", ".hpp",
            ".go", ".rs", ".rb", ".php", ".swift", ".kt", ".dart", ".lua",
            ".gd", ".gdscript", ".shader", ".hlsl", ".glsl",
            ".html", ".css", ".scss", ".less"
        };

        scanAndCollectFiles(dir, outputs, extensions, 0, 3); // 最多扫描3层目录，最多收集2000字符

        return outputs.length() > 2000 ? outputs.substring(0, 2000) + "\n...(文件产出截断)" : outputs.toString();
    }

    /**
     * 递归扫描并收集文件内容
     */
    private void scanAndCollectFiles(java.io.File dir, StringBuilder outputs, String[] extensions, int depth, int maxDepth) {
        if (depth > maxDepth || outputs.length() > 2000) return;

        java.io.File[] files = dir.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (outputs.length() > 2000) break;

            if (file.isDirectory()) {
                if (!file.getName().equals("node_modules") && !file.getName().equals(".git")
                    && !file.getName().equals(".claude") && !file.getName().equals("dist")
                    && !file.getName().equals("build")) {
                    scanAndCollectFiles(file, outputs, extensions, depth + 1, maxDepth);
                }
            } else {
                String name = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (name.endsWith(ext)) {
                        try {
                            // 只读取较小的文件
                            if (file.length() > 0 && file.length() < 10000) {
                                String content = java.nio.file.Files.readString(file.toPath());
                                if (content.length() > 500) {
                                    content = content.substring(0, 500) + "\n...(截断)";
                                }
                                outputs.append("**").append(file.getName()).append("**\n```\n");
                                outputs.append(content).append("\n```\n\n");
                            }
                        } catch (Exception e) {
                            // 忽略读取错误
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 更新步骤输入数据到数据库
     */
    private void updateStepExecutionInputData(String instanceId, String stepId, String inputData) {
        try {
            stepExecutionRepository.findByInstanceIdAndStepId(instanceId, stepId).ifPresent(entity -> {
                entity.setInputDataJson(inputData);
                stepExecutionRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("更新步骤输入数据失败: instance={}, step={}", instanceId, stepId, e);
        }
    }

    /**
     * 处理步骤成功
     */
    private void handleStepSuccess(WorkflowInstance instance, WorkflowStep step,
                                     StepExecution execution, String result) {
        execution.setStatus(StepStatus.COMPLETED);
        execution.setResult(result);
        execution.setCompletedAt(LocalDateTime.now());

        updateStepExecution(instance.getId(), step.getId(), "COMPLETED", null, result, null, null);

        Map<String, Object> detail = new HashMap<>();
        detail.put("result", result);
        auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_STEP_COMPLETED, detail);

        // 发布步骤完成事件
        eventPublisher.publishEvent(new StepCompletedEvent(instance.getId(), step.getId(), result, result));

        // 检查是否所有步骤完成
        if (isAllStepsCompleted(instance)) {
            completeWorkflow(instance);
        } else {
            // 推进到下一个就绪步骤
            executeReadySteps(instance);
        }
    }

    /**
     * 处理步骤失败（支持重试）
     */
    private void handleStepFailure(WorkflowInstance instance, WorkflowStep step,
                                     StepExecution execution, String error) {
        int currentRetry = execution.getRetryCount() != null ? execution.getRetryCount() : 0;
        int maxRetries = step.getMaxRetries() > 0 ? step.getMaxRetries() : 3;

        if (currentRetry < maxRetries) {
            // 重试
            currentRetry++;
            execution.setRetryCount(currentRetry);
            updateStepExecution(instance.getId(), step.getId(), "RUNNING", null, null, null, currentRetry);

            Map<String, Object> retryDetail = new HashMap<>();
            retryDetail.put("retryCount", currentRetry);
            retryDetail.put("maxRetries", maxRetries);
            retryDetail.put("error", error);
            auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_STEP_RETRIED, retryDetail);

            log.info("步骤重试: {} (实例: {}, 重试次数: {}/{})", step.getId(), instance.getId(), currentRetry, maxRetries);

            // 重新执行
            executeSingleStep(instance, step);
        } else {
            // 重试次数用尽，标记失败
            execution.setStatus(StepStatus.FAILED);
            execution.setError(error);
            execution.setCompletedAt(LocalDateTime.now());

            updateStepExecution(instance.getId(), step.getId(), "FAILED", null, null, error, null);

            Map<String, Object> failDetail = new HashMap<>();
            failDetail.put("error", error);
            failDetail.put("retryCount", currentRetry);
            auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_STEP_FAILED, failDetail);

            // 发布步骤失败事件
            eventPublisher.publishEvent(new StepFailedEvent(instance.getId(), step.getId(), error));

            // 工作流失败
            failWorkflow(instance, "步骤 " + step.getName() + " 失败: " + error);
        }
    }

    // ===== 审批机制 =====

    /**
     * 发起审批请求
     * 根据审批级别决定审批方式：
     * - AUTO: 自动审批（低于阈值）
     * - PRODUCER: 制作人审批（日常事务）
     * - HUMAN: 人工审批（重大决策）
     */
    private void requestApproval(WorkflowInstance instance, WorkflowStep step, StepExecution execution) {
        String approvalLevel = step.getApprovalLevel();

        // 检查审批级别
        if ("AUTO".equals(approvalLevel)) {
            // 自动审批
            log.info("步骤审批级别为AUTO，自动审批: 步骤={}, 实例={}", step.getId(), instance.getId());
            autoApproveStep(instance, step, execution);
            return;
        }

        if ("PRODUCER".equals(approvalLevel)) {
            // 制作人审批 - 发送给制作人处理
            log.info("步骤审批级别为PRODUCER，发送给制作人审批: 步骤={}, 实例={}", step.getId(), instance.getId());
            requestProducerApproval(instance, step, execution);
            return;
        }

        // HUMAN级别 - 需要人工审批
        log.info("步骤审批级别为HUMAN，需要人工审批: 步骤={}, 实例={}", step.getId(), instance.getId());
        requestHumanApproval(instance, step, execution);
    }

    /**
     * 自动审批步骤
     */
    private void autoApproveStep(WorkflowInstance instance, WorkflowStep step, StepExecution execution) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("stepName", step.getName());
        detail.put("approvalLevel", "AUTO");
        detail.put("autoApproved", true);
        auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_APPROVAL_APPROVED, detail);

        // 更新步骤状态为READY并继续执行
        execution.setStatus(StepStatus.READY);
        executorService.submit(() -> executeWorkflow(instance));
    }

    /**
     * 请求制作人审批
     * 发送消息给制作人，由制作人自主决策
     */
    private void requestProducerApproval(WorkflowInstance instance, WorkflowStep step, StepExecution execution) {
        // 创建审批记录
        WorkflowApprovalEntity approval = new WorkflowApprovalEntity();
        approval.setInstanceId(instance.getId());
        approval.setStepId(step.getId());
        approval.setStatus("PENDING_PRODUCER");  // 等待制作人审批
        approvalRepository.save(approval);

        Map<String, Object> detail = new HashMap<>();
        detail.put("stepName", step.getName());
        detail.put("approvalLevel", "PRODUCER");
        auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_APPROVAL_REQUESTED, detail);

        // 发布审批请求事件（制作人会监听并处理）
        eventPublisher.publishEvent(new ApprovalRequiredEvent(instance.getId(), step.getId()));

        log.info("制作人审批请求已发起: 步骤={}, 实例={}", step.getId(), instance.getId());
    }

    /**
     * 请求人工审批
     * 需要系统管理员（真人）审批
     */
    private void requestHumanApproval(WorkflowInstance instance, WorkflowStep step, StepExecution execution) {
        // 创建审批记录
        WorkflowApprovalEntity approval = new WorkflowApprovalEntity();
        approval.setInstanceId(instance.getId());
        approval.setStepId(step.getId());
        approval.setStatus("PENDING");  // 等待人工审批
        approvalRepository.save(approval);

        Map<String, Object> detail = new HashMap<>();
        detail.put("stepName", step.getName());
        detail.put("approvalLevel", "HUMAN");
        auditService.logSystem(instance.getId(), step.getId(), WorkflowAuditService.ACTION_APPROVAL_REQUESTED, detail);

        // 发布审批请求事件（管理员会监听并处理）
        eventPublisher.publishEvent(new ApprovalRequiredEvent(instance.getId(), step.getId()));

        log.info("人工审批请求已发起: 步骤={}, 实例={}, 重要程度={}", step.getId(), instance.getId(), step.getImportance());
    }

    /**
     * 制作人审批通过
     * 用于PRODUCER级别的审批
     */
    public boolean producerApproveStep(String instanceId, String stepId, String producerId, String comment) {
        Optional<WorkflowApprovalEntity> approvalOpt = approvalRepository.findByInstanceIdAndStepId(instanceId, stepId);
        if (approvalOpt.isEmpty() || !"PENDING_PRODUCER".equals(approvalOpt.get().getStatus())) {
            return false;
        }

        WorkflowApprovalEntity approval = approvalOpt.get();
        approval.setApproverId(null);  // 制作人审批没有具体ID
        approval.setApproverName("Producer");
        approval.setStatus("APPROVED");
        approval.setComment(comment);
        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        // 更新步骤状态为READY
        updateStepExecution(instanceId, stepId, "READY", null, null, null, null);

        // 更新内存中的状态
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null) {
            StepExecution execution = instance.getStepExecutions().get(stepId);
            if (execution != null) {
                execution.setStatus(StepStatus.READY);
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("approverName", "Producer");
            detail.put("comment", comment);
            auditService.logSystem(instanceId, stepId, WorkflowAuditService.ACTION_APPROVAL_APPROVED, detail);

            // 继续执行
            executorService.submit(() -> executeWorkflow(instance));
        }

        return true;
    }

    /**
     * 获取重要程度等级数值，用于比较
     * LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
     */
    private int getImportanceLevel(String importance) {
        if (importance == null) return 2;  // 默认MEDIUM
        return switch (importance.toUpperCase()) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "CRITICAL" -> 4;
            default -> 2;
        };
    }

    /**
     * 审批通过
     */
    public boolean approveStep(String instanceId, String stepId, Long approverId, String approverName, String comment) {
        // 更新审批记录
        Optional<WorkflowApprovalEntity> approvalOpt = approvalRepository.findByInstanceIdAndStepId(instanceId, stepId);
        if (approvalOpt.isEmpty() || !"PENDING".equals(approvalOpt.get().getStatus())) {
            return false;
        }

        WorkflowApprovalEntity approval = approvalOpt.get();
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setStatus("APPROVED");
        approval.setComment(comment);
        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        // 更新步骤状态为READY
        updateStepExecution(instanceId, stepId, "READY", null, null, null, null);

        // 更新内存中的状态
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null) {
            StepExecution execution = instance.getStepExecutions().get(stepId);
            if (execution != null) {
                execution.setStatus(StepStatus.READY);
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("approverName", approverName);
            detail.put("comment", comment);
            auditService.logUser(instanceId, stepId, WorkflowAuditService.ACTION_APPROVAL_APPROVED,
                String.valueOf(approverId), approverName, detail);

            // 继续执行
            executorService.submit(() -> executeWorkflow(instance));
        }

        return true;
    }

    /**
     * 审批拒绝
     */
    public boolean rejectStep(String instanceId, String stepId, Long approverId, String approverName, String comment) {
        Optional<WorkflowApprovalEntity> approvalOpt = approvalRepository.findByInstanceIdAndStepId(instanceId, stepId);
        if (approvalOpt.isEmpty() || !"PENDING".equals(approvalOpt.get().getStatus())) {
            return false;
        }

        WorkflowApprovalEntity approval = approvalOpt.get();
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setStatus("REJECTED");
        approval.setComment(comment);
        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        Map<String, Object> detail = new HashMap<>();
        detail.put("approverName", approverName);
        detail.put("comment", comment);
        auditService.logUser(instanceId, stepId, WorkflowAuditService.ACTION_APPROVAL_REJECTED,
            String.valueOf(approverId), approverName, detail);

        // 拒审批导致工作流失败
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null) {
            failWorkflow(instance, "审批被拒绝: " + comment);
        }

        return true;
    }

    // ===== 事件监听 =====

    @EventListener
    @Async
    public void onStepCompleted(StepCompletedEvent event) {
        log.debug("收到步骤完成事件: instance={}, step={}", event.getInstanceId(), event.getStepId());
    }

    @EventListener
    @Async
    public void onStepFailed(StepFailedEvent event) {
        log.debug("收到步骤失败事件: instance={}, step={}, error={}", event.getInstanceId(), event.getStepId(), event.getError());
    }

    @EventListener
    @Async
    public void onApprovalRequired(ApprovalRequiredEvent event) {
        log.info("收到审批请求事件: instance={}, step={}", event.getInstanceId(), event.getStepId());
    }

    // ===== 工作流状态管理 =====

    /** 完成工作流 */
    private void completeWorkflow(WorkflowInstance instance) {
        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.setCompletedAt(LocalDateTime.now());
        runningInstanceIds.remove(instance.getId());

        updateInstanceStatus(instance.getId(), "COMPLETED", null);
        auditService.logSystem(instance.getId(), null, WorkflowAuditService.ACTION_INSTANCE_COMPLETED, null);

        log.info("工作流完成: {}", instance.getId());
    }

    /** 工作流失败 */
    private void failWorkflow(WorkflowInstance instance, String error) {
        instance.setStatus(WorkflowStatus.FAILED);
        instance.setCompletedAt(LocalDateTime.now());
        runningInstanceIds.remove(instance.getId());

        updateInstanceStatus(instance.getId(), "FAILED", error);
        auditService.logSystem(instance.getId(), null, WorkflowAuditService.ACTION_INSTANCE_FAILED, null);

        log.error("工作流失败: {} - {}", instance.getId(), error);
    }

    /** 取消工作流 */
    public void cancelWorkflow(String instanceId) {
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null) {
            instance.setStatus(WorkflowStatus.CANCELLED);
            runningInstanceIds.remove(instanceId);

            updateInstanceStatus(instanceId, "CANCELLED", null);
            auditService.logSystem(instanceId, null, WorkflowAuditService.ACTION_INSTANCE_CANCELLED, null);

            log.info("工作流取消: {}", instanceId);
        }
    }

    /** 暂停工作流 */
    public void pauseWorkflow(String instanceId) {
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null && instance.getStatus() == WorkflowStatus.RUNNING) {
            instance.setStatus(WorkflowStatus.PAUSED);
            updateInstanceStatus(instanceId, "PAUSED", null);
            auditService.logSystem(instanceId, null, WorkflowAuditService.ACTION_INSTANCE_PAUSED, null);
            log.info("工作流暂停: {}", instanceId);
        }
    }

    /** 恢复工作流 */
    public void resumeWorkflow(String instanceId) {
        WorkflowInstance instance = getInstance(instanceId);
        if (instance != null && instance.getStatus() == WorkflowStatus.PAUSED) {
            instance.setStatus(WorkflowStatus.RUNNING);
            updateInstanceStatus(instanceId, "RUNNING", null);
            auditService.logSystem(instanceId, null, WorkflowAuditService.ACTION_INSTANCE_RESUMED, null);

            executorService.submit(() -> executeWorkflow(instance));
            log.info("工作流恢复: {}", instanceId);
        }
    }

    // ===== 查询方法 =====

    /**
     * 获取工作流实例
     * 优先从内存缓存查询，如果没有则从数据库恢复
     *
     * @param instanceId 实例 ID
     * @return 工作流实例，不存在返回 null
     */
    public WorkflowInstance getInstance(String instanceId) {
        // 1. 先从内存缓存查询
        WorkflowInstance instance = instanceCache.get(instanceId);
        if (instance != null) {
            return instance;
        }

        // 2. 从数据库恢复
        instance = recoverInstanceFromDatabase(instanceId);
        if (instance != null) {
            // 保存到缓存
            instanceCache.put(instanceId, instance);
        }

        return instance;
    }

    /**
     * 获取指定项目的所有工作流实例
     *
     * @param projectId 项目 ID
     * @return 工作流实例列表
     */
    public List<WorkflowInstance> getInstancesByProject(String projectId) {
        List<WorkflowInstance> result = new ArrayList<>();

        // 从数据库查询
        List<WorkflowInstanceEntity> entities = instanceRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        for (WorkflowInstanceEntity entity : entities) {
            WorkflowInstance instance = getInstance(entity.getId());
            if (instance != null) {
                result.add(instance);
            }
        }

        return result;
    }

    /**
     * 从数据库恢复工作流实例
     *
     * @param instanceId 实例 ID
     * @return 恢复的工作流实例，不存在返回 null
     */
    private WorkflowInstance recoverInstanceFromDatabase(String instanceId) {
        try {
            Optional<WorkflowInstanceEntity> entityOpt = instanceRepository.findById(instanceId);
            if (entityOpt.isEmpty()) {
                return null;
            }

            WorkflowInstanceEntity entity = entityOpt.get();

            // 恢复实例
            WorkflowInstance instance = new WorkflowInstance(instanceId, entity.getTemplateId(), entity.getProjectId());
            instance.setStatus(WorkflowStatus.valueOf(entity.getStatus()));

            // 恢复参数
            if (entity.getParametersJson() != null && !entity.getParametersJson().isEmpty()) {
                Map<String, String> params = objectMapper.readValue(entity.getParametersJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                instance.getParameters().putAll(params);
            }

            // 恢复步骤执行状态
            List<WorkflowStepExecutionEntity> stepEntities = stepExecutionRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
            for (WorkflowStepExecutionEntity stepEntity : stepEntities) {
                StepExecution stepExecution = new StepExecution(stepEntity.getStepId());
                stepExecution.setStatus(StepStatus.valueOf(stepEntity.getStatus()));
                stepExecution.setAgentId(stepEntity.getAgentId());
                stepExecution.setResult(stepEntity.getOutputDataJson());
                stepExecution.setError(stepEntity.getErrorMessage());
                stepExecution.setRetryCount(stepEntity.getRetryCount());

                if (stepEntity.getStartedAt() != null) {
                    stepExecution.setStartedAt(stepEntity.getStartedAt());
                }
                if (stepEntity.getCompletedAt() != null) {
                    stepExecution.setCompletedAt(stepEntity.getCompletedAt());
                }

                instance.getStepExecutions().put(stepEntity.getStepId(), stepExecution);
            }

            log.info("从数据库恢复工作流实例: {}", instanceId);
            return instance;
        } catch (Exception e) {
            log.error("从数据库恢复工作流实例失败: {}", instanceId, e);
            return null;
        }
    }

    /**
     * 获取所有运行中的工作流实例
     *
     * @return 运行中的实例列表
     */
    public List<WorkflowInstance> getRunningInstances() {
        List<WorkflowInstance> running = new ArrayList<>();

        // 从内存缓存中筛选运行中的实例
        for (WorkflowInstance instance : instanceCache.values()) {
            if (instance.getStatus() == WorkflowStatus.RUNNING ||
                instance.getStatus() == WorkflowStatus.PAUSED) {
                running.add(instance);
            }
        }

        // 如果缓存为空，从数据库恢复
        if (running.isEmpty()) {
            try {
                List<WorkflowInstanceEntity> entities = instanceRepository.findByStatusIn(
                    Arrays.asList("RUNNING", "PAUSED"));
                for (WorkflowInstanceEntity entity : entities) {
                    WorkflowInstance instance = getInstance(entity.getId());
                    if (instance != null) {
                        running.add(instance);
                    }
                }
            } catch (Exception e) {
                log.warn("从数据库获取运行中实例失败: {}", e.getMessage());
            }
        }

        return running;
    }

    /**
     * 获取所有工作流实例（包括已完成的）
     *
     * @return 所有实例列表
     */
    public List<WorkflowInstance> getAllInstances() {
        List<WorkflowInstance> all = new ArrayList<>(instanceCache.values());

        // 补充数据库中有但缓存中没有的实例
        try {
            List<WorkflowInstanceEntity> entities = instanceRepository.findAll();
            for (WorkflowInstanceEntity entity : entities) {
                if (!instanceCache.containsKey(entity.getId())) {
                    WorkflowInstance instance = getInstance(entity.getId());
                    if (instance != null) {
                        all.add(instance);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从数据库获取所有实例失败: {}", e.getMessage());
        }

        return all;
    }

    /** 获取实例的审计日志 */
    public List<WorkflowAuditLogEntity> getAuditLogs(String instanceId) {
        return auditService.getInstanceLogs(instanceId);
    }

    /** 获取待审批列表 */
    public List<WorkflowApprovalEntity> getPendingApprovals() {
        return approvalRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    /** 获取指定用户的待审批列表 */
    public List<WorkflowApprovalEntity> getPendingApprovalsByUser(Long userId) {
        return approvalRepository.findByApproverIdAndStatusOrderByRequestedAtDesc(userId, "PENDING");
    }

    /** 获取实例的步骤执行详情 */
    public List<WorkflowStepExecutionEntity> getStepExecutions(String instanceId) {
        return stepExecutionRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
    }

    /** 获取实例的审批记录 */
    public List<WorkflowApprovalEntity> getApprovals(String instanceId) {
        return approvalRepository.findByInstanceIdOrderByRequestedAtDesc(instanceId);
    }

    // ===== 内部工具方法 =====

    /** 检查依赖是否完成 */
    private boolean areDependenciesCompleted(WorkflowInstance instance, WorkflowStep step) {
        for (String dependencyId : step.getDependencies()) {
            StepExecution dependencyExecution = instance.getStepExecutions().get(dependencyId);
            if (dependencyExecution == null || dependencyExecution.getStatus() != StepStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /** 检查是否所有步骤都完成 */
    private boolean isAllStepsCompleted(WorkflowInstance instance) {
        for (StepExecution execution : instance.getStepExecutions().values()) {
            if (execution.getStatus() != StepStatus.COMPLETED &&
                execution.getStatus() != StepStatus.SKIPPED) {
                return false;
            }
        }
        return true;
    }

    /** 等待任务完成 */
    private void waitForTaskCompletion(Agent agent, String taskId, int timeoutMinutes) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutMinutes * 60L * 1000L;
        long pollIntervalMs = 2000;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!agent.isAlive()) {
                throw new RuntimeException("Agent已停止: " + agent.getId());
            }

            TaskAssignment task = findTaskById(agent, taskId);
            if (task != null) {
                TaskAssignment.TaskStatus status = task.getStatus();
                if (status == TaskAssignment.TaskStatus.COMPLETED) {
                    return;
                } else if (status == TaskAssignment.TaskStatus.FAILED) {
                    throw new RuntimeException("任务执行失败: " + task.getError());
                } else if (status == TaskAssignment.TaskStatus.CANCELLED) {
                    throw new RuntimeException("任务已被取消");
                }
            }

            Thread.sleep(pollIntervalMs);
        }

        throw new TimeoutException("任务执行超时（超过" + timeoutMinutes + "分钟）");
    }

    /** 在Agent的任务列表中查找指定任务 */
    private TaskAssignment findTaskById(Agent agent, String taskId) {
        List<TaskAssignment> tasks = agent.getTasks();
        if (tasks == null) return null;
        for (TaskAssignment task : tasks) {
            if (taskId.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    /** 从数据库恢复未完成的实例 */
    private void recoverUnfinishedInstances() {
        try {
            List<WorkflowInstanceEntity> unfinished = instanceRepository.findByStatusIn(
                Arrays.asList("RUNNING", "PAUSED"));
            for (WorkflowInstanceEntity entity : unfinished) {
                log.info("恢复未完成的工作流实例: {}", entity.getId());
                // 标记为失败（简化的恢复策略，实际可以更精细）
                entity.setStatus("FAILED");
                entity.setErrorMessage("系统重启，自动标记为失败");
                entity.setCompletedAt(LocalDateTime.now());
                instanceRepository.save(entity);
            }
        } catch (Exception e) {
            log.warn("恢复未完成实例失败: {}", e.getMessage());
        }
    }

    /**
     * 从系统配置加载默认参数
     */
    private void loadSystemConfig() {
        try {
            // 这里可以从 SystemConfigService 或数据库加载配置
            // 暂时使用硬编码的默认值，后续可以通过配置覆盖
            defaultStepTimeoutMinutes = 60;  // 默认 60 分钟
            defaultMaxRetries = 3;           // 默认重试 3 次

            log.info("工作流系统配置已加载: timeout={}min, maxRetries={}",
                defaultStepTimeoutMinutes, defaultMaxRetries);
        } catch (Exception e) {
            log.warn("加载工作流系统配置失败，使用默认值: {}", e.getMessage());
        }
    }

    /**
     * 获取默认步骤超时时间
     */
    public int getDefaultStepTimeoutMinutes() {
        return defaultStepTimeoutMinutes;
    }

    /**
     * 设置默认步骤超时时间
     */
    public void setDefaultStepTimeoutMinutes(int minutes) {
        this.defaultStepTimeoutMinutes = minutes;
    }

    /**
     * 获取默认最大重试次数
     */
    public int getDefaultMaxRetries() {
        return defaultMaxRetries;
    }

    /**
     * 设置默认最大重试次数
     */
    public void setDefaultMaxRetries(int retries) {
        this.defaultMaxRetries = retries;
    }

    /**
     * 持久化内置模板到数据库
     * 确保重启后内置模板不丢失
     */
    private void persistBuiltinTemplates() {
        for (WorkflowTemplate template : templates.values()) {
            try {
                // 检查数据库中是否已存在
                if (workflowTemplateRepository.existsById(template.getId())) {
                    continue;
                }

                WorkflowTemplateEntity entity = new WorkflowTemplateEntity();
                entity.setId(template.getId());
                entity.setName(template.getName());
                entity.setDescription(template.getDescription());
                entity.setBuiltin(true);
                entity.setStepsJson(objectMapper.writeValueAsString(template.getSteps()));
                workflowTemplateRepository.save(entity);

                log.debug("持久化内置工作流模板: {}", template.getId());
            } catch (Exception e) {
                log.warn("持久化内置工作流模板失败: {} - {}", template.getId(), e.getMessage());
            }
        }
    }

    /** 从数据库加载用户自定义模板 */
    private void loadCustomTemplatesFromDatabase() {
        try {
            List<WorkflowTemplateEntity> customList = workflowTemplateRepository.findByBuiltinFalse();
            for (WorkflowTemplateEntity entity : customList) {
                if (templates.containsKey(entity.getId())) continue;
                WorkflowTemplate template = convertFromEntity(entity);
                if (template != null) {
                    templates.put(template.getId(), template);
                }
            }
            log.info("从数据库加载了 {} 个自定义工作流模板", customList.size());
        } catch (Exception e) {
            log.warn("从数据库加载自定义工作流模板失败: {}", e.getMessage());
        }
    }

    /** 将数据库实体转换为工作流模板对象 */
    private WorkflowTemplate convertFromEntity(WorkflowTemplateEntity entity) {
        try {
            WorkflowTemplate template = new WorkflowTemplate(entity.getId(), entity.getName(), entity.getDescription());
            if (entity.getStepsJson() != null && !entity.getStepsJson().isEmpty()) {
                List<Map<String, Object>> stepsData = objectMapper.readValue(
                    entity.getStepsJson(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> stepData : stepsData) {
                    String stepId = (String) stepData.get("id");
                    String stepName = (String) stepData.get("name");
                    String agentRole = (String) stepData.get("agentRole");
                    String taskDesc = (String) stepData.get("taskDescription");
                    WorkflowStep step = new WorkflowStep(stepId, stepName, agentRole, taskDesc);
                    List<String> deps = (List<String>) stepData.get("dependencies");
                    if (deps != null) {
                        for (String dep : deps) step.addDependency(dep);
                    }
                    if (stepData.containsKey("parallel") && stepData.get("parallel") != null) step.setParallel((Boolean) stepData.get("parallel"));
                    if (stepData.containsKey("requiresApproval") && stepData.get("requiresApproval") != null) step.setRequiresApproval((Boolean) stepData.get("requiresApproval"));
                    template.addStep(step);
                }
            }
            return template;
        } catch (Exception e) {
            log.error("转换工作流模板实体失败: {}", entity.getId(), e);
            return null;
        }
    }

    // ===== 内部类 =====

    /** 工作流模板 */
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

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<WorkflowStep> getSteps() { return steps; }
        public void addStep(WorkflowStep step) { steps.add(step); }
    }

    /** 工作流步骤 */
    public static class WorkflowStep {
        private String id;
        private String name;
        private String agentRole;
        private String taskDescription;
        private List<String> dependencies;
        private boolean parallel;
        private int timeoutMinutes;
        private boolean requiresApproval;
        private int maxRetries;
        /** 重要程度：LOW, MEDIUM, HIGH, CRITICAL */
        private String importance;
        /** 审批级别：AUTO(自动), PRODUCER(制作人审批), HUMAN(人工审批) */
        private String approvalLevel;

        public WorkflowStep(String id, String name, String agentRole, String taskDescription) {
            this.id = id;
            this.name = name;
            this.agentRole = agentRole;
            this.taskDescription = taskDescription;
            this.dependencies = new ArrayList<>();
            this.parallel = false;
            this.timeoutMinutes = 60;  // 默认 60 分钟
            this.requiresApproval = false;
            this.maxRetries = 3;
            this.importance = "MEDIUM";  // 默认中等重要程度
            this.approvalLevel = "PRODUCER";  // 默认制作人审批
        }

        /**
         * 构造函数（带超时和重试配置）
         */
        public WorkflowStep(String id, String name, String agentRole, String taskDescription,
                            int timeoutMinutes, int maxRetries) {
            this(id, name, agentRole, taskDescription);
            this.timeoutMinutes = timeoutMinutes;
            this.maxRetries = maxRetries;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getAgentRole() { return agentRole; }
        public String getTaskDescription() { return taskDescription; }
        public List<String> getDependencies() { return dependencies; }
        public boolean isParallel() { return parallel; }
        public int getTimeoutMinutes() { return timeoutMinutes; }
        public boolean isRequiresApproval() { return requiresApproval; }
        public int getMaxRetries() { return maxRetries; }
        public String getImportance() { return importance; }
        public String getApprovalLevel() { return approvalLevel; }

        public void addDependency(String stepId) { dependencies.add(stepId); }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public void setImportance(String importance) { this.importance = importance; }
        public void setApprovalLevel(String approvalLevel) { this.approvalLevel = approvalLevel; }
    }

    /** 工作流实例 */
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

    /** 步骤执行状态 */
    public static class StepExecution {
        private String stepId;
        private String agentId;
        private StepStatus status;
        private String result;
        private String inputData;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String error;
        private Integer retryCount;

        public StepExecution(String stepId) {
            this.stepId = stepId;
            this.status = StepStatus.PENDING;
            this.retryCount = 0;
        }

        public String getStepId() { return stepId; }
        public String getAgentId() { return agentId; }
        public StepStatus getStatus() { return status; }
        public String getResult() { return result; }
        public String getInputData() { return inputData; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public String getError() { return error; }
        public Integer getRetryCount() { return retryCount; }

        public void setAgentId(String agentId) { this.agentId = agentId; }
        public void setStatus(StepStatus status) { this.status = status; }
        public void setResult(String result) { this.result = result; }
        public void setInputData(String inputData) { this.inputData = inputData; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public void setError(String error) { this.error = error; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    }

    /** 工作流状态枚举 */
    public enum WorkflowStatus {
        CREATED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    /** 步骤状态枚举 */
    public enum StepStatus {
        PENDING, WAITING_DEPENDENCIES, READY, RUNNING, COMPLETED, FAILED, SKIPPED
    }
}
