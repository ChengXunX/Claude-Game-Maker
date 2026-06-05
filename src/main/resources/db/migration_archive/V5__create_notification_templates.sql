-- 通知模板表
-- 存储邮件、飞书等渠道的通知模板配置
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码（唯一）',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道：EMAIL, FEISHU, SYSTEM',
    category VARCHAR(30) NOT NULL COMMENT '分类：ALERT, TASK, AGENT, SYSTEM',
    subject VARCHAR(200) COMMENT '主题（邮件主题/飞书标题）',
    content TEXT NOT NULL COMMENT '内容模板',
    description VARCHAR(500) COMMENT '描述',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    system_builtin BOOLEAN DEFAULT FALSE COMMENT '是否系统内置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_channel (channel),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- 初始化默认模板 - 邮件告警模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_EMAIL', '告警邮件模板', 'EMAIL', 'ALERT',
 '[${priorityDesc}] ${ruleName} - 系统告警通知',
 '尊敬的管理员，

系统检测到告警事件，请及时处理。

告警详情：
- 规则名称：${ruleName}
- 告警级别：${priorityDesc}
- 监控指标：${metric}
- 当前值：${triggerValue}
- 阈值：${thresholdValue}
- 触发时间：${time}

${content}

请及时登录系统查看详情并处理。

---
ChengXun Game Maker',
 '用于发送告警邮件通知', TRUE, TRUE);

-- 初始化默认模板 - 飞书告警模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_FEISHU', '告警飞书模板', 'FEISHU', 'ALERT',
 '告警通知',
 '**告警通知**

规则：${ruleName}
级别：${priorityDesc}
指标：${metric}
当前值：${triggerValue}
阈值：${thresholdValue}
时间：${time}

${content}',
 '用于发送告警飞书通知', TRUE, TRUE);

-- 初始化默认模板 - 站内告警通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_SYSTEM', '告警站内通知模板', 'SYSTEM', 'ALERT',
 '[${priorityDesc}] ${ruleName}',
 '告警详情：规则[${ruleName}]，级别[${priorityDesc}]，当前值[${triggerValue}]，阈值[${thresholdValue}]',
 '用于发送告警站内通知', TRUE, TRUE);

-- 初始化默认模板 - 邮件任务通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_EMAIL', '任务邮件模板', 'EMAIL', 'TASK',
 '任务通知：${taskTitle}',
 '您好，

您有新的任务通知。

任务详情：
- 任务标题：${taskTitle}
- 任务状态：${content}
- 通知时间：${time}

请登录系统查看详情。

---
ChengXun Game Maker',
 '用于发送任务相关邮件通知', TRUE, TRUE);

-- 初始化默认模板 - 飞书任务通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_FEISHU', '任务飞书模板', 'FEISHU', 'TASK',
 '任务通知',
 '**任务通知**

任务：${taskTitle}
状态：${content}
时间：${time}',
 '用于发送任务相关飞书通知', TRUE, TRUE);

-- 初始化默认模板 - 邮件Agent通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_EMAIL', 'Agent邮件模板', 'EMAIL', 'AGENT',
 'Agent通知：${agentName}',
 '您好，

Agent有新的通知。

Agent详情：
- Agent名称：${agentName}
- 通知内容：${content}
- 通知时间：${time}

请登录系统查看详情。

---
ChengXun Game Maker',
 '用于发送Agent相关邮件通知', TRUE, TRUE);

-- 初始化默认模板 - 飞书Agent通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_FEISHU', 'Agent飞书模板', 'FEISHU', 'AGENT',
 'Agent通知',
 '**Agent通知**

Agent：${agentName}
内容：${content}
时间：${time}',
 '用于发送Agent相关飞书通知', TRUE, TRUE);

-- 初始化默认模板 - 恢复通知邮件模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_EMAIL', '恢复通知邮件模板', 'EMAIL', 'ALERT',
 '[恢复] ${ruleName} - 告警已恢复',
 '尊敬的管理员，

告警已恢复，系统运行正常。

恢复详情：
- 规则名称：${ruleName}
- 当前值：${triggerValue}
- 恢复时间：${time}

---
ChengXun Game Maker',
 '用于发送告警恢复邮件通知', TRUE, TRUE);

-- 初始化默认模板 - 恢复通知飞书模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_FEISHU', '恢复通知飞书模板', 'FEISHU', 'ALERT',
 '告警恢复',
 '**告警恢复**

规则：${ruleName}
当前值：${triggerValue}
时间：${time}',
 '用于发送告警恢复飞书通知', TRUE, TRUE);

-- 初始化默认模板 - 站内任务通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_SYSTEM', '任务站内通知模板', 'SYSTEM', 'TASK',
 '任务通知：${taskTitle}',
 '任务状态：${content}',
 '用于发送任务站内通知', TRUE, TRUE);

-- 初始化默认模板 - 站内Agent通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_SYSTEM', 'Agent站内通知模板', 'SYSTEM', 'AGENT',
 'Agent通知：${agentName}',
 '通知内容：${content}',
 '用于发送Agent站内通知', TRUE, TRUE);

-- ===== 钉钉通知模板 =====

-- 初始化默认模板 - 钉钉告警模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_DINGTALK', '告警钉钉模板', 'DINGTALK', 'ALERT',
 '告警通知',
 '### 告警通知

**规则名称**：${ruleName}
**告警级别**：${priorityDesc}
**监控指标**：${metric}
**当前值**：${triggerValue}
**阈值**：${thresholdValue}
**触发时间**：${time}

${content}',
 '用于发送告警钉钉通知', TRUE, TRUE);

-- 初始化默认模板 - 钉钉恢复通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_DINGTALK', '恢复通知钉钉模板', 'DINGTALK', 'ALERT',
 '告警恢复',
 '### 告警恢复

**规则名称**：${ruleName}
**当前值**：${triggerValue}
**恢复时间**：${time}

告警已恢复，系统运行正常。',
 '用于发送告警恢复钉钉通知', TRUE, TRUE);

-- 初始化默认模板 - 钉钉任务通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_DINGTALK', '任务钉钉模板', 'DINGTALK', 'TASK',
 '任务通知',
 '### 任务通知

**任务标题**：${taskTitle}
**任务状态**：${content}
**通知时间**：${time}',
 '用于发送任务钉钉通知', TRUE, TRUE);

-- 初始化默认模板 - 钉钉Agent通知模板
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_DINGTALK', 'Agent钉钉模板', 'DINGTALK', 'AGENT',
 'Agent通知',
 '### Agent通知

**Agent名称**：${agentName}
**通知内容**：${content}
**通知时间**：${time}',
 '用于发送Agent钉钉通知', TRUE, TRUE);
