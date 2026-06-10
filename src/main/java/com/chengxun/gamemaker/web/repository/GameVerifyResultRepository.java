package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.GameVerifyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏验证结果仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface GameVerifyResultRepository extends JpaRepository<GameVerifyResult, Long> {

    /**
     * 获取项目的最新验证结果
     */
    Optional<GameVerifyResult> findFirstByProjectIdOrderByVerifiedAtDesc(String projectId);

    /**
     * 获取项目的最新快速验证结果
     */
    Optional<GameVerifyResult> findFirstByProjectIdAndVerifyTypeOrderByVerifiedAtDesc(String projectId, String verifyType);

    /**
     * 获取项目的所有验证结果（按时间倒序）
     */
    List<GameVerifyResult> findByProjectIdOrderByVerifiedAtDesc(String projectId);

    /**
     * 获取多个项目的最新验证结果
     */
    List<GameVerifyResult> findByProjectIdIn(List<String> projectIds);

    /**
     * 删除项目的验证结果
     */
    void deleteByProjectId(String projectId);
}
