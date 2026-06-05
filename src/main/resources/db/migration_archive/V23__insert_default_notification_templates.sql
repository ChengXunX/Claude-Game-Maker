-- 插入默认通知模板（使用INSERT IGNORE避免重复）

-- 告警邮件模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('ALERT_EMAIL_001', '告警邮件通知', 'EMAIL', 'ALERT', 
'[${priorityDesc}] ${title}', 
'<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
    .header { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .priority-HIGH { border-left: 4px solid #e74c3c; }
    .priority-MEDIUM { border-left: 4px solid #f39c12; }
    .priority-LOW { border-left: 4px solid #27ae60; }
    .content { padding: 15px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
    .footer { margin-top: 20px; padding-top: 15px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }
    table { width: 100%; border-collapse: collapse; margin: 10px 0; }
    td { padding: 8px; border-bottom: 1px solid #eee; }
    td:first-child { font-weight: bold; width: 120px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header priority-${priority}">
      <h2 style="margin:0;">${title}</h2>
    </div>
    <div class="content">
      <p>${content}</p>
      <table>
        <tr><td>告警规则</td><td>${ruleName}</td></tr>
        <tr><td>监控指标</td><td>${metric}</td></tr>
        <tr><td>触发值</td><td>${triggerValue}</td></tr>
        <tr><td>阈值</td><td>${thresholdValue}</td></tr>
        <tr><td>触发时间</td><td>${time}</td></tr>
      </table>
    </div>
    <div class="footer">
      <p>此邮件由 ${systemName} 自动发送，请勿回复。</p>
    </div>
  </div>
</body>
</html>',
'系统告警邮件通知模板，支持HTML格式', TRUE, TRUE);

-- 告警飞书模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('ALERT_FEISHU_001', '告警飞书通知', 'FEISHU', 'ALERT',
'${title}',
'**${title}**

告警级别：${priorityDesc}
告警规则：${ruleName}
监控指标：${metric}
触发值：${triggerValue}
阈值：${thresholdValue}
触发时间：${time}

${content}',
'系统告警飞书通知模板', TRUE, TRUE);

-- 告警站内信模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('ALERT_SYSTEM_001', '告警站内信通知', 'SYSTEM', 'ALERT',
'${title}',
'${content}

告警规则：${ruleName}
触发值：${triggerValue} / ${thresholdValue}
时间：${time}',
'系统告警站内信通知模板', TRUE, TRUE);

-- 任务完成邮件模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('TASK_EMAIL_001', '任务完成邮件通知', 'EMAIL', 'TASK',
'[任务完成] ${taskTitle}',
'<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
    .header { background-color: #27ae60; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .content { padding: 15px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
    .footer { margin-top: 20px; padding-top: 15px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h2 style="margin:0;">任务完成通知</h2>
    </div>
    <div class="content">
      <p><strong>任务标题：</strong>${taskTitle}</p>
      <p><strong>执行Agent：</strong>${agentName}</p>
      <p><strong>完成时间：</strong>${time}</p>
      <p><strong>任务结果：</strong>${taskResult}</p>
      <hr>
      <p>${content}</p>
    </div>
    <div class="footer">
      <p>此邮件由 ${systemName} 自动发送。</p>
    </div>
  </div>
</body>
</html>',
'任务完成邮件通知模板', TRUE, TRUE);

-- 任务完成飞书模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('TASK_FEISHU_001', '任务完成飞书通知', 'FEISHU', 'TASK',
'任务完成：${taskTitle}',
'**任务完成通知**

任务标题：${taskTitle}
执行Agent：${agentName}
完成时间：${time}
任务结果：${taskResult}

${content}',
'任务完成飞书通知模板', TRUE, TRUE);

-- Agent状态变更模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('AGENT_SYSTEM_001', 'Agent状态变更通知', 'SYSTEM', 'AGENT',
'Agent状态变更：${agentName}',
'Agent ${agentName} (${agentId}) 状态已变更。

${content}

时间：${time}',
'Agent状态变更站内信通知模板', TRUE, TRUE);

-- 系统通知模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('SYSTEM_EMAIL_001', '系统通知邮件', 'EMAIL', 'SYSTEM',
'[系统通知] ${title}',
'<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
    .header { background-color: #3498db; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .content { padding: 15px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
    .footer { margin-top: 20px; padding-top: 15px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h2 style="margin:0;">${title}</h2>
    </div>
    <div class="content">
      <p>${content}</p>
      <p style="color:#666;font-size:12px;">时间：${time}</p>
    </div>
    <div class="footer">
      <p>此邮件由 ${systemName} 自动发送。</p>
    </div>
  </div>
</body>
</html>',
'系统通用邮件通知模板', TRUE, TRUE);

-- 系统站内信模板
INSERT IGNORE INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin)
VALUES ('SYSTEM_SYSTEM_001', '系统站内信通知', 'SYSTEM', 'SYSTEM',
'${title}',
'${content}

时间：${time}',
'系统通用站内信通知模板', TRUE, TRUE);
