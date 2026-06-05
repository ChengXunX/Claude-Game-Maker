package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /**
     * 根据用户名查找用户，同时加载角色和权限信息
     * 使用JOIN FETCH避免懒加载问题
     *
     * @param username 用户名
     * @return 用户信息（包含角色和权限）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    boolean existsByUsername(String username);

    List<User> findByStatus(User.UserStatus status);

    List<User> findByRole(Role role);

    long countByStatus(User.UserStatus status);

    /**
     * 根据角色名称查找用户列表
     *
     * @param roleName 角色名称
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.status = 'APPROVED'")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * 获取第一个管理员用户
     */
    @Query("SELECT u FROM User u WHERE u.role.name = 'ADMIN' AND u.status = 'APPROVED' ORDER BY u.createdAt ASC")
    User findFirstAdmin();
}
