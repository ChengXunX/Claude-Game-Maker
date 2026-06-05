package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.NotificationPreference;
import com.chengxun.gamemaker.web.entity.NotificationPreference.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知偏好仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /** 获取用户的所有通知偏好 */
    List<NotificationPreference> findByUserIdOrderByNotificationTypeAscChannelAsc(Long userId);

    /** 获取用户某类型的通知偏好 */
    List<NotificationPreference> findByUserIdAndNotificationType(Long userId, String notificationType);

    /** 获取用户某渠道的通知偏好 */
    List<NotificationPreference> findByUserIdAndChannel(Long userId, Channel channel);

    /** 查找特定偏好 */
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
        Long userId, String notificationType, Channel channel);

    /** 检查用户是否启用了某类型某渠道的通知 */
    @Query("SELECT COUNT(p) > 0 FROM NotificationPreference p " +
           "WHERE p.userId = :userId AND p.notificationType = :type AND p.channel = :channel AND p.enabled = true")
    boolean isEnabled(@Param("userId") Long userId, @Param("type") String type, @Param("channel") Channel channel);

    /** 获取用户所有启用的通知偏好 */
    @Query("SELECT p FROM NotificationPreference p WHERE p.userId = :userId AND p.enabled = true")
    List<NotificationPreference> findEnabledByUserId(@Param("userId") Long userId);

    /** 删除用户的所有偏好 */
    void deleteByUserId(Long userId);
}
