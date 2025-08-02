package com.example.jobtracker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);//每次重新运行后token会变化

    public String generateToken(String username) {//生ｃ成包含用hu名的JWTtoken有效期1天
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 天
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }


    public String getUsernameFromToken(String token) {//用解析器从JWTtoken提取用hu名、用途"每人只能访问自己的数据”
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}