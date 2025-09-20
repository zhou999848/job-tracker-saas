package com.example.jobtracker.web;
import com.example.jobtracker.dto.LoginRequest;
import com.example.jobtracker.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public AuthController(JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

    /**
     * ✅ 刷新令牌接口 / Token Refresh
     * [POST] /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response,
    LoginRequest req) {
        String refresh = jwtUtil.resolveToken(request, "REFRESH");

        if (refresh == null || !jwtUtil.validateRefreshTokenStrict(refresh)) {
            clearAllAuthCookies(response);
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh"));
        }

        String username = jwtUtil.getUsername(refresh);

        // ✅ 新 Access
        String newAccess = jwtUtil.generateAccessToken(req.getTenantId(),username);
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", newAccess)
                .httpOnly(true)
                .secure(false) // 本地调试 false; 生产 true
                .sameSite("Lax")
                .path("/")
                .maxAge(15 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // ✅ 新 Refresh（Rotation）
        String newRefresh = jwtUtil.generateRefreshToken(req.getTenantId(),username);
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", newRefresh)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(Map.of("message", "Refreshed"));
    }

    private void clearAllAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
        response.addHeader("Set-Cookie", "REFRESH=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth/refresh; Max-Age=0; HttpOnly; Secure; SameSite=None");
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
    }
}
