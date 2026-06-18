package com.chengxun.gamemaker.feishu;

import com.chengxun.gamemaker.service.AiAssistantService;
import com.chengxun.gamemaker.web.entity.ChatMessage;
import com.chengxun.gamemaker.web.entity.ChatSession;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.ChatMessageRepository;
import com.chengxun.gamemaker.web.repository.ChatSessionRepository;
import com.chengxun.gamemaker.web.repository.FeishuUserBindingRepository;
import com.chengxun.gamemaker.web.repository.UserRepository;
import com.chengxun.gamemaker.web.entity.FeishuUserBinding;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 飞书-AI 助手桥接服务
 * 负责飞书用户绑定和消息转发给 AI 助手
 *
 * 主要功能：
 * - 生成绑定验证码
 * - 绑定/解绑飞书用户
 * - 转发飞书消息给 AI 助手
 * - 权限校验
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class FeishuAiBridgeService {

    private static final Logger log = LoggerFactory.getLogger(FeishuAiBridgeService.class);

    private final FeishuUserBindingRepository bindingRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AiAssistantService aiAssistantService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 30;

    public FeishuAiBridgeService(FeishuUserBindingRepository bindingRepository,
                                  UserRepository userRepository,
                                  UserService userService,
                                  AiAssistantService aiAssistantService,
                                  ChatSessionRepository chatSessionRepository,
                                  ChatMessageRepository chatMessageRepository) {
        this.bindingRepository = bindingRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.aiAssistantService = aiAssistantService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * 为指定用户生成绑定验证码
     *
     * @param userId 系统用户 ID
     * @return 生成的验证码
     */
    @org.springframework.transaction.annotation.Transactional
    public String generateBindCode(Long userId) {
        // 生成 6 位数字验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        // 清理该用户之前未使用的验证码
        bindingRepository.deleteByUserIdAndStatus(userId, FeishuUserBinding.BindingStatus.PENDING);

        // 保存新验证码
        FeishuUserBinding binding = new FeishuUserBinding();
        binding.setUserId(userId);
        binding.setBindingCode(code);
        binding.setStatus(FeishuUserBinding.BindingStatus.PENDING);
        binding.setCreatedAt(LocalDateTime.now());
        bindingRepository.save(binding);

        log.info("Generated bind code for user {}: {}", userId, code);
        return code;
    }

    /**
     * 绑定飞书用户
     *
     * @param openId 飞书用户 open_id
     * @param code 绑定验证码
     * @return 绑定结果消息
     */
    @org.springframework.transaction.annotation.Transactional
    public String bindUser(String openId, String code) {
        // 检查是否已绑定
        Optional<FeishuUserBinding> existing = bindingRepository.findByOpenIdAndStatus(
            openId, FeishuUserBinding.BindingStatus.BOUND);
        if (existing.isPresent()) {
            return "您已经绑定过了，无需重复绑定。如需解绑请发送「解绑」";
        }

        // 查找验证码对应的绑定记录
        Optional<FeishuUserBinding> pending = bindingRepository.findByBindingCodeAndStatus(
            code, FeishuUserBinding.BindingStatus.PENDING);
        if (pending.isEmpty()) {
            return "验证码无效或已过期，请重新获取验证码";
        }

        FeishuUserBinding binding = pending.get();

        // 检查验证码是否过期（30 分钟）
        if (binding.getCreatedAt().plusMinutes(CODE_EXPIRE_MINUTES).isBefore(LocalDateTime.now())) {
            bindingRepository.delete(binding);
            return "验证码已过期，请重新获取验证码";
        }

        // 检查该 open_id 是否已绑定其他用户
        bindingRepository.findByOpenId(openId).ifPresent(old -> bindingRepository.delete(old));

        // 完成绑定
        binding.setOpenId(openId);
        binding.setStatus(FeishuUserBinding.BindingStatus.BOUND);
        binding.setUpdatedAt(LocalDateTime.now());
        bindingRepository.save(binding);

        // 获取用户信息
        User user = userService.getUserById(binding.getUserId());
        String userName = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "用户" + binding.getUserId();

        log.info("Feishu user {} bound to system user {} ({})", openId, binding.getUserId(), userName);
        return String.format("绑定成功！欢迎 %s，现在可以直接向我提问了。\n\n发送「帮助」查看可用功能", userName);
    }

    /**
     * 解绑飞书用户
     *
     * @param openId 飞书用户 open_id
     * @return 解绑结果消息
     */
    public String unbindUser(String openId) {
        Optional<FeishuUserBinding> binding = bindingRepository.findByOpenIdAndStatus(
            openId, FeishuUserBinding.BindingStatus.BOUND);
        if (binding.isEmpty()) {
            return "您还没有绑定账号，无需解绑";
        }

        bindingRepository.delete(binding.get());
        log.info("Feishu user {} unbound", openId);
        return "解绑成功！如需重新使用 AI 助手，请重新绑定账号";
    }

    /**
     * 处理飞书消息并调用 AI 助手
     * 消息会保存到数据库，与网页端共享上下文
     *
     * @param openId 飞书用户 open_id
     * @param message 用户消息
     * @param feishuMessageId 飞书消息ID（用于回复）
     * @return AI 助手响应
     */
    @org.springframework.transaction.annotation.Transactional
    public String processMessage(String openId, String message, String feishuMessageId) {
        // 查询绑定状态
        Optional<FeishuUserBinding> binding = bindingRepository.findByOpenIdAndStatus(
            openId, FeishuUserBinding.BindingStatus.BOUND);

        if (binding.isEmpty()) {
            return "您还没有绑定账号，请先绑定：\n\n" +
                   "1. 在系统后台「个人资料」页面获取绑定验证码\n" +
                   "2. 发送「绑定 <验证码」完成绑定\n\n" +
                   "例如：绑定 123456";
        }

        Long userId = binding.get().getUserId();

        // 检查用户是否有 AI 助手使用权限（直接查数据库，绕过 Redis 缓存）
        Optional<User> userOpt = userRepository.findByIdWithRole(userId);
        if (userOpt.isEmpty()) {
            return "绑定的用户账号不存在，请重新绑定";
        }
        User user = userOpt.get();

        // 检查是否有 ai:use 权限
        if (!user.hasPermission("ai:use")) {
            return "您的账号没有 AI 助手使用权限，请联系管理员开通";
        }

        // 获取或创建飞书会话（与网页端共享上下文）
        ChatSession session = getOrCreateFeishuSession(userId);

        // 保存用户消息到数据库
        saveChatMessage(session.getId(), "user", message, feishuMessageId, openId);

        // 调用 AI 助手
        try {
            log.info("Feishu AI request from user {} (openId={}): {}", userId, openId,
                message.length() > 50 ? message.substring(0, 50) + "..." : message);

            AiAssistantService.AiResponse response = aiAssistantService.askQuestion(
                String.valueOf(userId), message);

            if (response == null || response.getAnswer() == null || response.getAnswer().isEmpty()) {
                return "AI 助手暂时无法响应，请稍后再试";
            }

            // 保存 AI 响应到数据库
            saveChatMessage(session.getId(), "assistant", response.getAnswer(), null, null);

            return response.getAnswer();
        } catch (Exception e) {
            log.error("Failed to process Feishu AI request: {}", e.getMessage(), e);
            return "处理请求时出错：" + e.getMessage();
        }
    }

    /**
     * 获取或创建用户的飞书会话
     * 如果已有飞书会话则复用，否则创建新会话
     *
     * @param userId 用户ID
     * @return 会话对象
     */
    private ChatSession getOrCreateFeishuSession(Long userId) {
        // 查找已有的飞书会话
        ChatSession session = chatSessionRepository.findFirstByUserIdAndSourceOrderByUpdatedAtDesc(userId, "feishu");
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            return chatSessionRepository.save(session);
        }

        // 创建新的飞书会话
        session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("飞书对话");
        session.setSource("feishu");
        return chatSessionRepository.save(session);
    }

    /**
     * 保存聊天消息到数据库
     *
     * @param sessionId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @param feishuMessageId 飞书消息ID
     * @param feishuOpenId 飞书用户open_id
     */
    private void saveChatMessage(Long sessionId, String role, String content,
                                  String feishuMessageId, String feishuOpenId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSessionId(sessionId);
            message.setRole(role);
            message.setContent(content);
            message.setFeishuMessageId(feishuMessageId);
            message.setFeishuOpenId(feishuOpenId);
            chatMessageRepository.save(message);
        } catch (Exception e) {
            log.warn("Failed to save chat message: {}", e.getMessage());
        }
    }

    /**
     * 获取用户的绑定信息
     *
     * @param openId 飞书用户 open_id
     * @return 绑定信息，未绑定返回 null
     */
    public FeishuUserBinding getBinding(String openId) {
        return bindingRepository.findByOpenIdAndStatus(openId, FeishuUserBinding.BindingStatus.BOUND)
            .orElse(null);
    }

    /**
     * 检查用户是否已绑定
     *
     * @param openId 飞书用户 open_id
     * @return 是否已绑定
     */
    public boolean isBound(String openId) {
        return bindingRepository.findByOpenIdAndStatus(openId, FeishuUserBinding.BindingStatus.BOUND)
            .isPresent();
    }

    /**
     * 检查系统用户是否已绑定飞书
     *
     * @param userId 系统用户 ID
     * @return 是否已绑定
     */
    public boolean isUserBound(Long userId) {
        return bindingRepository.findByUserIdAndStatus(userId, FeishuUserBinding.BindingStatus.BOUND)
            .isPresent();
    }

    /**
     * 获取用户绑定时间
     *
     * @param userId 系统用户 ID
     * @return 绑定时间字符串，未绑定返回 null
     */
    public String getUserBindTime(Long userId) {
        return bindingRepository.findByUserIdAndStatus(userId, FeishuUserBinding.BindingStatus.BOUND)
            .map(b -> b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : null)
            .orElse(null);
    }

    /**
     * 根据系统用户 ID 解绑
     *
     * @param userId 系统用户 ID
     * @return 是否解绑成功
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean unbindUserByUserId(Long userId) {
        Optional<FeishuUserBinding> binding = bindingRepository.findByUserIdAndStatus(
            userId, FeishuUserBinding.BindingStatus.BOUND);
        if (binding.isEmpty()) {
            return false;
        }
        bindingRepository.delete(binding.get());
        log.info("User {} unbound from Feishu", userId);
        return true;
    }
}
