package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProjectDiscussionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 项目讨论消息数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface ProjectDiscussionMessageRepository extends JpaRepository<ProjectDiscussionMessage, Long> {

    /** 获取讨论的所有消息（按时间正序） */
    List<ProjectDiscussionMessage> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    /** 统计讨论的消息数量 */
    long countByDiscussionId(Long discussionId);
}
