package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 项目讨论会话数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface ProjectDiscussionRepository extends JpaRepository<ProjectDiscussion, Long> {

    /** 获取项目的所有讨论（按更新时间倒序） */
    List<ProjectDiscussion> findByProjectIdOrderByUpdatedAtDesc(String projectId);

    /** 获取项目进行中的讨论 */
    List<ProjectDiscussion> findByProjectIdAndStatusOrderByUpdatedAtDesc(String projectId, String status);

    /** 获取用户发起的所有讨论 */
    List<ProjectDiscussion> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** 获取已生成纪要但未同步的讨论 */
    List<ProjectDiscussion> findByStatusAndSyncedToProducerFalse(String status);
}
