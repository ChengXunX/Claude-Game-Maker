package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 获取用户的通知列表（按时间倒序）
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 获取用户的通知列表
     */
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    /**
     * 获取用户已读通知列表
     */
    Page<Notification> findByUserIdAndReadTrue(Long userId, Pageable pageable);

    /**
     * 获取用户未读通知列表
     */
    Page<Notification> findByUserIdAndReadFalse(Long userId, Pageable pageable);

    /**
     * 获取用户未读通知数量
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * 获取用户未读通知列表
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 标记用户所有通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 删除用户指定天数之前的通知
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.createdAt < :beforeDate")
    int deleteOldNotifications(@Param("userId") Long userId, @Param("beforeDate") java.time.LocalDateTime beforeDate);

    /**
     * 删除无效通知（标题或内容为空或为"-"的通知）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.title IS NULL OR n.title = '' OR n.title = '-' OR n.content IS NULL OR n.content = '' OR n.content = '-'")
    int deleteInvalidNotifications();
}
