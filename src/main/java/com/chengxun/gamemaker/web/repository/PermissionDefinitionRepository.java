package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.PermissionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限定义仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface PermissionDefinitionRepository extends JpaRepository<PermissionDefinition, Long> {

    /** 获取所有启用的权限定义（按分类和排序） */
    List<PermissionDefinition> findByEnabledTrueOrderByCategoryAscSortOrderAsc();

    /** 获取所有权限定义（管理界面用） */
    List<PermissionDefinition> findAllByOrderByCategoryAscSortOrderAsc();

    /** 按分类查询 */
    List<PermissionDefinition> findByCategoryAndEnabledTrueOrderBySortOrderAsc(String category);

    /** 按标识查找 */
    Optional<PermissionDefinition> findByPermissionKey(String permissionKey);

    /** 检查标识是否存在 */
    boolean existsByPermissionKey(String permissionKey);

    /** 获取所有分类 */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.category FROM PermissionDefinition d WHERE d.enabled = true ORDER BY d.category")
    List<String> findDistinctCategories();
}
