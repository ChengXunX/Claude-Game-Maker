package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.entity.DeviceTrust;
import com.chengxun.gamemaker.web.repository.DeviceTrustRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceTrustService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTrustService.class);

    private final DeviceTrustRepository deviceTrustRepository;
    private final AppConfig appConfig;

    public DeviceTrustService(DeviceTrustRepository deviceTrustRepository, AppConfig appConfig) {
        this.deviceTrustRepository = deviceTrustRepository;
        this.appConfig = appConfig;
    }

    public boolean isDeviceTrustEnabled() {
        return appConfig.getSecurity().isDeviceTrustEnabled();
    }

    public int getTrustDays() {
        return appConfig.getSecurity().getDeviceTrustDays();
    }

    /**
     * 检查用户是否是首次登录（没有任何信任设备）
     */
    public boolean isFirstLogin(Long userId) {
        return deviceTrustRepository.countByUserId(userId) == 0;
    }

    /**
     * 检查设备是否已信任
     */
    public boolean isDeviceTrusted(Long userId, HttpServletRequest request) {
        if (!isDeviceTrustEnabled()) {
            return true;
        }

        String fingerprint = generateDeviceFingerprint(request);
        Optional<DeviceTrust> deviceOpt = deviceTrustRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);

        if (deviceOpt.isPresent()) {
            DeviceTrust device = deviceOpt.get();
            if (device.isExpired()) {
                deviceTrustRepository.delete(device);
                log.info("Device trust expired for user: {}, device: {}", userId, fingerprint);
                return false;
            }
            // 更新最后使用时间
            device.setLastUsedAt(LocalDateTime.now());
            deviceTrustRepository.save(device);
            return true;
        }

        return false;
    }

    /**
     * 检查设备是否需要二次验证
     * 首次登录自动信任，后续陌生设备需要验证
     */
    public DeviceCheckResult checkDevice(Long userId, HttpServletRequest request) {
        if (!isDeviceTrustEnabled()) {
            return new DeviceCheckResult(true, true, false, null);
        }

        boolean isFirstLogin = isFirstLogin(userId);
        boolean isTrusted = isDeviceTrusted(userId, request);
        String deviceName = parseDeviceName(request);

        // 首次登录：自动信任
        if (isFirstLogin) {
            trustDevice(userId, request);
            log.info("首次登录自动信任设备: user={}, device={}", userId, deviceName);
            return new DeviceCheckResult(true, true, false, deviceName);
        }

        // 已信任设备：直接放行
        if (isTrusted) {
            return new DeviceCheckResult(true, true, false, deviceName);
        }

        // 陌生设备：需要二次验证
        return new DeviceCheckResult(true, false, true, deviceName);
    }

    /**
     * 设备检查结果
     */
    public static class DeviceCheckResult {
        private final boolean enabled;      // 设备信任功能是否启用
        private final boolean trusted;      // 设备是否已信任
        private final boolean needVerify;   // 是否需要二次验证
        private final String deviceName;    // 设备名称

        public DeviceCheckResult(boolean enabled, boolean trusted, boolean needVerify, String deviceName) {
            this.enabled = enabled;
            this.trusted = trusted;
            this.needVerify = needVerify;
            this.deviceName = deviceName;
        }

        public boolean isEnabled() { return enabled; }
        public boolean isTrusted() { return trusted; }
        public boolean isNeedVerify() { return needVerify; }
        public String getDeviceName() { return deviceName; }
    }

    @Transactional
    public DeviceTrust trustDevice(Long userId, HttpServletRequest request) {
        String fingerprint = generateDeviceFingerprint(request);
        String deviceName = parseDeviceName(request);
        String ipAddress = getClientIp(request);

        Optional<DeviceTrust> existingOpt = deviceTrustRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);

        DeviceTrust device;
        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            device.setIpAddress(ipAddress);
            device.setDeviceName(deviceName);
        } else {
            device = new DeviceTrust();
            device.setUserId(userId);
            device.setDeviceFingerprint(fingerprint);
            device.setDeviceName(deviceName);
            device.setIpAddress(ipAddress);
            device.setTrustedAt(LocalDateTime.now());
        }

        int trustDays = getTrustDays();
        device.setExpiresAt(LocalDateTime.now().plusDays(trustDays));
        device.setLastUsedAt(LocalDateTime.now());

        DeviceTrust saved = deviceTrustRepository.save(device);
        log.info("Device trusted for user: {}, device: {}, expires in {} days", userId, deviceName, trustDays);
        return saved;
    }

    public List<DeviceTrust> getTrustedDevices(Long userId) {
        return deviceTrustRepository.findByUserIdOrderByLastUsedAtDesc(userId);
    }

    @Transactional
    public void removeDevice(Long userId, Long deviceId) {
        DeviceTrust device = deviceTrustRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此设备");
        }

        deviceTrustRepository.delete(device);
        log.info("Device trust removed: user={}, device={}", userId, deviceId);
    }

    @Transactional
    public void removeAllDevices(Long userId) {
        List<DeviceTrust> devices = deviceTrustRepository.findByUserIdOrderByLastUsedAtDesc(userId);
        deviceTrustRepository.deleteAll(devices);
        log.info("All device trusts removed for user: {}", userId);
    }

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点执行
    @Transactional
    public void cleanupExpiredDevices() {
        int deleted = deviceTrustRepository.deleteExpiredDevices(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired device trusts", deleted);
        }
    }

    public String generateDeviceFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        // 结合多个请求特征生成更安全的设备指纹
        StringBuilder raw = new StringBuilder();
        raw.append(userAgent);

        // 添加 Accept-Language 头
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null) {
            raw.append("|").append(acceptLanguage);
        }

        // 添加 Accept-Encoding 头
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding != null) {
            raw.append("|").append(acceptEncoding);
        }

        // 添加 X-Forwarded-For（如果存在）
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            raw.append("|").append(forwardedFor.split(",")[0].trim());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to generate device fingerprint", e);
            return String.valueOf(raw.toString().hashCode());
        }
    }

    public String parseDeviceName(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "未知设备";
        }

        // 解析浏览器
        String browser = "未知浏览器";
        if (userAgent.contains("Firefox/")) {
            browser = "Firefox";
        } else if (userAgent.contains("Edg/")) {
            browser = "Edge";
        } else if (userAgent.contains("Chrome/") && !userAgent.contains("Chromium")) {
            browser = "Chrome";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            browser = "Safari";
        }

        // 解析操作系统
        String os = "未知系统";
        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            os = "macOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            os = "iOS";
        }

        return browser + " / " + os;
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
