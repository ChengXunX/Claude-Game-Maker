package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.feishu.FeishuAiBridgeService;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import com.chengxun.gamemaker.web.entity.Pipeline;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApprovalService;
import com.chengxun.gamemaker.web.service.PipelineService;
import com.chengxun.gamemaker.web.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
public class FeishuController {

    private static final Logger log = LoggerFactory.getLogger(FeishuController.class);

    private final FeishuBotService feishuService;
    private final FeishuAiBridgeService aiBridgeService;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final PipelineService pipelineService;
    private final ApprovalService approvalService;
    private final UserService userService;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 飞书卡片回调的管理员用户ID，用于以管理员身份执行审批 */
    private static final Long FEISHU_SYSTEM_ADMIN_ID = 1L;

    public FeishuController(FeishuBotService feishuService, FeishuAiBridgeService aiBridgeService,
                            AgentManager agentManager, ProjectManager projectManager,
                            PipelineService pipelineService, ApprovalService approvalService,
                            UserService userService, AppConfig appConfig) {
        this.feishuService = feishuService;
        this.aiBridgeService = aiBridgeService;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.pipelineService = pipelineService;
        this.approvalService = approvalService;
        this.userService = userService;
        this.appConfig = appConfig;
    }

    // ===== 飞书 AI 助手绑定 =====

    /**
     * 生成飞书绑定验证码
     * 用户在个人资料页面点击"生成验证码"后调用
     *
     * @param authentication 认证信息
     * @return 绑定验证码
     */
    @PostMapping("/bind-code")
    public Map<String, Object> generateBindCode(Authentication authentication) {
        if (authentication == null) {
            return Map.of("success", false, "message", "未登录");
        }
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return Map.of("success", false, "message", "用户不存在");
            }
            String code = aiBridgeService.generateBindCode(user.getId());
            return Map.of("success", true, "code", code, "message", "验证码已生成，有效期30分钟");
        } catch (Exception e) {
            log.error("Failed to generate bind code", e);
            return Map.of("success", false, "message", "生成验证码失败");
        }
    }

    /**
     * 查询飞书绑定状态
     *
     * @param authentication 认证信息
     * @return 绑定状态
     */
    @GetMapping("/bind-status")
    public Map<String, Object> getBindStatus(Authentication authentication) {
        if (authentication == null) {
            return Map.of("success", false, "message", "未登录");
        }
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return Map.of("success", false, "message", "用户不存在");
            }
            // 查询该用户是否有已绑定的飞书账号
            boolean bound = aiBridgeService.isUserBound(user.getId());
            if (bound) {
                String bindTime = aiBridgeService.getUserBindTime(user.getId());
                return Map.of("success", true, "bound", true, "bindTime", bindTime != null ? bindTime : "");
            }
            return Map.of("success", true, "bound", false);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询失败");
        }
    }

    /**
     * 解绑飞书账号
     *
     * @param authentication 认证信息
     * @return 解绑结果
     */
    @PostMapping("/unbind")
    public Map<String, Object> unbindFeishu(Authentication authentication) {
        if (authentication == null) {
            return Map.of("success", false, "message", "未登录");
        }
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return Map.of("success", false, "message", "用户不存在");
            }
            boolean result = aiBridgeService.unbindUserByUserId(user.getId());
            if (result) {
                return Map.of("success", true, "message", "已解绑");
            }
            return Map.of("success", false, "message", "未绑定或解绑失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "解绑失败");
        }
    }

    // ===== 飞书事件订阅回调 =====

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody String body) {
        try {
            JsonNode node = objectMapper.readTree(body);

            // 处理加密请求
            if (node.has("encrypt")) {
                String encryptKey = appConfig.getFeishu().getEncryptKey();
                if (encryptKey == null || encryptKey.isEmpty()) {
                    log.warn("Feishu encrypt key not configured, cannot decrypt event");
                    Map<String, Object> errResp = new HashMap<>();
                    errResp.put("code", -1);
                    errResp.put("msg", "encrypt key not configured");
                    return ResponseEntity.ok(errResp);
                }
                String decrypted = decryptFeishuEncrypt(node.get("encrypt").asText(), encryptKey);
                log.info("Feishu event decrypted: {}", decrypted);
                node = objectMapper.readTree(decrypted);
            }

            // 处理 URL 验证请求
            if (node.has("type") && "url_verification".equals(node.get("type").asText())) {
                Map<String, Object> response = new HashMap<>();
                response.put("challenge", node.get("challenge").asText());
                return ResponseEntity.ok(response);
            }

            // 处理事件回调
            if (node.has("header") && node.has("event")) {
                JsonNode header = node.get("header");
                JsonNode event = node.get("event");

                String eventType = header.get("event_type").asText();

                switch (eventType) {
                    case "im.message.receive_v1" -> handleMessageEvent(event);
                    case "im.chat.member.bot.added_v1" -> handleBotAddedEvent(event);
                    default -> log.info("Unhandled event type: {}", eventType);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle Feishu event", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("msg", "Internal error");
            return ResponseEntity.ok(response);
        }
    }

    // ===== 飞书卡片交互回调 =====

    /**
     * 处理飞书卡片按钮点击回调
     * 当用户点击审批卡片上的「同意」或「拒绝」按钮时，飞书会调用此接口
     *
     * 回调数据格式：
     * {
     *   "action": {
     *     "value": { "action": "approve", "requestId": 123 },
     *     "tag": "button"
     *   },
     *   "open_id": "ou_xxx",
     *   "tenant_key": "xxx"
     * }
     */
    @PostMapping("/card/callback")
    public ResponseEntity<Map<String, Object>> handleCardCallback(@RequestBody String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            log.info("Feishu card callback received: {}", body);

            // 处理加密请求（飞书开启数据加密后，所有回调都是加密的）
            if (node.has("encrypt")) {
                String encryptKey = appConfig.getFeishu().getEncryptKey();
                if (encryptKey == null || encryptKey.isEmpty()) {
                    log.warn("Feishu encrypt key not configured, cannot decrypt callback");
                    return buildCardCallbackResponse("❌ 系统未配置飞书加密密钥");
                }
                String decrypted = decryptFeishuEncrypt(node.get("encrypt").asText(), encryptKey);
                log.info("Feishu card callback decrypted: {}", decrypted);
                node = objectMapper.readTree(decrypted);
            }

            // 处理 URL 验证请求（飞书配置回调地址时的验证）
            if (node.has("type") && "url_verification".equals(node.get("type").asText())) {
                Map<String, Object> response = new HashMap<>();
                response.put("challenge", node.get("challenge").asText());
                log.info("Card callback URL verification challenge responded");
                return ResponseEntity.ok(response);
            }

            // 提取按钮的 value 信息
            // 【修复】加密回调结构为 event.action.value，非加密回调为 action.value
            JsonNode actionNode = node.path("action");
            JsonNode valueNode = actionNode.path("value");
            String openId = node.path("open_id").asText("");
            // 如果是加密回调（schema 2.0），从 event.action.value 获取
            if (valueNode.isMissingNode() && node.has("event")) {
                JsonNode eventNode = node.path("event");
                actionNode = eventNode.path("action");
                valueNode = actionNode.path("value");
                // 从 event.operator.open_id 获取用户ID
                JsonNode operatorNode = eventNode.path("operator");
                if (operatorNode.has("open_id")) {
                    openId = operatorNode.path("open_id").asText("");
                }
            }

            String action = valueNode.path("action").asText();
            // 【修复】requestId 可能是数字或字符串类型，需要兼容处理
            long requestId = parseLong(valueNode.path("requestId"));
            long expireTime = parseLong(valueNode.path("expire"));
            String sig = valueNode.path("sig").asText(null);
            String actionId = valueNode.path("actionId").asText(null);

            // 如果没有 requestId 但有 actionId，跳转到审批页面
            if (requestId == 0 && actionId != null && !actionId.isEmpty()) {
                boolean approved = "approve".equals(action);
                String resultMsg = approved
                    ? "✅ 已确认同意，请前往审批页面完成最终审批"
                    : "❌ 已确认拒绝，请前往审批页面完成最终审批";
                log.info("Feishu card action without requestId: actionId={}, action={}", actionId, action);
                return buildCardCallbackResponse(resultMsg);
            }

            if (requestId == 0) {
                log.warn("Card callback missing requestId");
                return buildCardCallbackResponse("❌ 无效的审批请求");
            }

            // 1. 检查过期时间
            if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
                log.warn("Approval card expired: requestId={}, expireTime={}, now={}", requestId, expireTime, System.currentTimeMillis());
                return buildCardCallbackResponse("⏰ 审批已过期\n\n该审批卡片已失效，请前往系统管理页面处理。");
            }

            // 2. 验证签名
            String callbackToken = appConfig.getFeishu().getCallbackToken();
            if (callbackToken != null && !callbackToken.isEmpty()) {
                if (sig == null || sig.isEmpty()) {
                    log.warn("Approval card missing signature: requestId={}", requestId);
                    return buildCardCallbackResponse("❌ 签名缺失，无法验证审批卡片合法性");
                }
                String expectedSig = signCallback(requestId, expireTime, callbackToken);
                if (!sig.equals(expectedSig)) {
                    log.warn("Approval card signature mismatch: requestId={}", requestId);
                    return buildCardCallbackResponse("❌ 签名验证失败，审批卡片可能被篡改");
                }
            }

            // 3. 获取飞书用户 open_id，记录到审批意见中
            // 【修复】避免从 Redis 缓存获取 User 对象导致懒加载异常
            String approverName = "飞书审批(" + openId + ")";

            // 使用系统管理员身份执行审批
            Long approverId = FEISHU_SYSTEM_ADMIN_ID;

            boolean approved = "approve".equals(action);

            try {
                ApprovalRequest approvalRequest = approvalService.approve(
                    requestId,
                    approverId.toString(),
                    approverName,
                    approved,
                    approved ? "飞书卡片快速审批通过" : "飞书卡片快速审批拒绝"
                );

                String resultMsg = approved
                    ? "✅ 审批已通过！请求ID: " + requestId
                    : "❌ 审批已拒绝！请求ID: " + requestId;

                log.info("Feishu card approval processed: requestId={}, action={}, approver={}", requestId, action, approverName);

                // 返回更新后的卡片内容
                return buildCardCallbackResponse(resultMsg);
            } catch (Exception e) {
                log.error("Failed to process approval from Feishu card", e);
                return buildCardCallbackResponse("❌ 审批处理失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to handle Feishu card callback", e);
            return buildCardCallbackResponse("❌ 系统错误");
        }
    }

    /**
     * 生成审批回调签名
     * 使用 HMAC-SHA256 对 requestId:expire 进行签名
     */
    private String signCallback(Long requestId, long expireTime, String callbackToken) {
        if (callbackToken == null || callbackToken.isEmpty()) {
            return null;
        }
        try {
            String data = requestId + ":" + expireTime;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            log.error("Failed to sign callback", e);
            return null;
        }
    }

    /**
     * 解密飞书加密回调数据
     * 飞书开启数据加密后，回调请求体中的 encrypt 字段是 Base64 编码的 AES-256-CBC 加密数据
     * 解密过程：Base64 解码 → 前 16 字节为 IV → 剩余为密文 → AES-256-CBC 解密
     *
     * @param encryptData Base64 编码的加密数据
     * @param encryptKey 飞书配置的加密密钥
     * @return 解密后的 JSON 字符串
     */
    private String decryptFeishuEncrypt(String encryptData, String encryptKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptData);
            // 前 16 字节是 IV
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[decoded.length - 16];
            System.arraycopy(decoded, 0, iv, 0, 16);
            System.arraycopy(decoded, 16, encrypted, 0, encrypted.length);

            // 密钥是 encryptKey 的 SHA-256 哈希
            java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(encryptKey.getBytes(StandardCharsets.UTF_8));

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(keyBytes, "AES"),
                new javax.crypto.spec.IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt Feishu encrypt data: {}", e.getMessage(), e);
            throw new RuntimeException("解密飞书回调数据失败: " + e.getMessage());
        }
    }

    /**
     * 构建卡片回调响应
     * 飞书卡片回调响应格式：
     * 1. {"toast": {"type": "success", "content": "提示内容"}} - 显示toast提示
     * 2. 返回新的卡片JSON - 更新卡片内容
     *
     * 【重要】必须返回toast格式，否则飞书会一直转圈等待响应
     */
    private ResponseEntity<Map<String, Object>> buildCardCallbackResponse(String message) {
        // 【修复】返回toast格式响应
        Map<String, Object> toast = new HashMap<>();
        toast.put("type", message.startsWith("✅") ? "success" : "info");
        toast.put("content", message);

        Map<String, Object> response = new HashMap<>();
        response.put("toast", toast);
        return ResponseEntity.ok(response);
    }

    /**
     * 安全解析 JsonNode 为 long
     * 兼容数字和字符串两种类型
     *
     * @param node JSON 节点
     * @return 解析后的 long 值，解析失败返回 0
     */
    private long parseLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isLong() || node.isInt()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return node.asLong(0);
    }

    private void handleMessageEvent(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String chatId = message.get("chat_id").asText();
            String messageId = message.get("message_id").asText();
            String msgType = message.get("message_type").asText();
            String chatType = message.has("chat_type") ? message.get("chat_type").asText() : "unknown";

            log.info("handleMessageEvent: chatId={}, msgType={}, chatType={}", chatId, msgType, chatType);

            if (!"text".equals(msgType)) {
                log.info("Ignoring non-text message: {}", msgType);
                return;
            }

            // 提取发送者 open_id
            String openId = null;
            if (event.has("sender")) {
                openId = event.get("sender").path("sender_id").path("open_id").asText(null);
            }

            JsonNode content = objectMapper.readTree(message.get("content").asText());
            String text = content.get("text").asText().trim();

            // 移除 @机器人 的 mention 标记
            text = text.replaceAll("@_user_\\d+", "").trim();

            log.info("Received message from Feishu chat {} (openId={}, chatType={}): {}", chatId, openId, chatType, text);

            // 处理绑定/解绑命令
            if (openId != null && text.startsWith("绑定")) {
                String code = text.substring(2).trim();
                log.info("Processing bind command: openId={}, code={}", openId, code);
                String result = aiBridgeService.bindUser(openId, code);
                feishuService.sendMessage(chatId, result);
                return;
            }
            if (openId != null && "解绑".equals(text)) {
                String result = aiBridgeService.unbindUser(openId);
                feishuService.sendMessage(chatId, result);
                return;
            }

            // 处理帮助命令
            if ("帮助".equals(text) || "help".equalsIgnoreCase(text)) {
                handleHelp(chatId);
                return;
            }

            // 检查是否已绑定（群聊和私聊都支持）
            if (openId != null && aiBridgeService.isBound(openId)) {
                log.info("User is bound, forwarding to AI assistant: openId={}, text={}", openId, text);
                // 异步处理 AI 请求，立即返回响应给飞书（避免飞书重试）
                String capturedChatId = chatId;
                String capturedOpenId = openId;
                String capturedText = text;
                String capturedMessageId = messageId;
                CompletableFuture.runAsync(() -> {
                    try {
                        String aiResponse = aiBridgeService.processMessage(capturedOpenId, capturedText, capturedMessageId);
                        log.info("AI response received, length={}", aiResponse != null ? aiResponse.length() : 0);
                        // 使用回复消息形式发送，@提问者
                        feishuService.sendReplyCardMessage(capturedMessageId, capturedOpenId, "🤖 AI 助手", "blue", aiResponse);
                    } catch (Exception e) {
                        log.error("Failed to process AI message: {}", e.getMessage(), e);
                        feishuService.sendCardMessageWithMention(capturedChatId, capturedOpenId, "🤖 AI 助手", "red", "处理消息时出错：" + e.getMessage());
                    }
                });
                return;
            }

            log.info("User not bound or openId null, processing as command: openId={}, text={}", openId, text);
            // 处理原有命令
            processCommand(chatId, text);
        } catch (Exception e) {
            log.error("Failed to handle message event", e);
        }
    }

    private void handleBotAddedEvent(JsonNode event) {
        try {
            String chatId = event.get("chat_id").asText();
            log.info("Bot added to chat: {}", chatId);

            // 自动设置为默认群
            feishuService.setDefaultChatId(chatId);

            feishuService.sendMessage(chatId, "👋 你好！我是 ChengXun Game Maker 的智能助手。\n\n" +
                "**🤖 AI 助手**\n" +
                "- 绑定账号后可直接向我提问\n" +
                "- 发送「绑定 <验证码>」绑定账号\n" +
                "- 发送「帮助」查看所有功能\n\n" +
                "**项目管理**\n" +
                "- `创建项目 <名称> <目录>` - 创建新游戏项目\n" +
                "- `项目列表` - 查看所有项目\n\n" +
                "**Agent 管理**\n" +
                "- `状态` - 查看所有 Agent 状态\n" +
                "- `分配 <Agent> <目录>` - 为 Agent 分配工作目录\n\n" +
                "**任务管理**\n" +
                "- `任务 <Agent> <内容>` - 向 Agent 发送任务\n" +
                "- `查询 <Agent> <问题>` - 向 Agent 提问\n\n" +
                "**其他**\n" +
                "- `帮助` - 显示帮助信息");
        } catch (Exception e) {
            log.error("Failed to handle bot added event", e);
        }
    }

    // ===== 命令处理 =====

    private void processCommand(String chatId, String text) {
        // 确保 chatId 已设置
        if (feishuService.getDefaultChatId() == null) {
            feishuService.setDefaultChatId(chatId);
        }

        String[] parts = text.split("\\s+", 3);
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        switch (command) {
            case "帮助", "help" -> handleHelp(chatId);
            case "状态", "status" -> handleStatus(chatId);
            case "项目列表", "projects" -> handleProjectList(chatId);
            case "创建项目", "create" -> handleCreateProject(chatId, parts);
            case "分配", "assign" -> handleAssignWorkDir(chatId, parts);
            case "任务", "task" -> handleTask(chatId, parts);
            case "查询", "query" -> handleQuery(chatId, parts);
            case "approve" -> handleApproval(chatId, true);
            case "reject" -> handleApproval(chatId, false);
            // 流水线相关命令
            case "流水线", "pipeline" -> handlePipelineCommand(chatId, parts);
            case "构建", "build" -> handleBuild(chatId, parts);
            case "部署", "deploy" -> handleDeploy(chatId, parts);
            case "测试", "test" -> handleTest(chatId, parts);
            default -> feishuService.sendMessage(chatId, "❓ 未知命令: " + command + "\n\n输入 **帮助** 查看可用命令");
        }
    }

    private void handleHelp(String chatId) {
        feishuService.sendMessage(chatId, "📖 **可用命令**\n\n" +
            "**🤖 AI 助手**\n" +
            "- `绑定 <验证码>` - 绑定系统账号\n" +
            "- `解绑` - 解除绑定\n" +
            "- 绑定后直接发消息即可使用 AI 助手\n\n" +
            "**项目管理**\n" +
            "- `创建项目 <名称> <目录>` - 创建新游戏项目\n" +
            "- `项目列表` - 查看所有项目\n\n" +
            "**Agent 管理**\n" +
            "- `状态` - 查看所有 Agent 状态\n" +
            "- `分配 <Agent名称> <目录>` - 为 Agent 分配工作目录\n\n" +
            "**任务管理**\n" +
            "- `任务 <Agent名称> <内容>` - 向 Agent 发送任务\n" +
            "- `查询 <Agent名称> <问题>` - 向 Agent 提问\n\n" +
            "**审批**\n" +
            "- `approve` - 批准审批请求\n" +
            "- `reject` - 拒绝审批请求");
    }

    private void handleStatus(String chatId) {
        StringBuilder sb = new StringBuilder("**📊 系统状态**\n\n");

        sb.append("**Agent 状态：**\n");
        for (Agent agent : agentManager.getAllAgents()) {
            sb.append(String.format("- %s (%s): %s %s\n",
                agent.getName(),
                agent.getRole(),
                agent.isAlive() ? "🟢" : "🔴",
                agent.isBusy() ? "忙碌" : "空闲"));
        }

        sb.append("\n**项目列表：**\n");
        for (GameProject project : projectManager.getAllProjects()) {
            sb.append(String.format("- %s (%s)\n", project.getName(), project.getWorkDir()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    private void handleProjectList(String chatId) {
        StringBuilder sb = new StringBuilder("**📁 项目列表**\n\n");

        if (projectManager.getAllProjects().isEmpty()) {
            sb.append("暂无项目\n\n使用 `创建项目 <名称> <目录>` 创建新项目");
        } else {
            for (GameProject project : projectManager.getAllProjects()) {
                sb.append(String.format("- **%s**\n  目录: %s\n  状态: %s\n  Agent数: %d\n\n",
                    project.getName(),
                    project.getWorkDir(),
                    project.getStatus(),
                    project.getAgentIds().size()));
            }
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    private void handleCreateProject(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `创建项目 <名称> <目录>`");
            return;
        }

        String name = parts[1];
        String workDir = parts[2];

        try {
            GameProject project = projectManager.createProject(name, "由飞书创建的游戏项目", workDir);
            feishuService.sendMessage(chatId, String.format("✅ 项目创建成功\n\n- 名称: %s\n- 目录: %s\n- ID: %s",
                project.getName(), project.getWorkDir(), project.getId()));
        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 项目创建失败: " + e.getMessage());
        }
    }

    private void handleAssignWorkDir(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `分配 <Agent名称> <目录>`");
            return;
        }

        String agentName = parts[1];
        String workDir = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        if (targetAgent instanceof ProducerAgent producer) {
            producer.assignWorkDir(targetAgent.getId(), workDir);
            feishuService.sendMessage(chatId, String.format("✅ 已为 %s 分配工作目录: %s", targetAgent.getName(), workDir));
        } else {
            targetAgent.getDefinition().setWorkDir(workDir);
            targetAgent.saveContext();
            feishuService.sendMessage(chatId, String.format("✅ 已为 %s 分配工作目录: %s", targetAgent.getName(), workDir));
        }
    }

    private void handleTask(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `任务 <Agent名称> <内容>`");
            return;
        }

        String agentName = parts[1];
        String taskContent = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        // 发送任务给 Agent
        com.chengxun.gamemaker.model.AgentMessage taskMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
            "feishu-user", targetAgent.getId(), taskContent);
        targetAgent.receiveMessage(taskMsg);

        feishuService.sendMessage(chatId, String.format("✅ 任务已发送给 %s\n\n任务内容: %s",
            targetAgent.getName(), taskContent));
    }

    private void handleQuery(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `查询 <Agent名称> <问题>`");
            return;
        }

        String agentName = parts[1];
        String queryContent = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        // 发送查询给 Agent
        com.chengxun.gamemaker.model.AgentMessage queryMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
            .fromAgentId("feishu-user")
            .toAgentId(targetAgent.getId())
            .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.QUERY)
            .content(queryContent)
            .build();
        targetAgent.receiveMessage(queryMsg);

        feishuService.sendMessage(chatId, String.format("✅ 查询已发送给 %s\n\n问题: %s\n\n请等待回复...",
            targetAgent.getName(), queryContent));
    }

    /**
     * 处理审批请求
     * 记录审批结果并通知相关Agent
     *
     * @param chatId 飞书聊天ID
     * @param approved 是否批准
     */
    private void handleApproval(String chatId, boolean approved) {
        log.info("Approval request processed: chatId={}, approved={}", chatId, approved);

        // 通知所有Producer Agent审批结果
        agentManager.getAllAgents().stream()
            .filter(a -> "producer".equals(a.getRole()))
            .forEach(agent -> {
                com.chengxun.gamemaker.model.AgentMessage approvalMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
                    .fromAgentId("feishu-user")
                    .toAgentId(agent.getId())
                    .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.SYSTEM)
                    .content(approved ? "审批已通过" : "审批已被拒绝")
                    .build();
                agent.receiveMessage(approvalMsg);
            });

        feishuService.sendMessage(chatId, approved ? "✅ 已批准，已通知制作人" : "❌ 已拒绝，已通知制作人");
    }

    // ===== 流水线相关命令 =====

    /**
     * 处理流水线命令
     */
    private void handlePipelineCommand(String chatId, String[] parts) {
        if (parts.length < 2) {
            showPipelineHelp(chatId);
            return;
        }

        String subCommand = parts[1].toLowerCase();

        switch (subCommand) {
            case "列表", "list" -> handlePipelineList(chatId);
            case "触发", "trigger" -> handlePipelineTrigger(chatId, parts);
            case "状态", "status" -> handlePipelineStatus(chatId, parts);
            case "创建", "create" -> handlePipelineCreate(chatId, parts);
            default -> showPipelineHelp(chatId);
        }
    }

    /**
     * 显示流水线帮助信息
     */
    private void showPipelineHelp(String chatId) {
        feishuService.sendMessage(chatId, "🔧 **流水线命令帮助**\n\n" +
            "**查看流水线**\n" +
            "- `流水线 列表` - 查看所有流水线\n" +
            "- `流水线 状态 <编号>` - 查看流水线状态\n\n" +
            "**触发执行**\n" +
            "- `流水线 触发 <编号>` - 触发指定流水线\n" +
            "- `构建 <项目名>` - 快速触发构建\n" +
            "- `测试 <项目名>` - 快速触发测试\n" +
            "- `部署 <项目名>` - 快速触发部署\n\n" +
            "**创建流水线**\n" +
            "- `流水线 创建 <项目名> <类型>` - 创建新流水线\n" +
            "  类型: build/test/deploy/full");
    }

    /**
     * 查看流水线列表
     */
    private void handlePipelineList(String chatId) {
        List<Pipeline> pipelines = pipelineService.getAllPipelines();

        if (pipelines.isEmpty()) {
            feishuService.sendMessage(chatId, "📋 **流水线列表**\n\n暂无流水线\n\n使用 `流水线 创建 <项目名> <类型>` 创建新流水线");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 **流水线列表**\n\n");
        for (Pipeline pipeline : pipelines) {
            sb.append(String.format("- **%s** (%s)\n  项目: %s\n  状态: %s\n  类型: %s\n\n",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getStatusDescription(),
                pipeline.getPipelineTypeDescription()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    /**
     * 触发流水线执行
     */
    private void handlePipelineTrigger(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 触发 <编号>`\n\n例如: `流水线 触发 PL-20260529-ABC123`");
            return;
        }

        String pipelineNo = parts[2];

        // 查找流水线
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        Pipeline targetPipeline = pipelines.stream()
            .filter(p -> p.getPipelineNo().equals(pipelineNo))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            feishuService.sendMessage(chatId, "❌ 未找到流水线: " + pipelineNo);
            return;
        }

        if (targetPipeline.isRunning()) {
            feishuService.sendMessage(chatId, "⚠️ 流水线正在执行中，无法重复触发");
            return;
        }

        try {
            // 触发执行
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "飞书用户", "FEISHU");

            feishuService.sendMessage(chatId, String.format("✅ 流水线已触发执行\n\n" +
                "- 名称: %s\n" +
                "- 编号: %s\n" +
                "- 项目: %s\n" +
                "- 类型: %s\n\n" +
                "请等待执行完成，可使用 `流水线 状态 %s` 查看进度",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getPipelineTypeDescription(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 触发失败: " + e.getMessage());
        }
    }

    /**
     * 查看流水线状态
     */
    private void handlePipelineStatus(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 状态 <编号>`");
            return;
        }

        String pipelineNo = parts[2];

        // 查找流水线
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        Pipeline targetPipeline = pipelines.stream()
            .filter(p -> p.getPipelineNo().equals(pipelineNo))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            feishuService.sendMessage(chatId, "❌ 未找到流水线: " + pipelineNo);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **流水线状态**\n\n");
        sb.append(String.format("- 名称: %s\n", targetPipeline.getName()));
        sb.append(String.format("- 编号: %s\n", targetPipeline.getPipelineNo()));
        sb.append(String.format("- 项目: %s\n", targetPipeline.getProjectName()));
        sb.append(String.format("- 类型: %s\n", targetPipeline.getPipelineTypeDescription()));
        sb.append(String.format("- 状态: %s\n", targetPipeline.getStatusDescription()));
        sb.append(String.format("- 进度: %d%%\n", targetPipeline.getProgress()));

        if (targetPipeline.getCurrentStage() != null) {
            sb.append(String.format("- 当前阶段: %s\n", targetPipeline.getCurrentStage()));
        }

        if (targetPipeline.getTriggeredByName() != null) {
            sb.append(String.format("- 触发人: %s\n", targetPipeline.getTriggeredByName()));
        }

        if (targetPipeline.getDurationSeconds() != null) {
            sb.append(String.format("- 耗时: %d秒\n", targetPipeline.getDurationSeconds()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    /**
     * 创建流水线
     */
    private void handlePipelineCreate(String chatId, String[] parts) {
        if (parts.length < 4) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 创建 <项目名> <类型>`\n\n" +
                "类型: build/test/deploy/full\n\n" +
                "例如: `流水线 创建 my-game full`");
            return;
        }

        String projectName = parts[2];
        String pipelineType = parts[3].toUpperCase();

        // 查找项目
        GameProject targetProject = projectManager.getAllProjects().stream()
            .filter(p -> p.getName().contains(projectName) || p.getId().contains(projectName))
            .findFirst()
            .orElse(null);

        if (targetProject == null) {
            feishuService.sendMessage(chatId, "❌ 未找到项目: " + projectName);
            return;
        }

        // 验证流水线类型
        if (!List.of("BUILD", "TEST", "DEPLOY", "FULL").contains(pipelineType)) {
            feishuService.sendMessage(chatId, "❌ 无效的流水线类型: " + pipelineType + "\n\n有效类型: build/test/deploy/full");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + getPipelineTypeName(pipelineType),
                "由飞书创建的流水线",
                targetProject.getId(),
                pipelineType,
                null
            );

            feishuService.sendMessage(chatId, String.format("✅ 流水线创建成功\n\n" +
                "- 名称: %s\n" +
                "- 编号: %s\n" +
                "- 项目: %s\n" +
                "- 类型: %s\n\n" +
                "使用 `流水线 触发 %s` 开始执行",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getPipelineTypeDescription(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 创建失败: " + e.getMessage());
        }
    }

    /**
     * 快速构建
     */
    private void handleBuild(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `构建 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "BUILD", "构建");
    }

    /**
     * 快速测试
     */
    private void handleTest(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `测试 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "TEST", "测试");
    }

    /**
     * 快速部署
     */
    private void handleDeploy(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `部署 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "DEPLOY", "部署");
    }

    /**
     * 快速触发流水线
     */
    private void quickTriggerPipeline(String chatId, String projectName, String pipelineType, String actionName) {
        // 查找项目
        GameProject targetProject = projectManager.getAllProjects().stream()
            .filter(p -> p.getName().contains(projectName) || p.getId().contains(projectName))
            .findFirst()
            .orElse(null);

        if (targetProject == null) {
            feishuService.sendMessage(chatId, "❌ 未找到项目: " + projectName);
            return;
        }

        // 查找或创建流水线
        List<Pipeline> projectPipelines = pipelineService.getProjectPipelines(targetProject.getId());
        Pipeline targetPipeline = projectPipelines.stream()
            .filter(p -> p.getPipelineType().equals(pipelineType))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            // 创建新流水线
            targetPipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + actionName,
                "快速" + actionName + "流水线",
                targetProject.getId(),
                pipelineType,
                null
            );
        }

        if (targetPipeline.isRunning()) {
            feishuService.sendMessage(chatId, "⚠️ 流水线正在执行中，请等待完成");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "飞书用户", "FEISHU");

            feishuService.sendMessage(chatId, String.format("✅ %s流水线已触发\n\n" +
                "- 项目: %s\n" +
                "- 编号: %s\n\n" +
                "请等待执行完成",
                actionName,
                targetProject.getName(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 触发失败: " + e.getMessage());
        }
    }

    /**
     * 获取流水线类型名称
     */
    private String getPipelineTypeName(String type) {
        return switch (type) {
            case "BUILD" -> "构建";
            case "TEST" -> "测试";
            case "DEPLOY" -> "部署";
            case "FULL" -> "完整流水线";
            default -> type;
        };
    }
}
