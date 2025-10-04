package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.example.jobtracker.repository.UserRepository; // ← 按你的实际包名调整

@Component
public class JwtUtil {

    private final Key key;
    private final long accessExpSeconds;
    private final long refreshExpSeconds;
    private final long clockSkewSeconds;
    private final UserRepository userRepo;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-seconds:900}") long accessExpSeconds,
            @Value("${jwt.refresh-seconds:604800}") long refreshExpSeconds,
            @Value("${jwt.clockskew-seconds:60}") long clockSkewSeconds,
            UserRepository userRepo
    ) {
        this.userRepo = userRepo;
        // 建议 secret >= 32 字节
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpSeconds  = accessExpSeconds > 0 ? accessExpSeconds : 900;
        this.refreshExpSeconds = refreshExpSeconds > 0 ? refreshExpSeconds : 7 * 24 * 3600;
        this.clockSkewSeconds  = Math.max(0, clockSkewSeconds);
    }

    /* =========================
     * 生成 Token（带 tenantId）
     * ========================= */
    public String generateAccessToken(UUID tenantId, String username) {
        return buildToken(username, tenantId, accessExpSeconds);
    }

    public String generateRefreshToken(UUID tenantId, String username) {
        return buildToken(username, tenantId, refreshExpSeconds);
    }

    private String buildToken(String username, UUID tenantId, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("tenantId", tenantId.toString())     // ★ 把租户放进 claim
                .setIssuedAt(Date.from(now))                // iat
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds))) // exp
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* =========================
     * 解析 / 校验
     * ========================= */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID getTenantId(String token) {
        String tid = parseClaims(token).get("tenantId", String.class);
        return (tid == null || tid.isBlank()) ? null : UUID.fromString(tid);
    }

    public Instant getIssuedAt(String token) {
        Date iat = parseClaims(token).getIssuedAt();
        return iat == null ? null : iat.toInstant();
    }

    /** 严格 Access 校验：签名 + 未过期 + iat > passwordChangedAt（按租户+用户名） */
    public boolean validateAccessTokenStrict(String token) {
        try {
            Claims c = parseClaims(token);
            Date exp = c.getExpiration();
            if (exp == null || exp.before(new Date())) return false;

            String username = c.getSubject();
            String tidStr = c.get("tenantId", String.class);
            if (tidStr == null || tidStr.isBlank()) return false;
            UUID tenantId = UUID.fromString(tidStr);

            Instant tokenIat = c.getIssuedAt() == null ? null : c.getIssuedAt().toInstant();
            if (tokenIat == null) return false;

            Instant pwdChangedAt = userRepo.findByTenantIdAndUsername( tenantId,username)
                    .map(u -> u.getPasswordChangedAt())
                    .orElse(Instant.EPOCH);

            return tokenIat.isAfter(pwdChangedAt);
        } catch (Exception e) {
            return false;
        }
    }
    public boolean validateRefreshTokenStrict(String token) {
        try {
            Claims c = parseClaims(token);
            // 1) 过期检查
            if (c.getExpiration() == null || c.getExpiration().before(new Date())) {
                return false;
            }
            // 2) 取出 username + tenantId（从 claim）
            String username = c.getSubject();
            String tidStr = c.get("tenantId", String.class);
            if (tidStr == null || tidStr.isBlank()) return false;
            UUID tenantId = UUID.fromString(tidStr);

            // 3) iat 与密码变更时间对比（密码变更后旧 Refresh 失效）
            Instant tokenIat = c.getIssuedAt() == null ? Instant.EPOCH : c.getIssuedAt().toInstant();
            Instant pwdChangedAt = userRepo.findByTenantIdAndUsername( tenantId,username)
                    .map(User::getPasswordChangedAt)
                    .orElse(Instant.EPOCH);

            return tokenIat.isAfter(pwdChangedAt);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    /** 内部解析入口（会校验签名；抛 JwtException） */
    private Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .build()
                .parseClaimsJws(token);
        return jws.getBody();
    }
    public Claims getClaims(String token) {
        return parseClaims(token);
    }

    /* =========================
     * 从 Cookie 读取指定 token
     * ========================= */
    public String resolveToken(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v;
            }
        }
        return null;
    }
}



