package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectTokenBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目 Token 绑定数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ProjectTokenBindingRepository extends JpaRepository<ProjectTokenBinding, Long> {

    /** 查找项目的所有 Token 绑定 */
    List<ProjectTokenBinding> findByProjectIdOrderByPriorityAsc(String projectId);

    /** 查找项目的默认 Token 绑定 */
    Optional<ProjectTokenBinding> findByProjectIdAndIsDefaultTrue(String projectId);

    /** 查找使用某 Token 的所有项目 */
    List<ProjectTokenBinding> findByTokenId(Long tokenId);

    /** 检查项目是否绑定了某 Token */
    boolean existsByProjectIdAndTokenId(String projectId, Long tokenId);
}
