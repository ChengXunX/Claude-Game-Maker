package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.TokenUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Token 使用记录 Repository
 * 支持滑动窗口配额查询
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface TokenUsageRecordRepository extends JpaRepository<TokenUsageRecord, Long> {

    /**
     * 查询指定 Token 在某个时间点之后的累计使用量
     * 用于滑动窗口配额计算
     *
     * @param tokenId Token ID
     * @param since   起始时间
     * @return 累计使用量（token 数）
     */
    @Query("SELECT COALESCE(SUM(r.tokensUsed), 0) FROM TokenUsageRecord r WHERE r.tokenId = :tokenId AND r.usedAt >= :since")
    long sumUsageSince(@Param("tokenId") Long tokenId, @Param("since") LocalDateTime since);

    /**
     * 删除指定时间之前的使用记录
     * 用于定期清理过期数据
     *
     * @param before 时间阈值
     */
    void deleteByUsedAtBefore(LocalDateTime before);
}
