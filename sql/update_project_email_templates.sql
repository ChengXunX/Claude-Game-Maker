-- 项目相关邮件通知模板优化：增加项目名称、创建用户、项目描述
-- 执行时间：2026-06-13

-- 通用项目信息 HTML 片段（添加到 content 的 ${content} 之前）：
-- <div style="background:#e8f4fd;padding:12px;border-radius:6px;margin-bottom:15px;border-left:4px solid #2196F3;">
--   <p style="margin:4px 0;font-size:14px;"><b>项目：</b>${projectName}</p>
--   <p style="margin:4px 0;font-size:14px;"><b>创建人：</b>${createdBy}</p>
--   <p style="margin:4px 0;font-size:14px;"><b>描述：</b>${projectDescription}</p>
-- </div>

-- 1. 需要人工审批
UPDATE notification_templates SET content =
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #ff922b 0%, #fd7e14 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔔 需要人工审批</h1>
  </div>
  <div style="background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <div style="background:#e8f4fd;padding:12px;border-radius:6px;margin-bottom:15px;border-left:4px solid #2196F3;">
      <p style="margin:4px 0;font-size:14px;"><b>项目：</b>${projectName}</p>
      <p style="margin:4px 0;font-size:14px;"><b>创建人：</b>${createdBy}</p>
      <p style="margin:4px 0;font-size:14px;"><b>描述：</b>${projectDescription}</p>
    </div>
    <p>${content}</p>
    <p style="color: #666; margin-top: 20px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/approvals" style="display: inline-block; background: #fd7e14; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">前往审批</a>
    </div>
  </div>
</div>'
WHERE template_code = 'PRODUCER_WORKFLOW_HUMAN_APPROVAL_NEEDED_EMAIL';

-- 2. 版本迭代已启动
UPDATE notification_templates SET content =
'<div style="font-family: ''Microsoft YaHei'', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="background: linear-gradient(135deg, #339af0 0%, #228be6 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;">
    <h1 style="margin: 0; font-size: 24px;">🔄 版本迭代已启动</h1>
  </div>
  <div style="background: #f8f9fa; padding: 20px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
    <div style="background:#e8f4fd;padding:12px;border-radius:6px;margin-bottom:15px;border-left:4px solid #2196F3;">
      <p style="margin:4px 0;font-size:14px;"><b>项目：</b>${projectName}</p>
      <p style="margin:4px 0;font-size:14px;"><b>创建人：</b>${createdBy}</p>
      <p style="margin:4px 0;font-size:14px;"><b>描述：</b>${projectDescription}</p>
    </div>
    <p>${content}</p>
    <p style="color: #666; margin-top: 20px;">⏰ ${time}</p>
    <div style="text-align: center; margin-top: 20px;">
      <a href="${domain}/projects" style="display: inline-block; background: #228be6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">查看项目</a>
    </div>
  </div>
</div>'
WHERE template_code = 'PRODUCER_VERSION_ITERATION_STARTED_EMAIL';
