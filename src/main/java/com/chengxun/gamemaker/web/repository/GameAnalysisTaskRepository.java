package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.GameAnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏分析任务仓库
 */
@Repository
public interface GameAnalysisTaskRepository extends JpaRepository<GameAnalysisTask, String> {

    /** 按项目查询任务列表，按创建时间倒序 */
    List<GameAnalysisTask> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /** 获取项目最新的任务 */
    Optional<GameAnalysisTask> findFirstByProjectIdOrderByCreatedAtDesc(String projectId);

    /** 按状态查询任务 */
    List<GameAnalysisTask> findByStatus(String status);

    /** 删除项目的所有任务 */
    void deleteByProjectId(String projectId);
}
