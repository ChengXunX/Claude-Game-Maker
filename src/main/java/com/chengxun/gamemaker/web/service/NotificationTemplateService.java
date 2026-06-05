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

    public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
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
}
