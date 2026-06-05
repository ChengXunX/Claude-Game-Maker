package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Git仓库数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {

    /**
     * 根据项目ID查找所有仓库
     */
    List<GitRepository> findByProjectIdOrderByRepositoryType(String projectId);

    /**
     * 根据项目ID和仓库类型查找
     */
    Optional<GitRepository> findByProjectIdAndRepositoryType(String projectId, String repositoryType);

    /**
     * 根据项目ID和仓库名称查找
     */
    Optional<GitRepository> findByProjectIdAndName(String projectId, String name);

    /**
     * 查找活跃状态的仓库
     */
    List<GitRepository> findByProjectIdAndStatus(String projectId, String status);

    /**
     * 根据仓库类型查找
     */
    List<GitRepository> findByRepositoryType(String repositoryType);

    /**
     * 统计项目的仓库数量
     */
    @Query("SELECT COUNT(g) FROM GitRepository g WHERE g.projectId = :projectId")
    Long countByProjectId(@Param("projectId") String projectId);

    /**
     * 查找启用了自动审查的仓库
     */
    @Query("SELECT g FROM GitRepository g WHERE g.projectId = :projectId AND g.autoReviewEnabled = true AND g.status = 'ACTIVE'")
    List<GitRepository> findActiveWithAutoReview(@Param("projectId") String projectId);
}
