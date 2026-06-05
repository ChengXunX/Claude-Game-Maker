package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI助手会话数据访问接口
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 获取用户的会话列表（按更新时间倒序）
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * 统计用户会话数量
     */
    long countByUserId(Long userId);

    /**
     * 删除用户的所有会话
     */
    void deleteByUserId(Long userId);
}
