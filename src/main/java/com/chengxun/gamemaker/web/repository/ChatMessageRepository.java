package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI助手消息数据访问接口
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 获取会话的消息列表（按创建时间正序）
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * 统计会话消息数量
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话的所有消息
     */
    void deleteBySessionId(Long sessionId);
}
