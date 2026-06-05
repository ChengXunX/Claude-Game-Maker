---
name: Git工作流
description: Git分支管理和协作工作流
trigger: Git工作流,分支策略,Git Flow,代码合并,版本管理
examples: 分支管理|合并代码|解决冲突|版本发布
---

# Git工作流技能

## Git Flow工作流

### 分支类型
```
main        ← 生产环境代码
  ↑
release     ← 发布准备
  ↑
develop     ← 开发主线
  ↑
feature/*   ← 功能分支
hotfix/*    ← 紧急修复
```

### 分支命令
```bash
# 创建功能分支
git checkout -b feature/user-login develop

# 完成功能，合并到develop
git checkout develop
git merge --no-ff feature/user-login
git branch -d feature/user-login

# 创建发布分支
git checkout -b release/1.0.0 develop

# 发布完成后合并到main
git checkout main
git merge --no-ff release/1.0.0
git tag -a v1.0.0 -m "Release 1.0.0"

# 紧急修复
git checkout -b hotfix/critical-bug main
git checkout main
git merge --no-ff hotfix/critical-bug
git checkout develop
git merge --no-ff hotfix/critical-bug
```

## 提交规范

### Conventional Commits
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型说明
| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug修复 |
| docs | 文档 |
| style | 格式 |
| refactor | 重构 |
| test | 测试 |
| chore | 构建/工具 |

### 示例
```
feat(auth): add JWT login support

- Implement JWT token generation
- Add login API endpoint
- Add token validation filter

Closes #123
```

## 冲突解决

### 解决步骤
```bash
# 1. 拉取最新代码
git fetch origin
git merge origin/develop

# 2. 查看冲突文件
git status

# 3. 手动解决冲突后
git add .
git commit -m "merge: resolve conflicts"
```

## 常用命令

```bash
# 查看历史
git log --oneline --graph --all

# 暂存更改
git stash
git stash pop

# 撤销修改
git checkout -- file.txt
git reset HEAD file.txt

# 回滚提交
git revert <commit-hash>
git reset --soft HEAD~1
```
