package com.chengxun.gamemaker.web.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责 JWT Token 的生成、验证和解析
 *
 * 主要功能：
 * - 生成 JWT Token
 * - 验证 JWT Token 有效性
 * - 解析 JWT Token 获取用户信息
 * - 支持 Token 过期时间配置
 *
 * 安全提示：
 * - 请在生产环境中配置自定义的 JWT 密钥（app.jwt.secret）
 * - 默认密钥仅用于开发环境，生产环境使用会有安全风险
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    /** 默认密钥标识，用于检测是否使用了默认密钥 */
    private static final String DEFAULT_SECRET = "GameMakerDefaultSecretKey2026!@#$%^&*()_+";

    /** JWT 密钥（从配置文件读取） */
    @Value("${app.jwt.secret:GameMakerDefaultSecretKey2026!@#$%^&*()_+}")
    private String secret;

    /** Token 过期时间（毫秒），默认 24 小时 */
    @Value("${app.jwt.expiration:86400000}")
    private long expiration;

    /**
     * 初始化方法，检查是否使用了默认密钥
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("========================================================");
            log.warn("警告: 正在使用默认的 JWT 密钥！");
            log.warn("请在 application.properties 中配置 app.jwt.secret 属性");
            log.warn("生产环境使用默认密钥会有安全风险！");
            log.warn("========================================================");
        }
    }

    /** 验证 token 过期时间（毫秒），默认 5 分钟 */
    private static final long VERIFY_TOKEN_EXPIRATION = 5 * 60 * 1000L;

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求头名称 */
    public static final String HEADER_STRING = "Authorization";

    /**
     * 获取签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @param userId 用户ID
     * @param role 用户角色
     * @return 生成的 JWT Token
     */
    public String generateToken(String username, Long userId, String role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("生成 JWT Token: username={}, userId={}, role={}", username, userId, role);
        return token;
    }

    /**
     * 从 Token 中解析 Claims
     *
     * @param token JWT Token
     * @return Claims 对象
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从 Token 解析用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("从 Token 解析用户 ID 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户角色
     *
     * @param token JWT Token
     * @return 用户角色
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("从 Token 解析用户角色失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expirationDate = claims.getExpiration();
            boolean isValid = expirationDate.after(new Date());

            if (!isValid) {
                log.warn("JWT Token 已过期: username={}", claims.getSubject());
            }

            return isValid;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("JWT Token 签名验证失败: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token 参数错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求头中提取 Token
     *
     * @param authHeader Authorization 请求头
     * @return Token（不含前缀），如果格式错误返回 null
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     *
     * @param token JWT Token
     * @return 剩余有效时间（毫秒），如果 Token 无效返回 -1
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expirationDate = claims.getExpiration();
            long remaining = expirationDate.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 生成设备验证专用 Token
     * 仅用于设备验证流程，有效期 5 分钟，包含 purpose 标识
     *
     * @param username 用户名
     * @param userId 用户ID
     * @return 验证专用 Token
     */
    public String generateVerifyToken(String username, Long userId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + VERIFY_TOKEN_EXPIRATION);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("purpose", "device-verify");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从验证 Token 中获取用户名
     * 验证 Token 必须包含 purpose=device-verify 标识
     *
     * @param verifyToken 验证专用 Token
     * @return 用户名，如果 Token 无效或不是验证专用 Token 返回 null
     */
    public String getUsernameFromVerifyToken(String verifyToken) {
        try {
            Claims claims = parseToken(verifyToken);
            String purpose = claims.get("purpose", String.class);
            if (!"device-verify".equals(purpose)) {
                log.warn("Token 不是验证专用 token: purpose={}", purpose);
                return null;
            }
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从验证 Token 解析用户名失败: {}", e.getMessage());
            return null;
        }
    }
}
