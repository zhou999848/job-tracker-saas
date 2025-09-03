package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessExpSeconds;     // Access Token 有效期（秒）
    private final long refreshExpSeconds;    // Refresh Token 有效期（秒）
    private final long clockSkewSeconds;     // 允许的时钟偏差（秒）
    private final UserRepository userRepo;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-seconds:900;}") long accessExpSeconds,         // 默认 15 分钟900
            @Value("${jwt.refresh-seconds:7 * 24 * 3600;}") long refreshExpSeconds,    // 默认 7 天604800
            @Value("${jwt.clockskew-seconds:60}") long clockSkewSeconds ,       // 默认 60 秒
            UserRepository userRepo
    ) {
        this.userRepo = userRepo;
        // 建议 secret >= 32 字节
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpSeconds  = accessExpSeconds > 0 ? accessExpSeconds :900;
        this.refreshExpSeconds = refreshExpSeconds > 0 ? refreshExpSeconds :7 * 24 * 3600;
        this.clockSkewSeconds  = Math.max(0, clockSkewSeconds);
    }

    /* =========================
     * 生成 Access / Refresh
     * ========================= */

        /* =========================
         * 生成 Token
         * ========================= */
        public String generateAccessToken(String username) {
            return buildToken(username, accessExpSeconds);
        }

        public String generateRefreshToken(String username) {
            return buildToken(username, refreshExpSeconds);
        }

        private String buildToken(String username, long ttlSeconds) {
            Instant now = Instant.now();
            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(Date.from(now))                           // iat
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

        public Instant getIssuedAt(String token) {
            Date iat = parseClaims(token).getIssuedAt();
            return iat == null ? null : iat.toInstant();
        }

        /** 仅用于简单判断是否过期（不抛异常）。 */
        public boolean isTokenExpired(String token) {
            try {
                Date exp = parseClaims(token).getExpiration();
                return exp == null || exp.toInstant().isBefore(Instant.now());
            } catch (JwtException | IllegalArgumentException e) {
                return true;
            }
        }

        /** Access 校验（签名 + 未过期）。 */
        public boolean validateAccessToken(String token) {
            try {
                Claims c = parseClaims(token);
                return c.getExpiration() != null && c.getExpiration().after(new Date());
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }

        /** Refresh 校验（签名 + 未过期）。 */
        public boolean validateRefreshToken(String token) {
            try {
                Claims c = parseClaims(token);
                return c.getExpiration() != null && c.getExpiration().after(new Date());
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }

        /** ⛔ 严格 Access 校验：签名+未过期+iat > passwordChangedAt */
        public boolean validateAccessTokenStrict(String token) {
            try {
                Claims c = parseClaims(token);
                if (c.getExpiration() == null || c.getExpiration().before(new Date())) {
                    return false;
                }
                String username = c.getSubject();
                Instant tokenIat = c.getIssuedAt().toInstant();
                Instant pwdChangedAt = userRepo.findByUsername(username)
                        .map(User::getPasswordChangedAt)
                        .orElse(Instant.EPOCH);

                return tokenIat.isAfter(pwdChangedAt);
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }

        /** ⛔ 严格 Refresh 校验：签名+未过期+iat > passwordChangedAt */
        public boolean validateRefreshTokenStrict(String token) {
            try {
                Claims c = parseClaims(token);
                if (c.getExpiration() == null || c.getExpiration().before(new Date())) {
                    return false;
                }
                String username = c.getSubject();
                Instant tokenIat = c.getIssuedAt().toInstant();
                Instant pwdChangedAt = userRepo.findByUsername(username)
                        .map(User::getPasswordChangedAt)
                        .orElse(Instant.EPOCH);

                return tokenIat.isAfter(pwdChangedAt);
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }

        /** 内部解析入口（会校验签名；抛 JwtException）。 */
        private Claims parseClaims(String token) {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(clockSkewSeconds)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
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

