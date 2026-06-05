package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.ProjectMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 项目权限服务
 * 管理项目级别的成员和权限控制
 *
 * 权限模型：
 * - 管理员（ADMIN 角色）自动拥有所有项目的完全权限
 * - 项目成员通过 project_members 表配置角色
 * - 角色层级：OWNER > MANAGER > DEVELOPER > VIEWER
 * - 权限校验用代码调用（非注解），因为项目不是 JPA 实体
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class ProjectPermissionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectPermissionService.class);

    private final ProjectMemberRepository memberRepository;

    public ProjectPermissionService(ProjectMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // ===== 权限校验 =====

    /**
     * 检查用户是否有项目的指定权限
     *
     * @param user 用户
     * @param projectId 项目 ID
     * @param requiredRole 需要的最低角色
     * @return 是否有权限
     */
    public boolean hasProjectAccess(User user, String projectId, ProjectMember.ProjectRole requiredRole) {
        if (user == null || projectId == null) return false;

        // 管理员自动拥有所有项目权限
        if (user.isAdmin()) return true;

        // 检查项目成员记录
        Optional<ProjectMember> member = memberRepository.findByProjectIdAndUserId(projectId, user.getId());
        return member.isPresent() && member.get().hasPermission(requiredRole);
    }

    /**
     * 检查用户是否是项目成员（任意角色）
     */
    public boolean isProjectMember(User user, String projectId) {
        if (user == null || projectId == null) return false;
        if (user.isAdmin()) return true;
        return memberRepository.existsByProjectIdAndUserId(projectId, user.getId());
    }

    /**
     * 获取用户在项目中的角色
     *
     * @param user 用户
     * @param projectId 项目 ID
     * @return 项目角色，不是成员返回 null
     */
    public ProjectMember.ProjectRole getUserProjectRole(User user, String projectId) {
        if (user == null || projectId == null) return null;

        // 管理员视为 OWNER
        if (user.isAdmin()) return ProjectMember.ProjectRole.OWNER;

        Optional<ProjectMember> member = memberRepository.findByProjectIdAndUserId(projectId, user.getId());
        return member.map(ProjectMember::getRole).orElse(null);
    }

    // ===== 成员管理 =====

    /**
     * 添加项目成员
     *
     * @param projectId 项目 ID
     * @param userId 用户 ID
     * @param role 项目角色
     * @return 创建的成员记录
     */
    @Transactional
    public ProjectMember addMember(String projectId, Long userId, ProjectMember.ProjectRole role) {
        Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(projectId, userId);
        if (existing.isPresent()) {
            // 更新角色
            ProjectMember member = existing.get();
            member.setRole(role);
            return memberRepository.save(member);
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        ProjectMember saved = memberRepository.save(member);
        log.info("Added member to project {}: userId={}, role={}", projectId, userId, role);
        return saved;
    }

    /**
     * 移除项目成员
     */
    @Transactional
    public void removeMember(String projectId, Long userId) {
        memberRepository.findByProjectIdAndUserId(projectId, userId)
            .ifPresent(member -> {
                memberRepository.delete(member);
                log.info("Removed member from project {}: userId={}", projectId, userId);
            });
    }

    /**
     * 更新成员角色
     */
    @Transactional
    public ProjectMember updateMemberRole(String projectId, Long userId, ProjectMember.ProjectRole newRole) {
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不是项目成员"));

        member.setRole(newRole);
        ProjectMember saved = memberRepository.save(member);
        log.info("Updated member role in project {}: userId={}, newRole={}", projectId, userId, newRole);
        return saved;
    }

    /**
     * 获取项目的所有成员
     */
    public List<ProjectMember> getProjectMembers(String projectId) {
        return memberRepository.findByProjectId(projectId);
    }

    /**
     * 获取用户参与的所有项目
     */
    public List<ProjectMember> getUserProjects(Long userId) {
        return memberRepository.findByUserId(userId);
    }

    /**
     * 获取用户的项目 ID 列表
     */
    public List<String> getUserProjectIds(Long userId) {
        return memberRepository.findByUserId(userId).stream()
            .map(ProjectMember::getProjectId)
            .toList();
    }

    /**
     * 删除项目的所有成员（项目删除时调用）
     */
    @Transactional
    public void removeAllMembers(String projectId) {
        memberRepository.deleteByProjectId(projectId);
        log.info("Removed all members from project: {}", projectId);
    }
}
