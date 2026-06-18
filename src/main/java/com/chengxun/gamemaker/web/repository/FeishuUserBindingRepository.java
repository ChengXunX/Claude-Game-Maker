package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.FeishuUserBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 飞书用户绑定数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface FeishuUserBindingRepository extends JpaRepository<FeishuUserBinding, Long> {

    /**
     * 根据 open_id 和状态查找绑定记录
     */
    Optional<FeishuUserBinding> findByOpenIdAndStatus(String openId, FeishuUserBinding.BindingStatus status);

    /**
     * 根据 open_id 查找绑定记录
     */
    Optional<FeishuUserBinding> findByOpenId(String openId);

    /**
     * 根据验证码和状态查找绑定记录
     */
    Optional<FeishuUserBinding> findByBindingCodeAndStatus(String code, FeishuUserBinding.BindingStatus status);

    /**
     * 删除指定用户和状态的绑定记录
     */
    void deleteByUserIdAndStatus(Long userId, FeishuUserBinding.BindingStatus status);

    /**
     * 根据用户 ID 和状态查找绑定记录
     */
    Optional<FeishuUserBinding> findByUserIdAndStatus(Long userId, FeishuUserBinding.BindingStatus status);
}
