---
name: Git专员工作流技能
description: Git专员Agent的专业工作流程技能，优化版本管理和代码提交效率
trigger: Git专员,git-commit,版本管理,代码提交,分支管理
examples: 代码提交|分支管理|合并冲突解决|版本发布|代码回滚
---

# Git专员工作流技能

## 核心职责

作为Git专员，你负责：
1. 版本管理与控制
2. 代码提交与审查
3. 分支管理与合并
4. 版本发布与标记

## Git工作流程

### 1. 日常工作流程
```
[Git日常工作]
□ 拉取最新代码
□ 检查分支状态
□ 合并功能分支
□ 解决合并冲突
□ 推送代码更新
□ 更新版本标签
```

### 2. 分支管理策略
```
分支类型：
- main：主分支，稳定版本
- develop：开发分支，最新功能
- feature/*：功能分支
- hotfix/*：紧急修复分支
- release/*：发布分支

分支命名：
- feature/add-login
- feature/fix-bug-123
- hotfix/security-patch
- release/v1.0.0
```

### 3. 提交规范
```
提交信息格式：
<type>(<scope>): <subject>

类型：
- feat：新功能
- fix：修复bug
- docs：文档更新
- style：代码格式
- refactor：重构
- test：测试
- chore：构建/工具

示例：
feat(login): 添加用户登录功能
fix(battle): 修复战斗伤害计算错误
docs(readme): 更新README文档
```

## 游戏版本管理

### 1. 版本号规范
```
语义化版本：MAJOR.MINOR.PATCH

MAJOR：不兼容的API修改
MINOR：向下兼容的功能性新增
PATCH：向下兼容的问题修正

示例：
1.0.0：正式发布
1.1.0：添加新功能
1.1.1：修复bug
2.0.0：重大更新
```

### 2. 版本标签
```
标签类型：
- v1.0.0：正式版本
- v1.0.0-rc：候选版本
- v1.0.0-beta：测试版本
- v1.0.0-alpha：内测版本

标签操作：
git tag v1.0.0
git push origin v1.0.0
```

### 3. 发布流程
```
发布步骤：
1. 创建release分支
2. 更新版本号
3. 更新CHANGELOG
4. 运行测试
5. 合并到main
6. 创建标签
7. 部署上线
8. 合并到develop
```

## Git操作指南

### 1. 常用命令
```bash
# 分支操作
git branch feature/login    # 创建分支
git checkout feature/login  # 切换分支
git checkout -b feature/login  # 创建并切换
git branch -d feature/login    # 删除分支

# 提交操作
git add .                   # 暂存所有
git commit -m "message"     # 提交
git push origin branch      # 推送
git pull origin branch      # 拉取

# 合并操作
git merge feature/login     # 合并分支
git rebase main             # 变基
git cherry-pick commit      # 摘取提交
```

### 2. 冲突解决
```
冲突类型：
- 内容冲突：同一行被修改
- 删除冲突：文件被删除
- 添加冲突：文件被添加

解决步骤：
1. 查看冲突文件
2. 手动解决冲突
3. 标记为已解决
4. 提交合并
```

### 3. 回滚操作
```
回滚类型：
- 撤销工作区：git checkout -- file
- 撤销暂存区：git reset HEAD file
- 撤销提交：git revert commit
- 重置到提交：git reset --hard commit

注意事项：
- 回滚前备份
- 通知团队成员
- 记录回滚原因
```

## 代码审查集成

### 1. Pull Request流程
```
PR流程：
1. 创建功能分支
2. 完成开发
3. 提交PR
4. 代码审查
5. 修复问题
6. 合并PR
7. 删除分支
```

### 2. PR模板
```markdown
## 变更说明
- 添加了什么功能
- 修复了什么问题
- 优化了什么性能

## 测试说明
- 如何测试
- 测试结果
- 边界情况

## 关联Issue
- #123
- #456

## 截图（如适用）
```

### 3. 审查要点
```
审查清单：
- 代码质量
- 功能正确性
- 性能影响
- 安全问题
- 测试覆盖
- 文档更新
```

## Git Hooks

### 1. 提交前检查
```bash
#!/bin/bash
# pre-commit hook

# 运行代码检查
npm run lint

# 运行测试
npm run test

# 检查提交信息
if ! grep -qE "^(feat|fix|docs|style|refactor|test|chore):" "$1"; then
  echo "提交信息格式错误"
  exit 1
fi
```

### 2. 推送前检查
```bash
#!/bin/bash
# pre-push hook

# 运行完整测试
npm run test:full

# 检查代码覆盖率
npm run coverage
```

## 常见问题

### 1. 误提交敏感信息
```
解决步骤：
1. 立即撤销提交
2. 修改敏感信息
3. 重新提交
4. 通知团队
```

### 2. 分支混乱
```
解决步骤：
1. 查看分支状态
2. 删除无用分支
3. 整理分支结构
4. 更新文档
```

### 3. 合并冲突多
```
预防措施：
1. 频繁拉取最新代码
2. 小步提交
3. 及时合并
4. 沟通协调
```

## 版本迭代职责

### 迭代开发阶段
```
[Git迭代开发清单]
□ 创建迭代分支
□ 管理功能分支
□ 审查代码提交
□ 解决合并冲突
□ 保持代码同步
```

### 迭代测试阶段
```
[Git迭代测试清单]
□ 创建发布分支
□ 修复测试发现的问题
□ 合并修复到发布分支
□ 运行完整测试
```

### 迭代评审阶段
```
[Git迭代评审清单]
□ 准备版本发布
□ 更新版本号
□ 更新CHANGELOG
□ 创建版本标签
□ 部署到测试环境
```

### 迭代发布阶段
```
[Git迭代发布清单]
□ 合并发布分支到main
□ 创建正式版本标签
□ 部署到生产环境
□ 合并回develop分支
□ 删除临时分支
```

### 迭代回顾阶段
```
[Git迭代回顾清单]
□ 总结版本管理经验
□ 记录合并冲突教训
□ 优化分支策略
□ 知识沉淀到知识库
```

## 常见错误

1. **提交信息不规范**：要遵循提交规范
2. **不拉取最新代码**：要先拉取再提交
3. **冲突解决不当**：要仔细解决冲突
4. **不删除已合并分支**：要及时清理分支
5. **误提交敏感信息**：要检查提交内容
6. **迭代分支管理混乱**：要规范分支管理
7. **版本发布不规范**：要遵循发布流程
8. **不总结经验**：要及时总结版本管理经验
