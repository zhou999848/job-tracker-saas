package com.example.jobtracker.security;


import com.example.jobtracker.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
public class AuthPageController {
    private final JwtUtil jwtUtil;

    public AuthPageController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/silent-refresh")
    public void silentRefresh(@RequestParam(required = false) String redirect,
                              HttpServletRequest req,
                              HttpServletResponse res,
                              LoginRequest reqs) throws IOException {

        String refresh = readCookie(req, "REFRESH");

        // ⛔ 失败：无票据 / 无效 / iat <= passwordChangedAt
        if (refresh == null || !jwtUtil.validateRefreshTokenStrict(refresh)) {
            clearAllAuthCookies(res);
            SecurityContextHolder.clearContext();

            String to = (redirect != null && !redirect.isBlank()) ? redirect : "/";
            res.sendRedirect("/login?redirect=" +
                    URLEncoder.encode(to, StandardCharsets.UTF_8));
            return;
        }

        // ✅ 成功：签发新的 Access（推荐也 Rotate Refresh）
        String username = jwtUtil.getUsername(refresh);
        String newAccess = jwtUtil.generateAccessToken(reqs.getTenantId(),username);
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", newAccess)
                .httpOnly(true)
                .secure(false) // 本地调试 false; 生产 true
                .sameSite("Lax")
                .path("/")
                .maxAge(15 * 60) // 15 分钟
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 可选：刷新 Refresh Token（Rotation）
        String newRefresh = jwtUtil.generateRefreshToken(reqs.getTenantId(),username);
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", newRefresh)
                .httpOnly(true)
                .secure(false) // 本地调试 false; 生产 true
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 3600) // 7 天
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String to = (redirect != null && !redirect.isBlank()) ? redirect : "/";
        res.sendRedirect(to);
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (var c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
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

