package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.DeviceTrust;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTrustRepository extends JpaRepository<DeviceTrust, Long> {

    List<DeviceTrust> findByUserIdOrderByLastUsedAtDesc(Long userId);

    Optional<DeviceTrust> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);

    @Modifying
    @Query("DELETE FROM DeviceTrust d WHERE d.userId = :userId AND d.deviceFingerprint = :fingerprint")
    void deleteByUserIdAndDeviceFingerprint(@Param("userId") Long userId, @Param("fingerprint") String fingerprint);

    @Modifying
    @Query("DELETE FROM DeviceTrust d WHERE d.expiresAt < :now")
    int deleteExpiredDevices(@Param("now") LocalDateTime now);

    long countByUserId(Long userId);
}
