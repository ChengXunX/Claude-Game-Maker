package com.chengxun.gamemaker.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 创建角色请求DTO
 * 用于创建角色接口的参数验证
 *
 * @author chengxun
 * @since 1.0.0
 */
public class CreateRoleRequest {

    /** 角色名称，必填，长度2-50，只允许大写字母和下划线 */
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度必须在2-50之间")
    private String name;

    /** 显示名称，必填，长度2-100 */
    @NotBlank(message = "显示名称不能为空")
    @Size(min = 2, max = 100, message = "显示名称长度必须在2-100之间")
    private String displayName;

    /** 角色描述，可选，最大500字符 */
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    /** 权限集合 */
    private Set<String> permissions;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}
