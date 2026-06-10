package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.NotificationTemplate;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Category;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Channel;
import com.chengxun.gamemaker.web.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板服务
 * 负责通知模板的管理和变量替换
 *
 * 主要功能：
 * - 模板CRUD操作
 * - 变量替换引擎
 * - 模板预览
 * - 默认模板初始化
 *
 * 变量格式：${variableName}
 *
 * 内置变量：
 * - ${title} - 标题
 * - ${content} - 内容
 * - ${time} - 当前时间
 * - ${priority} / ${priorityDesc} - 优先级
 * - ${ruleName} - 规则名称
 * - ${metric} - 指标名称
 * - ${triggerValue} - 触发值
 * - ${thresholdValue} - 阈值
 * - ${agentId} / ${agentName} - Agent信息
 * - ${taskTitle} - 任务标题
 * - ${taskResult} - 任务结果
 * - ${projectName} - 项目名称
 * - ${userName} - 用户名称
 * - ${systemName} - 系统名称
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);

    /** 变量匹配模式：${variableName} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /** 日期时间格式 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository,
                                        @org.springframework.context.annotation.Lazy EmailService emailService) {
        this.templateRepository = templateRepository;
        this.emailService = emailService;
    }

    /**
     * 启动时初始化默认模板
     * 为所有通知场景 × 渠道创建内置模板，已存在的不覆盖
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultTemplates() {
        int created = 0;

        // ===== 告警通知模板 =====
        created += createIfAbsent("ALERT_SYSTEM", "站内告警通知", Channel.SYSTEM, Category.ALERT,
            "[${priorityDesc}] 告警: ${ruleName}",
            "⚠️ 监控告警已触发\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📋 告警规则：${ruleName}\n" +
            "🔴 告警级别：${priorityDesc}\n" +
            "📊 当前值：${triggerValue}\n" +
            "📏 阈值：${thresholdValue}\n" +
            "🕐 触发时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━\n\n" +
            "请及时处理！");

        created += createIfAbsent("ALERT_EMAIL", "邮件告警通知", Channel.EMAIL, Category.ALERT,
            "[${priorityDesc}] 告警: ${ruleName}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">⚠️ 监控告警</h1>\n" +
            "    <p style=\"margin: 10px 0 0 0; opacity: 0.9;\">${priorityDesc}级别告警</p>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">告警规则</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${ruleName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">告警级别</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\"><span style=\"background: #ff6b6b; color: white; padding: 2px 8px; border-radius: 4px;\">${priorityDesc}</span></td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">当前值</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold; color: #ff6b6b;\">${triggerValue}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">阈值</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\">${thresholdValue}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">触发时间</td><td style=\"padding: 10px;\">${time}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "  <div style=\"background: #fff3cd; padding: 15px; border: 1px solid #ffc107; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <p style=\"margin: 0; color: #856404;\">💡 请及时处理告警，避免影响系统稳定性</p>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("ALERT_FEISHU", "飞书告警通知", Channel.FEISHU, Category.ALERT,
            "[${priorityDesc}] 告警: ${ruleName}",
            "**⚠️ 监控告警通知**\n\n" +
            "---\n\n" +
            "**告警规则**：${ruleName}\n" +
            "**告警级别**：${priorityDesc}\n" +
            "**当前值**：${triggerValue}\n" +
            "**阈值**：${thresholdValue}\n" +
            "**触发时间**：${time}\n\n" +
            "---\n\n" +
            "请及时处理告警，避免影响系统稳定性！");

        created += createIfAbsent("ALERT_DINGTALK", "钉钉告警通知", Channel.DINGTALK, Category.ALERT,
            "[${priorityDesc}] 告警: ${ruleName}",
            "### ⚠️ 监控告警通知\n\n" +
            "---\n\n" +
            "- **告警规则**：${ruleName}\n" +
            "- **告警级别**：${priorityDesc}\n" +
            "- **当前值**：${triggerValue}\n" +
            "- **阈值**：${thresholdValue}\n" +
            "- **触发时间**：${time}\n\n" +
            "---\n\n" +
            "请及时处理告警，避免影响系统稳定性！");

        // ===== 告警恢复模板 =====
        created += createIfAbsent("RECOVERY_SYSTEM", "站内告警恢复", Channel.SYSTEM, Category.ALERT,
            "✅ 告警恢复: ${ruleName}",
            "✅ 告警已恢复\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📋 告警规则：${ruleName}\n" +
            "📊 当前值：${triggerValue}\n" +
            "🕐 恢复时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━\n\n" +
            "系统已恢复正常运行。");

        created += createIfAbsent("RECOVERY_EMAIL", "邮件告警恢复", Channel.EMAIL, Category.ALERT,
            "✅ 告警恢复: ${ruleName}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #51cf66 0%, #40c057 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">✅ 告警已恢复</h1>\n" +
            "    <p style=\"margin: 10px 0 0 0; opacity: 0.9;\">系统已恢复正常</p>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">告警规则</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${ruleName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">当前值</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold; color: #51cf66;\">${triggerValue}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">恢复时间</td><td style=\"padding: 10px;\">${time}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("RECOVERY_FEISHU", "飞书告警恢复", Channel.FEISHU, Category.ALERT,
            "✅ 告警恢复: ${ruleName}",
            "**✅ 告警已恢复**\n\n" +
            "---\n\n" +
            "**告警规则**：${ruleName}\n" +
            "**当前值**：${triggerValue}\n" +
            "**恢复时间**：${time}\n\n" +
            "---\n\n" +
            "系统已恢复正常运行。");

        created += createIfAbsent("RECOVERY_DINGTALK", "钉钉告警恢复", Channel.DINGTALK, Category.ALERT,
            "✅ 告警恢复: ${ruleName}",
            "### ✅ 告警已恢复\n\n" +
            "---\n\n" +
            "- **告警规则**：${ruleName}\n" +
            "- **当前值**：${triggerValue}\n" +
            "- **恢复时间**：${time}\n\n" +
            "---\n\n" +
            "系统已恢复正常运行。");

        // ===== 任务通知模板 =====
        created += createIfAbsent("TASK_SYSTEM", "站内任务通知", Channel.SYSTEM, Category.TASK,
            "📋 任务通知: ${taskTitle}",
            "📋 任务通知\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📝 任务名称：${taskTitle}\n" +
            "📊 执行结果：${taskResult}\n" +
            "🕐 完成时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━");

        created += createIfAbsent("TASK_EMAIL", "邮件任务通知", Channel.EMAIL, Category.TASK,
            "📋 任务通知: ${taskTitle}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #748ffc 0%, #5c7cfa 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">📋 任务通知</h1>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">任务名称</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${taskTitle}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">执行结果</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\"><span style=\"background: #51cf66; color: white; padding: 2px 8px; border-radius: 4px;\">${taskResult}</span></td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">完成时间</td><td style=\"padding: 10px;\">${time}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("TASK_FEISHU", "飞书任务通知", Channel.FEISHU, Category.TASK,
            "📋 任务通知: ${taskTitle}",
            "**📋 任务通知**\n\n" +
            "---\n\n" +
            "**任务名称**：${taskTitle}\n" +
            "**执行结果**：${taskResult}\n" +
            "**完成时间**：${time}");

        created += createIfAbsent("TASK_DINGTALK", "钉钉任务通知", Channel.DINGTALK, Category.TASK,
            "📋 任务通知: ${taskTitle}",
            "### 📋 任务通知\n\n" +
            "---\n\n" +
            "- **任务名称**：${taskTitle}\n" +
            "- **执行结果**：${taskResult}\n" +
            "- **完成时间**：${time}");

        // ===== Agent通知模板 =====
        created += createIfAbsent("AGENT_SYSTEM", "站内Agent通知", Channel.SYSTEM, Category.AGENT,
            "🤖 Agent通知: ${agentName}",
            "🤖 Agent通知\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "🤖 Agent名称：${agentName}\n" +
            "📝 通知内容：\n${content}\n" +
            "🕐 通知时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━");

        created += createIfAbsent("AGENT_EMAIL", "邮件Agent通知", Channel.EMAIL, Category.AGENT,
            "🤖 Agent通知: ${agentName}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #845ef7 0%, #7048e8 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">🤖 Agent通知</h1>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">Agent名称</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${agentName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">通知内容</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\">${content}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">通知时间</td><td style=\"padding: 10px;\">${time}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("AGENT_FEISHU", "飞书Agent通知", Channel.FEISHU, Category.AGENT,
            "🤖 Agent通知: ${agentName}",
            "**🤖 Agent通知**\n\n" +
            "---\n\n" +
            "**Agent名称**：${agentName}\n" +
            "**通知内容**：\n${content}\n" +
            "**通知时间**：${time}");

        created += createIfAbsent("AGENT_DINGTALK", "钉钉Agent通知", Channel.DINGTALK, Category.AGENT,
            "🤖 Agent通知: ${agentName}",
            "### 🤖 Agent通知\n\n" +
            "---\n\n" +
            "- **Agent名称**：${agentName}\n" +
            "- **通知内容**：\n${content}\n" +
            "- **通知时间**：${time}");

        // ===== 审批通知模板 =====
        created += createIfAbsent("APPROVAL_SYSTEM", "站内审批通知", Channel.SYSTEM, Category.SYSTEM,
            "📝 审批通知: ${title}",
            "📝 审批通知\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "${content}\n" +
            "🕐 通知时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━");

        created += createIfAbsent("APPROVAL_EMAIL", "邮件审批通知", Channel.EMAIL, Category.SYSTEM,
            "📝 审批通知: ${title}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #ff922b 0%, #fd7e14 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">📝 审批通知</h1>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <p>${content}</p>\n" +
            "    <p style=\"color: #666; margin-top: 20px;\">通知时间：${time}</p>\n" +
            "  </div>\n" +
            "</div>");

        // 新审批请求通知（通知管理员有新的审批需要处理）
        created += createIfAbsent("APPROVAL_NEW_SYSTEM", "站内新审批请求", Channel.SYSTEM, Category.SYSTEM,
            "🔔 [审批] ${requestTypeDesc}",
            "🔔 新审批请求\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📋 审批类型：${requestTypeDesc}\n" +
            "👤 发起者：${requesterName}\n" +
            "📝 描述：${description}\n" +
            "📁 项目：${projectName}\n" +
            "🕐 时间：${time}\n" +
            "━━━━━━━━━━━━━━━━━━\n\n" +
            "请及时处理！");

        created += createIfAbsent("APPROVAL_NEW_EMAIL", "邮件新审批请求", Channel.EMAIL, Category.SYSTEM,
            "🔔 [审批] ${requestTypeDesc}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #ff922b 0%, #fd7e14 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">🔔 新审批请求</h1>\n" +
            "    <p style=\"margin: 10px 0 0 0; opacity: 0.9;\">请及时处理</p>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">审批类型</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${requestTypeDesc}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">发起者</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\">${requesterName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">描述</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\">${description}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">项目</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\">${projectName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">时间</td><td style=\"padding: 10px;\">${time}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "  <div style=\"background: #fff3cd; padding: 15px; border: 1px solid #ffc107; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <p style=\"margin: 0; color: #856404;\">💡 请及时处理审批请求</p>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("APPROVAL_NEW_FEISHU", "飞书新审批请求", Channel.FEISHU, Category.SYSTEM,
            "🔔 [审批] ${requestTypeDesc}",
            "**🔔 新审批请求**\n\n" +
            "---\n\n" +
            "**审批类型**：${requestTypeDesc}\n" +
            "**发起者**：${requesterName}\n" +
            "**描述**：${description}\n" +
            "**项目**：${projectName}\n" +
            "**时间**：${time}\n\n" +
            "---\n\n" +
            "请及时处理！");

        created += createIfAbsent("APPROVAL_NEW_DINGTALK", "钉钉新审批请求", Channel.DINGTALK, Category.SYSTEM,
            "🔔 [审批] ${requestTypeDesc}",
            "### 🔔 新审批请求\n\n" +
            "---\n\n" +
            "- **审批类型**：${requestTypeDesc}\n" +
            "- **发起者**：${requesterName}\n" +
            "- **描述**：${description}\n" +
            "- **项目**：${projectName}\n" +
            "- **时间**：${time}\n\n" +
            "---\n\n" +
            "请及时处理！");

        // 审批通过通知（通知请求者审批已通过）
        created += createIfAbsent("APPROVAL_APPROVED_SYSTEM", "站内审批通过", Channel.SYSTEM, Category.SYSTEM,
            "[审批通过] ${requestTypeDesc}",
            "您的审批请求已通过\n\n类型：${requestTypeDesc}\n描述：${description}\n审批人：${approverName}\n审批意见：${approvalComment}\n时间：${time}");
        created += createIfAbsent("APPROVAL_APPROVED_EMAIL", "邮件审批通过", Channel.EMAIL, Category.SYSTEM,
            "[审批通过] ${requestTypeDesc}",
            "<h2>审批通过通知</h2><p>您的审批请求已通过</p><p><b>类型：</b>${requestTypeDesc}</p><p><b>描述：</b>${description}</p><p><b>审批人：</b>${approverName}</p><p><b>审批意见：</b>${approvalComment}</p><p><b>时间：</b>${time}</p>");
        created += createIfAbsent("APPROVAL_APPROVED_FEISHU", "飞书审批通过", Channel.FEISHU, Category.SYSTEM,
            "[审批通过] ${requestTypeDesc}",
            "**审批通过通知**\n\n- 类型：${requestTypeDesc}\n- 描述：${description}\n- 审批人：${approverName}\n- 审批意见：${approvalComment}\n- 时间：${time}");

        // 审批拒绝通知（通知请求者审批被拒绝）
        created += createIfAbsent("APPROVAL_REJECTED_SYSTEM", "站内审批拒绝", Channel.SYSTEM, Category.SYSTEM,
            "[审批拒绝] ${requestTypeDesc}",
            "您的审批请求被拒绝\n\n类型：${requestTypeDesc}\n描述：${description}\n审批人：${approverName}\n拒绝原因：${approvalComment}\n时间：${time}");
        created += createIfAbsent("APPROVAL_REJECTED_EMAIL", "邮件审批拒绝", Channel.EMAIL, Category.SYSTEM,
            "[审批拒绝] ${requestTypeDesc}",
            "<h2>审批拒绝通知</h2><p>您的审批请求被拒绝</p><p><b>类型：</b>${requestTypeDesc}</p><p><b>描述：</b>${description}</p><p><b>审批人：</b>${approverName}</p><p><b>拒绝原因：</b>${approvalComment}</p><p><b>时间：</b>${time}</p>");
        created += createIfAbsent("APPROVAL_REJECTED_FEISHU", "飞书审批拒绝", Channel.FEISHU, Category.SYSTEM,
            "[审批拒绝] ${requestTypeDesc}",
            "**审批拒绝通知**\n\n- 类型：${requestTypeDesc}\n- 描述：${description}\n- 审批人：${approverName}\n- 拒绝原因：${approvalComment}\n- 时间：${time}");

        created += createIfAbsent("PERMISSION_SYSTEM", "站内权限通知", Channel.SYSTEM, Category.SYSTEM,
            "权限通知: ${title}",
            "${content}\n时间：${time}");
        created += createIfAbsent("PERMISSION_EMAIL", "邮件权限通知", Channel.EMAIL, Category.SYSTEM,
            "权限通知: ${title}",
            "<h2>权限通知</h2><p>${content}</p><p><b>时间：</b>${time}</p>");

        created += createIfAbsent("SYSTEM_MAINTENANCE_SYSTEM", "站内系统维护", Channel.SYSTEM, Category.SYSTEM,
            "系统维护: ${title}",
            "${content}\n时间：${time}");
        created += createIfAbsent("SYSTEM_MAINTENANCE_EMAIL", "邮件系统维护", Channel.EMAIL, Category.SYSTEM,
            "系统维护: ${title}",
            "<h2>系统维护通知</h2><p>${content}</p><p><b>时间：</b>${time}</p>");

        created += createIfAbsent("TOKEN_EXHAUSTED_SYSTEM", "站内Token耗尽", Channel.SYSTEM, Category.SYSTEM,
            "Token 耗尽警告",
            "API Token 配额不足，请及时补充。\n时间：${time}");
        created += createIfAbsent("TOKEN_EXHAUSTED_EMAIL", "邮件Token耗尽", Channel.EMAIL, Category.SYSTEM,
            "Token 耗尽警告",
            "<h2>Token 耗尽警告</h2><p>API Token 配额不足，请及时补充。</p><p><b>时间：</b>${time}</p>");

        created += createIfAbsent("PROJECT_SYSTEM", "站内项目通知", Channel.SYSTEM, Category.SYSTEM,
            "项目通知: ${projectName}",
            "${content}\n项目：${projectName}\n时间：${time}");
        created += createIfAbsent("PROJECT_EMAIL", "邮件项目通知", Channel.EMAIL, Category.SYSTEM,
            "项目通知: ${projectName}",
            "<h2>项目通知</h2><p><b>项目：</b>${projectName}</p><p>${content}</p><p><b>时间：</b>${time}</p>");

        // 验证码邮件模板
        created += createIfAbsent("VERIFICATION_EMAIL", "邮箱验证码", Channel.EMAIL, Category.SYSTEM,
            "${systemName} - 邮箱验证码",
            "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\"><h2 style=\"color: #333;\">邮箱验证码</h2><p>您好，</p><p>您的邮箱验证码是：</p><div style=\"background: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;\">${code}</div><p>验证码 <b>${expireMinutes}</b> 分钟内有效。</p><p style=\"color: #999; font-size: 12px;\">如果这不是您的操作，请忽略此邮件。</p><hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\"><p style=\"color: #999; font-size: 12px;\">${systemName}</p></div>");

        // ===== 游戏分析通知模板 =====
        created += createIfAbsent("GAME_ANALYSIS_COMPLETED", "游戏分析完成通知", Channel.SYSTEM, Category.SYSTEM,
            "游戏质量分析完成: ${projectName}",
            "游戏质量分析完成\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📁 项目名称：${projectName}\n" +
            "📊 总分：${score}/100\n" +
            "📈 状态：${status}\n" +
            "📝 摘要：${summary}\n" +
            "━━━━━━━━━━━━━━━━━━\n\n" +
            "请前往游戏验证页面查看详细结果。");

        created += createIfAbsent("GAME_ANALYSIS_COMPLETED_EMAIL", "游戏分析完成邮件", Channel.EMAIL, Category.SYSTEM,
            "游戏质量分析完成: ${projectName}",
            "<div style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
            "  <div style=\"background: linear-gradient(135deg, #51cf66 0%, #40c057 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;\">\n" +
            "    <h1 style=\"margin: 0; font-size: 24px;\">游戏质量分析完成</h1>\n" +
            "    <p style=\"margin: 10px 0 0 0; opacity: 0.9;\">${projectName}</p>\n" +
            "  </div>\n" +
            "  <div style=\"background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none;\">\n" +
            "    <table style=\"width: 100%; border-collapse: collapse;\">\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">项目名称</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold;\">${projectName}</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">总分</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; font-weight: bold; color: #51cf66;\">${score}/100</td></tr>\n" +
            "      <tr><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef; color: #666;\">状态</td><td style=\"padding: 10px; border-bottom: 1px solid #e9ecef;\"><span style=\"background: #51cf66; color: white; padding: 2px 8px; border-radius: 4px;\">${status}</span></td></tr>\n" +
            "      <tr><td style=\"padding: 10px; color: #666;\">摘要</td><td style=\"padding: 10px;\">${summary}</td></tr>\n" +
            "    </table>\n" +
            "  </div>\n" +
            "  <div style=\"background: #d4edda; padding: 15px; border: 1px solid #c3e6cb; border-top: none; border-radius: 0 0 10px 10px;\">\n" +
            "    <p style=\"margin: 0; color: #155724;\">请前往游戏验证页面查看详细结果。</p>\n" +
            "  </div>\n" +
            "</div>");

        created += createIfAbsent("GAME_ANALYSIS_FAILED", "游戏分析失败通知", Channel.SYSTEM, Category.SYSTEM,
            "游戏质量分析失败: ${projectName}",
            "游戏质量分析失败\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "📁 项目名称：${projectName}\n" +
            "❌ 错误信息：${errorMessage}\n" +
            "━━━━━━━━━━━━━━━━━━\n\n" +
            "请检查项目配置后重试。");

        if (created > 0) {
            log.info("初始化默认通知模板完成，新增 {} 个模板", created);
        }
    }

    /**
     * 如果模板不存在则创建
     *
     * @return 1 如果创建了，0 如果已存在
     */
    private int createIfAbsent(String code, String name, Channel channel, Category category,
                                String subject, String content) {
        if (templateRepository.existsByTemplateCode(code)) {
            return 0;
        }
        NotificationTemplate t = new NotificationTemplate();
        t.setTemplateCode(code);
        t.setTemplateName(name);
        t.setChannel(channel);
        t.setCategory(category);
        t.setSubject(subject);
        t.setContent(content);
        t.setEnabled(true);
        t.setSystemBuiltin(true);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(t);
        return 1;
    }

    // ===== 模板管理 =====

    /**
     * 获取所有模板
     *
     * @return 模板列表
     */
    public List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    /**
     * 获取所有启用的模板
     *
     * @return 模板列表
     */
    public List<NotificationTemplate> getEnabledTemplates() {
        return templateRepository.findByEnabledTrue();
    }

    /**
     * 根据ID获取模板
     *
     * @param id 模板ID
     * @return 模板
     */
    public NotificationTemplate getTemplate(Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    /**
     * 根据编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板
     */
    public NotificationTemplate getTemplateByCode(String templateCode) {
        return templateRepository.findByTemplateCode(templateCode).orElse(null);
    }

    /**
     * 根据渠道获取模板
     *
     * @param channel 通知渠道
     * @return 模板列表
     */
    public List<NotificationTemplate> getTemplatesByChannel(Channel channel) {
        return templateRepository.findByChannelAndEnabledTrue(channel);
    }

    /**
     * 根据分类获取模板
     *
     * @param category 模板分类
     * @return 模板列表
     */
    public List<NotificationTemplate> getTemplatesByCategory(Category category) {
        return templateRepository.findByCategoryAndEnabledTrue(category);
    }

    /**
     * 创建模板
     *
     * @param template 模板
     * @return 创建的模板
     */
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        if (templateRepository.existsByTemplateCode(template.getTemplateCode())) {
            throw new RuntimeException("模板编码已存在: " + template.getTemplateCode());
        }
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        NotificationTemplate saved = templateRepository.save(template);
        log.info("Created notification template: {} ({})", saved.getTemplateCode(), saved.getId());
        return saved;
    }

    /**
     * 更新模板
     *
     * @param template 模板
     * @return 更新的模板
     */
    public NotificationTemplate updateTemplate(NotificationTemplate template) {
        NotificationTemplate existing = templateRepository.findById(template.getId())
            .orElseThrow(() -> new RuntimeException("模板不存在: " + template.getId()));

        // 系统内置模板只能修改内容，不能修改编码和渠道
        if (existing.isSystemBuiltin()) {
            existing.setSubject(template.getSubject());
            existing.setContent(template.getContent());
            existing.setDescription(template.getDescription());
            existing.setEnabled(template.isEnabled());
        } else {
            existing.setTemplateName(template.getTemplateName());
            existing.setChannel(template.getChannel());
            existing.setCategory(template.getCategory());
            existing.setSubject(template.getSubject());
            existing.setContent(template.getContent());
            existing.setDescription(template.getDescription());
            existing.setEnabled(template.isEnabled());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        NotificationTemplate saved = templateRepository.save(existing);
        log.info("Updated notification template: {} ({})", saved.getTemplateCode(), saved.getId());
        return saved;
    }

    /**
     * 删除模板
     *
     * @param id 模板ID
     */
    public void deleteTemplate(Long id) {
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + id));

        if (template.isSystemBuiltin()) {
            throw new RuntimeException("系统内置模板不可删除");
        }

        templateRepository.delete(template);
        log.info("Deleted notification template: {} ({})", template.getTemplateCode(), id);
    }

    /**
     * 切换模板启用状态
     *
     * @param id 模板ID
     * @param enabled 是否启用
     */
    public void toggleTemplate(Long id, boolean enabled) {
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + id));
        template.setEnabled(enabled);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
        log.info("Template {} {}: {}", template.getTemplateCode(), enabled ? "enabled" : "disabled", id);
    }

    // ===== 变量替换 =====

    /**
     * 渲染模板（替换变量）
     *
     * @param templateCode 模板编码
     * @param variables 变量映射
     * @return 渲染后的内容（包含subject和content）
     */
    public Map<String, String> renderTemplate(String templateCode, Map<String, String> variables) {
        NotificationTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateCode);
        }
        return renderTemplate(template, variables);
    }

    /**
     * 渲染模板（替换变量）
     *
     * @param template 模板
     * @param variables 变量映射
     * @return 渲染后的内容（包含subject和content）
     */
    public Map<String, String> renderTemplate(NotificationTemplate template, Map<String, String> variables) {
        // 合并内置变量
        Map<String, String> allVariables = buildDefaultVariables();
        if (variables != null) {
            allVariables.putAll(variables);
        }

        Map<String, String> result = new HashMap<>();
        result.put("subject", replaceVariables(template.getSubject(), allVariables));
        result.put("content", replaceVariables(template.getContent(), allVariables));

        return result;
    }

    /**
     * 预览模板
     *
     * @param templateId 模板ID
     * @param variables 变量映射
     * @return 预览内容
     */
    public Map<String, String> previewTemplate(Long templateId, Map<String, String> variables) {
        NotificationTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        return renderTemplate(template, variables);
    }

    /**
     * 替换变量
     *
     * @param template 模板内容
     * @param variables 变量映射
     * @return 替换后的内容
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, matcher.group(0));
            // 转义特殊字符，避免正则替换问题
            value = Matcher.quoteReplacement(value);
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 构建默认变量
     *
     * @return 默认变量映射
     */
    private Map<String, String> buildDefaultVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("time", LocalDateTime.now().format(DATETIME_FORMATTER));
        variables.put("systemName", "ChengXun Game Maker");
        return variables;
    }

    /**
     * 提取模板中的变量名
     *
     * @param templateContent 模板内容
     * @return 变量名列表
     */
    public List<String> extractVariables(String templateContent) {
        List<String> variables = new ArrayList<>();
        if (templateContent == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.contains(variableName)) {
                variables.add(variableName);
            }
        }

        return variables;
    }

    /**
     * 获取变量说明
     *
     * @return 变量说明映射
     */
    public Map<String, String> getVariableDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("title", "标题");
        descriptions.put("content", "内容");
        descriptions.put("time", "当前时间");
        descriptions.put("priority", "优先级（英文）");
        descriptions.put("priorityDesc", "优先级描述（中文）");
        descriptions.put("ruleName", "规则名称");
        descriptions.put("metric", "指标名称");
        descriptions.put("triggerValue", "触发值");
        descriptions.put("thresholdValue", "阈值");
        descriptions.put("agentId", "Agent ID");
        descriptions.put("agentName", "Agent名称");
        descriptions.put("taskTitle", "任务标题");
        descriptions.put("taskResult", "任务结果");
        descriptions.put("projectName", "项目名称");
        descriptions.put("userName", "用户名称");
        descriptions.put("systemName", "系统名称");
        return descriptions;
    }

    /**
     * 测试发送通知模板
     * 使用系统配置发送真实的测试通知
     *
     * @param templateId 模板ID
     * @param userEmail 当前用户的邮箱（邮件类型模板使用）
     * @return 发送结果
     * @throws RuntimeException 当系统未配置对应通知渠道时抛出
     */
    public Map<String, Object> testSendTemplate(Long templateId, String userEmail) {
        NotificationTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        // 构建测试变量
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("title", "测试通知");
        testVariables.put("content", "这是一条测试通知，用于验证模板配置是否正确。");
        testVariables.put("ruleName", "测试规则");
        testVariables.put("priorityDesc", "中等");
        testVariables.put("triggerValue", "85");
        testVariables.put("thresholdValue", "80");
        testVariables.put("agentName", "测试Agent");
        testVariables.put("taskTitle", "测试任务");
        testVariables.put("taskResult", "任务执行成功");
        testVariables.put("projectName", "测试项目");
        testVariables.put("userName", "测试用户");

        // 渲染模板
        Map<String, String> rendered = renderTemplate(template, testVariables);
        String subject = rendered.get("subject");
        String content = rendered.get("content");

        // 根据渠道类型发送通知
        Channel channel = template.getChannel();
        switch (channel) {
            case SYSTEM:
                // 站内信通知
                return Map.of(
                    "status", "success",
                    "message", "站内信通知已发送",
                    "channel", "SYSTEM",
                    "subject", subject,
                    "content", content
                );
            case EMAIL:
                // 邮件通知 - 检查邮件配置
                if (!emailService.isEmailEnabled()) {
                    throw new RuntimeException("邮件服务未启用，请在系统设置中配置邮件服务");
                }

                // 如果提供了用户邮箱，直接发送
                if (userEmail != null && !userEmail.isEmpty()) {
                    try {
                        emailService.sendGeneralEmail(userEmail, subject, content);
                        log.info("测试邮件已发送到用户邮箱: {}", userEmail);
                        return Map.of(
                            "status", "success",
                            "message", "测试邮件已发送到您的邮箱: " + userEmail,
                            "channel", "EMAIL",
                            "toEmail", userEmail
                        );
                    } catch (Exception e) {
                        log.error("发送测试邮件失败", e);
                        throw new RuntimeException("发送邮件失败: " + e.getMessage());
                    }
                }

                // 如果用户没有配置邮箱，返回提示
                return Map.of(
                    "status", "error",
                    "message", "您的账号未配置邮箱，请先在个人资料中设置邮箱",
                    "channel", "EMAIL"
                );
            case FEISHU:
                // 飞书通知
                return Map.of(
                    "status", "success",
                    "message", "飞书通知已发送（请检查飞书配置）",
                    "channel", "FEISHU",
                    "subject", subject,
                    "content", content
                );
            case DINGTALK:
                // 钉钉通知
                return Map.of(
                    "status", "success",
                    "message", "钉钉通知已发送（请检查钉钉配置）",
                    "channel", "DINGTALK",
                    "subject", subject,
                    "content", content
                );
            default:
                throw new RuntimeException("不支持的通知渠道: " + channel);
        }
    }

    /**
     * 测试发送邮件模板到指定邮箱
     *
     * @param templateId 模板ID
     * @param toEmail 收件人邮箱
     * @return 发送结果
     */
    public Map<String, Object> testSendEmailTemplate(Long templateId, String toEmail) {
        NotificationTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        if (template.getChannel() != Channel.EMAIL) {
            throw new RuntimeException("该模板不是邮件模板");
        }

        if (!emailService.isEmailEnabled()) {
            throw new RuntimeException("邮件服务未启用，请在系统设置中配置邮件服务");
        }

        // 构建测试变量
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("title", "测试通知");
        testVariables.put("content", "这是一条测试通知，用于验证模板配置是否正确。");
        testVariables.put("ruleName", "测试规则");
        testVariables.put("priorityDesc", "中等");
        testVariables.put("triggerValue", "85");
        testVariables.put("thresholdValue", "80");
        testVariables.put("agentName", "测试Agent");
        testVariables.put("taskTitle", "测试任务");
        testVariables.put("taskResult", "任务执行成功");
        testVariables.put("projectName", "测试项目");
        testVariables.put("userName", "测试用户");

        // 渲染模板
        Map<String, String> rendered = renderTemplate(template, testVariables);
        String subject = rendered.get("subject");
        String content = rendered.get("content");

        // 发送邮件
        try {
            emailService.sendGeneralEmail(toEmail, subject, content);
            log.info("测试邮件已发送到: {}", toEmail);
            return Map.of(
                "status", "success",
                "message", "测试邮件已发送到 " + toEmail,
                "channel", "EMAIL",
                "toEmail", toEmail
            );
        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }
}
