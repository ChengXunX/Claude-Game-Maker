package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.PermissionRequest;
import com.chengxun.gamemaker.web.entity.PermissionRequest.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限申请仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface PermissionRequestRepository extends JpaRepository<PermissionRequest, Long> {

    /** 获取用户的所有申请 */
    Page<PermissionRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 获取待审批申请 */
    List<PermissionRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    /** 获取待审批申请（分页） */
    Page<PermissionRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable);

    /** 统计待审批数量 */
    long countByStatus(RequestStatus status);

    /** 检查用户是否已申请某权限（待审批中） */
    boolean existsByUserIdAndPermissionAndStatus(Long userId, String permission, RequestStatus status);

    /** 获取用户的所有待审批申请 */
    List<PermissionRequest> findByUserIdAndStatus(Long userId, RequestStatus status);
}
