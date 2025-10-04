package com.example.jobtracker.security;


import com.example.jobtracker.dto.LoginRequest;
import com.example.jobtracker.dto.UserDto;
import io.jsonwebtoken.JwtException;
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
import java.util.UUID;

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


            // 1) 解析 Refresh，拿 username & tenantId（从 token claims）
            final String username = jwtUtil.getUsername(refresh);
            final UUID tenantId = jwtUtil.getTenantId(refresh); // ✅ 从 Refresh 的 claims 里拿

            // 2) 生成新的 Access（建议 15 min）
            final String newAccess = jwtUtil.generateAccessToken(tenantId, username);

            ResponseCookie accessCookie = ResponseCookie.from("ACCESS", newAccess)
                    .httpOnly(true)
                    .secure(false)        // 本地调试 false；生产请改为 true
                    .sameSite("Lax")
                    .path("/")            // 前端所有路径都能带上
                    .maxAge(15 * 60)      // 15 分钟
                    .build();
            res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

            // 3) 可选：Rotate Refresh（建议）
            final String newRefresh = jwtUtil.generateRefreshToken(tenantId, username);

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", newRefresh)
                    .httpOnly(true)
                    .secure(false)        // 生产 true
                    .sameSite("Lax")
                    .path("/api/auth/refresh") // 仅在刷新接口请求时携带
                    .maxAge(7 * 24 * 3600)     // 7 天
                    .build();
            res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 4) 重定向（可选）
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

