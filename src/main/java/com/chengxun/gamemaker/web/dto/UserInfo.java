package com.chengxun.gamemaker.web.dto;

/**
 * 用户信息DTO
 * 用于在控制器中传递当前登录用户的信息
 *
 * @author chengxun
 * @since 1.0.0
 */
public class UserInfo {

    /** 用户ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    /** 用户角色名称 */
    private final String roleName;

    /**
     * 构造函数
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param roleName 角色名称
     */
    public UserInfo(Long userId, String username, String roleName) {
        this.userId = userId;
        this.username = username;
        this.roleName = roleName;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取角色名称
     *
     * @return 角色名称
     */
    public String getRoleName() {
        return roleName;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}
