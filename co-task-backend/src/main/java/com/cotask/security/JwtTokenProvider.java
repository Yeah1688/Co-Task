
package com.cotask.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 生成与校验工具类
 * 负责签发、解析、验证 JWT 令牌
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:default-secret-key-for-co-tosk-must-be-at-least-256-bits-long-for-hmac-sha}") String secret,
            @Value("${jwt.access-token-validity-ms:86400000}") long accessTokenValidityMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    /**
     * 生成 JWT Access Token
     * @param userId 用户ID（作为 subject）
     * @param email  用户邮箱（作为自定义 claim）
     * @return 签名后的 JWT 字符串
     */
    public String generateAccessToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中提取用户邮箱
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    /**
     * 验证 Token 是否合法且未过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
