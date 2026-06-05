package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户权限仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    /** 获取用户的所有有效权限 */
    @Query("SELECT p FROM UserPermission p WHERE p.userId = :userId AND p.enabled = true " +
           "AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)")
    List<UserPermission> findValidByUserId(@Param("userId") Long userId);

    /** 获取用户的所有权限（包括过期的） */
    List<UserPermission> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 检查用户是否拥有某权限 */
    @Query("SELECT COUNT(p) > 0 FROM UserPermission p WHERE p.userId = :userId AND p.permission = :permission " +
           "AND p.enabled = true AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)")
    boolean existsByUserIdAndPermission(@Param("userId") Long userId, @Param("permission") String permission);

    /** 查找特定权限记录 */
    Optional<UserPermission> findByUserIdAndPermission(Long userId, String permission);

    /** 删除用户的所有权限 */
    void deleteByUserId(Long userId);

    /** 删除用户的特定权限 */
    void deleteByUserIdAndPermission(Long userId, String permission);

    /** 统计拥有某权限的用户数 */
    @Query("SELECT COUNT(DISTINCT p.userId) FROM UserPermission p WHERE p.permission = :permission AND p.enabled = true")
    long countByPermission(@Param("permission") String permission);

    /** 清理过期权限 */
    @Modifying
    @Query("UPDATE UserPermission p SET p.enabled = false WHERE p.expiresAt < CURRENT_TIMESTAMP AND p.enabled = true")
    int disableExpiredPermissions();
}
