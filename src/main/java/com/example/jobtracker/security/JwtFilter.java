package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    /**
     * iat 与 DB 时间精度的容差（防止边界时刻误判）
     */
    private static final Duration IAT_SKEW = Duration.ofSeconds(2);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;                       // ✅ 你新版的 JwtUtil（无旧方法）
    private final MyUserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil,
                     MyUserDetailsService userDetailsService,
                     UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 哪些请求不进 JWT 过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login")                 // 表单页
                || path.equals("/logout")
                // 允许未登录访问的新认证接口
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                // （如果你的旧接口还在用，可以放行；否则可删除下面两行）
                || path.equals("/api/users/login")
                || path.equals("/api/users/register")
                // 静态资源
                || path.startsWith("/css/")
                || path.equals("/style.css")
                || path.startsWith("/images/")
                || path.startsWith("/js/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveAccessToken(request); // Bearer / Cookie: ACCESS
            if (token != null) {
                try {
                    // 1) 基础校验：过期/非法 → 当未登录处理（不抛401）
                    // 在 JwtFilter 中，尽早做严格校验
                    if (!jwtUtil.validateAccessTokenStrict(token)) {
                        if (log.isDebugEnabled()) log.debug("[JWT] invalid access (expired/signature/iat<=pwdAt)");
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure()); // ← 同时清除
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }
                  //  if (!jwtUtil.validateAccessTokenStrict(token)) {
                    //    clearAccessCookie(response, request.isSecure());
                      //  chain.doFilter(request, response);
                        //return;
                    //}

                    // 2) 解析用户名 + iat
                    String username = jwtUtil.getUsername(token);
                    Instant tokenIat = jwtUtil.getIssuedAt(token);
                    if (log.isDebugEnabled()) {
                        log.debug("[JWT] token accepted preliminarily. uri={}, user={}, iat={}",
                                request.getRequestURI(), username, tokenIat);
                    }

                    // 3) passwordChangedAt 失效策略：若 token.iat <= pwdChangedAt(+skew) 则判旧
                    Instant pwdAt = userRepository.findByUsername(username)
                            .map(User::getPasswordChangedAt)
                            .orElse(null);

                    boolean invalidByPwdChange = (pwdAt != null)
                            && (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                    if (invalidByPwdChange) {
                        if (log.isDebugEnabled()) {
                            log.debug("[JWT] reject due to password change. uri={}, iat={}, pwdChangedAt={}",
                                    request.getRequestURI(), tokenIat, pwdAt);
                        }
                        // ✅ 关键改动：密码已变更 → 彻底清理，防止静默刷新再次登录
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());        // ← 新增：同时清除 REFRESH
                        invalidateSessionIfPresent(request);                    // ← 新增：失效 HttpSession
                        SecurityContextHolder.clearContext();                   // ← 新增：清空上下文

                        chain.doFilter(request, response); // 后续由 EntryPoint/Controller 决定 401 或 302
                        return;
                    }

                    // 4) 构建认证并放进 SecurityContext
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    if (log.isDebugEnabled()) {
                        log.debug("[JWT] authenticated. uri={}, user={}", request.getRequestURI(), username);
                    }

                } catch (io.jsonwebtoken.JwtException e) {
                    log.warn("[JWT] invalid token: {}", e.getMessage());
                    clearAccessCookie(response, request.isSecure());
                } catch (Exception e) {
                    log.error("[JWT] unexpected auth error", e);
                }
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 只清 ACCESS（保留自动刷新场景）
     */
    private void clearAccessCookie(HttpServletResponse response, boolean secure) {
        StringBuilder sb = new StringBuilder("ACCESS=; Path=/; Max-Age=0; HttpOnly; ");
        if (secure) sb.append("Secure; SameSite=Lax");
        else sb.append("SameSite=Lax");
        response.addHeader("Set-Cookie", sb.toString());
    }

    /**
     * 🔥 新增：清 REFRESH（用于“密码已变更”场景，一次性踢下线）
     */
    private void clearRefreshCookie(HttpServletResponse response, boolean secure) {
        StringBuilder sb = new StringBuilder("REFRESH=; Path=/; Max-Age=0; HttpOnly; ");
        if (secure) sb.append("Secure; SameSite=Lax");
        else sb.append("SameSite=Lax");
        response.addHeader("Set-Cookie", sb.toString());
    }

    /**
     * 🔥 新增：失效 Session（若存在）
     */
    private void invalidateSessionIfPresent(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) s.invalidate();
    }
    /** 从 Authorization: Bearer 或 Cookie: ACCESS 解析访问令牌 */
    private String resolveAccessToken(HttpServletRequest request) {
        // 1) Authorization: Bearer <token>
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1);
            }
            if (!t.isEmpty()) return t;
        }
        // 2) Cookie: ACCESS
        return readCookie(request, "ACCESS");
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                String v = c.getValue();
                if (v != null) {
                    v = v.trim();
                    if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    }
                    return v.isEmpty() ? null : v;
                }
            }
        }
        return null;
    }

}
