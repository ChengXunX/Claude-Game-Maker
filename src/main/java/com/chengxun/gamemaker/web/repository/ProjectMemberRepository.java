package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目成员数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** 查找用户在指定项目中的成员记录 */
    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, Long userId);

    /** 查找指定项目的所有成员 */
    List<ProjectMember> findByProjectId(String projectId);

    /** 查找用户参与的所有项目 */
    List<ProjectMember> findByUserId(Long userId);

    /** 查找指定项目中指定角色的成员 */
    List<ProjectMember> findByProjectIdAndRole(String projectId, ProjectMember.ProjectRole role);

    /** 检查用户是否是项目成员 */
    boolean existsByProjectIdAndUserId(String projectId, Long userId);

    /** 删除项目的所有成员 */
    void deleteByProjectId(String projectId);
}
