package com.example.jobtracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key key;
    private final long expirationSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        // 确保 secret >= 32 字节（HS256/HS512都OK；越长越安全）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username) {
      //  long now = System.currentTimeMillis();
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now)) // iat

                .setExpiration(Date.from(now.plusMillis(expirationSeconds * 1000)))

                .signWith(key, SignatureAlgorithm.HS256) // 和解析时一致
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    public Instant getIssuedAt(String token) {

        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        return claims.getIssuedAt().toInstant();
    }


        private SecretKey key() {
            // 用 HMAC SHA 密钥；secret 建议放 base64 或足够长字符串
            return Keys.hmacShaKeyFor(((javax.crypto.SecretKey) key).getEncoded());
        }
    public boolean validateSignature(String token) {
        try {
            // ===== 如果你用 JJWT 0.11.x： =====
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token);

            // ===== 如果你用 JJWT 0.12.x，请改成： =====
            // Jwts.parser()
            //     .verifyWith(key())
            //     .build()
            //     .parseSignedClaims(token);

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不合法 / token 结构错误 / 等
            return false;
        }
    }
}
