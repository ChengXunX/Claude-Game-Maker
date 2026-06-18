-- ============================================
-- 通知模板美化 v2
-- 新增变量：${domain}、${projectLink}、${approvalLink}、${agentLink}
-- 美化样式：更好的排版、颜色、按钮
-- ============================================

-- 先删除旧的飞书和邮件模板
DELETE FROM notification_templates WHERE channel IN ('FEISHU', 'EMAIL');

-- ============================================
-- 飞书通知模板（美化版）
-- ============================================

-- 审批相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_NEW_FEISHU', '飞书新审批请求', 'FEISHU', 'SYSTEM', '🔔 新审批请求',
'**📋 审批请求**

---

**审批类型**：${requestTypeDesc}
**发起者**：${requesterName}
**项目**：${projectName}

**描述**：${description}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

💡 请点击卡片底部按钮或前往管理后台处理审批',
'新审批请求飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_APPROVED_FEISHU', '飞书审批通过', 'FEISHU', 'SYSTEM', '✅ 审批通过',
'**✅ 审批通过通知**

---

**类型**：${requestTypeDesc}
**描述**：${description}
**审批人**：${approverName}
**审批意见**：${approvalComment}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批通过飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_REJECTED_FEISHU', '飞书审批拒绝', 'FEISHU', 'SYSTEM', '❌ 审批拒绝',
'**❌ 审批拒绝通知**

---

**类型**：${requestTypeDesc}
**描述**：${description}
**审批人**：${approverName}
**拒绝原因**：${approvalComment}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批拒绝飞书通知', TRUE, TRUE);

-- Agent 相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_FEISHU', '飞书Agent通知', 'FEISHU', 'AGENT', '🤖 Agent通知',
'**🤖 Agent通知**

---

**Agent**：${agentName}
**状态**：${status}

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent相关飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_STATUS_FEISHU', '飞书Agent状态变更', 'FEISHU', 'AGENT', '🔄 Agent状态变更',
'**🔄 Agent状态变更**

---

**Agent名称**：${agentName}
**当前状态**：${status}
**备注**：${remark}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent状态变更飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AGENT_CREATED_FEISHU', '飞书Agent已创建', 'FEISHU', 'AGENT', '🤖 Agent已创建',
'**🤖 Agent已创建**

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'Agent已创建飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AGENT_EVALUATED_FEISHU', '飞书Agent评估完成', 'FEISHU', 'AGENT', '📊 Agent评估完成',
'**📊 Agent评估完成**

---

${content}

---

⏰ ${time}

🔗 [查看评估](${domain}/performance)',
'Agent评估完成飞书通知', TRUE, TRUE);

-- 告警相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_FEISHU', '飞书告警通知', 'FEISHU', 'ALERT', '🚨 告警通知',
'**🚨 告警通知**

---

**告警规则**：${ruleName}
**告警级别**：${priorityDesc}
**指标**：${metric}
**当前值**：${triggerValue}
**阈值**：${thresholdValue}

---

${content}

---

⏰ ${time}

🔗 [查看告警](${domain}/alerts)

⚠️ 请及时处理告警，避免影响系统稳定性',
'告警飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_FEISHU', '飞书告警恢复', 'FEISHU', 'ALERT', '✅ 告警恢复',
'**✅ 告警恢复**

---

**告警规则**：${ruleName}
**当前值**：${triggerValue}

---

⏰ ${time}

🔗 [查看告警](${domain}/alerts)

✅ 系统已恢复正常',
'告警恢复飞书通知', TRUE, TRUE);

-- 任务相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_FEISHU', '飞书任务通知', 'FEISHU', 'TASK', '📋 任务通知',
'**📋 任务通知**

---

**任务**：${taskTitle}
**状态**：${status}

---

${content}

---

⏰ ${time}

🔗 [查看任务](${domain}/scheduler)',
'任务飞书通知', TRUE, TRUE);

-- 审批流程相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_HUMAN_APPROVAL_NEEDED_FEISHU', '飞书需要人工审批', 'FEISHU', 'SYSTEM', '🔔 需要人工审批',
'**🔔 需要人工审批**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'需要人工审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_APPROVAL_REQUIRED_FEISHU', '飞书招聘审批', 'FEISHU', 'SYSTEM', '👥 招聘审批请求',
'**👥 招聘审批请求**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批是否允许招聘该 Agent。',
'招聘审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_CREATE_AGENT_APPROVAL_REQUIRED_FEISHU', '飞书创建Agent审批', 'FEISHU', 'SYSTEM', '🤖 创建Agent审批',
'**🤖 创建Agent审批请求**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批。',
'创建Agent审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DELIVERY_APPROVAL_FEISHU', '飞书交付审批', 'FEISHU', 'SYSTEM', '📦 交付审批',
'**📦 交付审批请求**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请审批是否可以交付。',
'交付审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_APPROVAL_REQUIRED_FEISHU', '飞书需要审批', 'FEISHU', 'SYSTEM', '📋 需要审批',
'**📋 需要审批**

---

${content}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'需要审批飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_APPROVAL_REJECTED_FEISHU', '飞书审批被拒绝', 'FEISHU', 'SYSTEM', '❌ 审批被拒绝',
'**❌ 审批被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'审批被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_REJECTED_FEISHU', '飞书招聘被拒绝', 'FEISHU', 'SYSTEM', '❌ 招聘被拒绝',
'**❌ 招聘被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'招聘被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_CREATE_AGENT_REJECTED_FEISHU', '飞书创建Agent被拒绝', 'FEISHU', 'SYSTEM', '❌ 创建Agent被拒绝',
'**❌ 创建Agent被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'创建Agent被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DISMISS_REJECTED_FEISHU', '飞书解雇被拒绝', 'FEISHU', 'SYSTEM', '❌ 解雇被拒绝',
'**❌ 解雇被拒绝**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)',
'解雇被拒绝飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_DISMISS_REQUEST_SENT_FEISHU', '飞书解雇请求已发送', 'FEISHU', 'SYSTEM', '📤 解雇请求已发送',
'**📤 解雇请求已发送**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/approvals)

等待管理员审批。',
'解雇请求已发送飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_RECRUIT_COMPLETED_FEISHU', '飞书招聘已完成', 'FEISHU', 'AGENT', '✅ 招聘已完成',
'**✅ 招聘已完成**

---

${content}

---

⏰ ${time}

🔗 [查看Agent](${domain}/agents)',
'招聘已完成飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_AUTO_RECRUIT_REQUEST_FEISHU', '飞书自动招聘请求', 'FEISHU', 'AGENT', '🔄 自动招聘请求',
'**🔄 自动招聘请求**

---

${content}

---

⏰ ${time}

🔗 [查看详情](${domain}/agents)',
'自动招聘请求飞书通知', TRUE, TRUE);

-- 项目状态相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_MILESTONE_STUCK_FEISHU', '飞书里程碑卡住', 'FEISHU', 'SYSTEM', '⚠️ 里程碑卡住',
'**⚠️ 里程碑卡住**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

请关注项目进展。',
'里程碑卡住飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PROJECT_STUCK_FEISHU', '飞书项目卡住', 'FEISHU', 'SYSTEM', '🚨 项目卡住',
'**🚨 项目卡住**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)

项目进展受阻，请及时干预。',
'项目卡住飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PROJECT_RULES_GENERATED_FEISHU', '飞书项目规则已生成', 'FEISHU', 'SYSTEM', '📜 项目规则已生成',
'**📜 项目规则已生成**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'项目规则已生成飞书通知', TRUE, TRUE);

-- 版本迭代相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_ITERATION_STARTED_FEISHU', '飞书版本迭代已启动', 'FEISHU', 'SYSTEM', '🔄 版本迭代已启动',
'**🔄 版本迭代已启动**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'版本迭代已启动飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_STAFFING_ISSUE_FEISHU', '飞书人员配置问题', 'FEISHU', 'SYSTEM', '👥 人员配置问题',
'**👥 版本人员配置问题**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'人员配置问题飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_LOW_PERFORMANCE_FEISHU', '飞书版本低绩效', 'FEISHU', 'SYSTEM', '📉 版本低绩效',
'**📉 版本低绩效预警**

---

${content}

---

⏰ ${time}

🔗 [查看绩效](${domain}/performance)',
'版本低绩效飞书通知', TRUE, TRUE);

-- 工作流相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_STARTED_FEISHU', '飞书工作流已启动', 'FEISHU', 'SYSTEM', '🚀 工作流已启动',
'**🚀 工作流已启动**

---

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'工作流已启动飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_CREATED_FEISHU', '飞书新工作流已创建', 'FEISHU', 'SYSTEM', '📋 新工作流已创建',
'**📋 新工作流已创建**

---

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'新工作流已创建飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_PRODUCER_APPROVED_FEISHU', '飞书工作流已自动审批', 'FEISHU', 'SYSTEM', '✅ 工作流已自动审批',
'**✅ 工作流已自动审批**

---

${content}

---

⏰ ${time}

🔗 [查看工作流](${domain}/workflow)',
'工作流已自动审批飞书通知', TRUE, TRUE);

-- 团队相关
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_TEAM_OPTIMIZATION_FEISHU', '飞书团队优化', 'FEISHU', 'AGENT', '🔧 团队优化建议',
'**🔧 团队优化建议**

---

${content}

---

⏰ ${time}

🔗 [查看团队](${domain}/agents)',
'团队优化飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_TEAM_WARNING_FEISHU', '飞书团队警告', 'FEISHU', 'SYSTEM', '⚠️ 团队警告',
'**⚠️ 团队警告**

---

${content}

---

⏰ ${time}

🔗 [查看团队](${domain}/agents)',
'团队警告飞书通知', TRUE, TRUE);

-- 其他
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_PERIODIC_VERIFY_FAILED_FEISHU', '飞书定期验证失败', 'FEISHU', 'SYSTEM', '❌ 定期验证失败',
'**❌ 定期验证失败**

---

${content}

---

⏰ ${time}

🔗 [查看项目](${domain}/projects)',
'定期验证失败飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('DISMISSAL_REQUEST_FEISHU', '飞书解雇审批请求', 'FEISHU', 'SYSTEM', '🔔 解雇审批请求',
'**🔔 解雇审批请求**

---

**Agent名称**：${agentName}
**申请原因**：${reason}
**申请人**：${requesterName}

---

⏰ ${time}

🔗 [前往审批](${domain}/approvals)

请及时处理！',
'解雇审批请求飞书通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PERFORMANCE_FEISHU', '飞书绩效通知', 'FEISHU', 'SYSTEM', '📊 绩效通知',
'**📊 绩效通知**

---

${content}

---

⏰ ${time}

🔗 [查看绩效](${domain}/performance)',
'绩效飞书通知', TRUE, TRUE);

-- ============================================
-- 邮件通知模板（美化版）
-- ============================================

-- 通用邮件样式头
-- <div style="font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
--   <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
--     <h1 style="margin: 0; font-size: 24px;">标题</h1>
--   </div>
--   <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
--     内容
--     <div style="text-align: center; margin-top: 20px;">
--       <a href="${domain}/xxx" style="display: inline-block; background: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">按钮文字</a>
--     </div>
--   </div>
-- </div>

-- 审批相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_NEW_EMAIL', '邮件新审批请求', 'EMAIL', 'SYSTEM', '🔔 [审批] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📋 新审批请求</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>审批类型：</strong>${requestTypeDesc}</p>
    <p><strong>发起者：</strong>${requesterName}</p>
    <p><strong>项目：</strong>${projectName}</p>
    <p><strong>描述：</strong>${description}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #f5576c; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">前往审批</a>
    </div>
  </div>
</div>',
'新审批请求邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_APPROVED_EMAIL', '邮件审批通过', 'EMAIL', 'SYSTEM', '✅ [审批通过] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">✅ 审批通过</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>类型：</strong>${requestTypeDesc}</p>
    <p><strong>描述：</strong>${description}</p>
    <p><strong>审批人：</strong>${approverName}</p>
    <p><strong>审批意见：</strong>${approvalComment}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #43e97b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看详情</a>
    </div>
  </div>
</div>',
'审批通过邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('APPROVAL_REJECTED_EMAIL', '邮件审批拒绝', 'EMAIL', 'SYSTEM', '❌ [审批拒绝] ${requestTypeDesc}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">❌ 审批拒绝</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>类型：</strong>${requestTypeDesc}</p>
    <p><strong>描述：</strong>${description}</p>
    <p><strong>审批人：</strong>${approverName}</p>
    <p><strong>拒绝原因：</strong>${approvalComment}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #ff6b6b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看详情</a>
    </div>
  </div>
</div>',
'审批拒绝邮件通知', TRUE, TRUE);

-- Agent 相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('AGENT_EMAIL', '邮件Agent通知', 'EMAIL', 'AGENT', '🤖 Agent通知: ${agentName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🤖 Agent通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>Agent：</strong>${agentName}</p>
    <p><strong>状态：</strong>${status}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/agents" style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看Agent</a>
    </div>
  </div>
</div>',
'Agent邮件通知', TRUE, TRUE);

-- 告警相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('ALERT_EMAIL', '邮件告警通知', 'EMAIL', 'ALERT', '🚨 [${priorityDesc}] ${ruleName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🚨 告警通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>告警规则：</strong>${ruleName}</p>
    <p><strong>告警级别：</strong>${priorityDesc}</p>
    <p><strong>指标：</strong>${metric}</p>
    <p><strong>当前值：</strong>${triggerValue}</p>
    <p><strong>阈值：</strong>${thresholdValue}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/alerts" style="display: inline-block; background: #ff6b6b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看告警</a>
    </div>
    <p style="color: #dc3545; font-weight: bold; margin-top: 15px;">⚠️ 请及时处理告警，避免影响系统稳定性</p>
  </div>
</div>',
'告警邮件通知', TRUE, TRUE);

INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('RECOVERY_EMAIL', '邮件告警恢复', 'EMAIL', 'ALERT', '✅ [恢复] ${ruleName}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">✅ 告警恢复</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>告警规则：</strong>${ruleName}</p>
    <p><strong>当前值：</strong>${triggerValue}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/alerts" style="display: inline-block; background: #43e97b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看告警</a>
    </div>
    <p style="color: #28a745; font-weight: bold; margin-top: 15px;">✅ 系统已恢复正常</p>
  </div>
</div>',
'告警恢复邮件通知', TRUE, TRUE);

-- 任务相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('TASK_EMAIL', '邮件任务通知', 'EMAIL', 'TASK', '📋 任务通知: ${taskTitle}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📋 任务通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p><strong>任务：</strong>${taskTitle}</p>
    <p><strong>状态：</strong>${status}</p>
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/scheduler" style="display: inline-block; background: #a18cd1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看任务</a>
    </div>
  </div>
</div>',
'任务邮件通知', TRUE, TRUE);

-- 项目相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_WORKFLOW_HUMAN_APPROVAL_NEEDED_EMAIL', '邮件需要人工审批', 'EMAIL', 'SYSTEM', '🔔 需要人工审批: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔔 需要人工审批</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #f5576c; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">前往审批</a>
    </div>
  </div>
</div>',
'需要人工审批邮件通知', TRUE, TRUE);

-- 版本迭代相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PRODUCER_VERSION_ITERATION_STARTED_EMAIL', '邮件版本迭代已启动', 'EMAIL', 'SYSTEM', '🔄 版本迭代已启动: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔄 版本迭代已启动</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/projects" style="display: inline-block; background: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看项目</a>
    </div>
  </div>
</div>',
'版本迭代已启动邮件通知', TRUE, TRUE);

-- 绩效相关邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('PERFORMANCE_EMAIL', '邮件绩效通知', 'EMAIL', 'SYSTEM', '📊 绩效通知: ${title}',
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📊 绩效通知</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>${content}</p>
    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 20px 0;">
    <p style="color: #666; font-size: 14px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/performance" style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看绩效</a>
    </div>
  </div>
</div>',
'绩效邮件通知', TRUE, TRUE);

-- 验证码邮件
INSERT INTO notification_templates (template_code, template_name, channel, category, subject, content, description, enabled, system_builtin) VALUES
('VERIFICATION_EMAIL', '邮件验证码', 'EMAIL', 'SYSTEM', '${systemName} - 邮箱验证码',
'<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">📧 邮箱验证码</h1>
  </div>
  <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <p>您好，</p>
    <p>您的邮箱验证码是：</p>
    <div style="text-align: center; margin: 30px 0;">
      <span style="font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px;">${code}</span>
    </div>
    <p>验证码有效期为 ${expireMinutes} 分钟，请尽快使用。</p>
    <p style="color: #dc3545; font-size: 14px;">⚠️ 如果这不是您的操作，请忽略此邮件。</p>
  </div>
</div>',
'邮箱验证码通知', TRUE, TRUE);

-- ============================================
-- 更新 SQL 脚本文件
-- ============================================
-- 请将以上内容更新到 sql_init_data_mysql.sql 和 sql_init_data_h2.sql
