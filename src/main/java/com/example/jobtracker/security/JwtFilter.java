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
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final Duration IAT_SKEW = Duration.ofSeconds(2);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final MyUserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil,
                     MyUserDetailsService userDetailsService,
                     UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login")
                || path.equals("/logout")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/users/login")
                || path.equals("/api/users/register")
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

        UUID ctxTenantId = null;

        try {
            // 仅当当前还未认证时才尝试基于 JWT 认证
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = resolveAccessToken(request);
                if (token != null) {
                    // 1) 严格校验 Access Token（签名/过期/时钟偏移等）
                    if (!jwtUtil.validateAccessTokenStrict(token)) {
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                    } else {
                        // 2) 解析载荷
                        String username = jwtUtil.getUsername(token);
                        UUID tenantId = jwtUtil.getTenantId(token);
                        Instant tokenIat = jwtUtil.getIssuedAt(token);

                        // 3) 用 tenantId + username 查实体（拿到 id + pwdChangedAt）
                        Optional<User> optUser = userRepository.findByTenantIdAndUsername(tenantId, username);

                        if (optUser.isEmpty()) {
                            // token 有效但用户已不存在/跨租户不匹配
                            clearAccessCookie(response, request.isSecure());
                            clearRefreshCookie(response, request.isSecure());
                            invalidateSessionIfPresent(request);
                            SecurityContextHolder.clearContext();
                        } else {
                            User user = optUser.get();

                            // 4) 密码变更失效（token iat 必须晚于 pwdChangedAt + 容忍偏移）
                            Instant pwdAt = user.getPasswordChangedAt();
                            boolean invalidByPwdChange =
                                    (pwdAt != null) && (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                            if (invalidByPwdChange) {
                                clearAccessCookie(response, request.isSecure());
                                clearRefreshCookie(response, request.isSecure());
                                invalidateSessionIfPresent(request);
                                SecurityContextHolder.clearContext();
                            } else {
                                // 5) 设置租户上下文（ThreadLocal + Request Attribute）
                                ctxTenantId = tenantId;
                                com.example.jobtracker.tenant.TenantContext.set(tenantId);
                                request.setAttribute(com.example.jobtracker.tenant.TenantContext.REQUEST_ATTR, tenantId);

                                // 6) 加载权限（可能依赖 TenantContext）
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                // 7) 自定义 Principal，带上 userId/username/tenantId
                                LoginUser principal = new LoginUser(
                                        user.getId(),          // ★ 关键：不再是 null
                                        user.getUsername(),
                                        tenantId
                                );

                                org.slf4j.LoggerFactory.getLogger(JwtFilter.class).info(
                                        "[AUTH] user={}, tenant={}, authorities={}",
                                        username, tenantId, userDetails.getAuthorities()
                                );

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                principal, null, userDetails.getAuthorities());
                                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 认证阶段任何异常都做降级：清上下文与 Cookie，放行给后续异常处理
            log.error("[JWT] unexpected auth error", e);
            clearAccessCookie(response, request.isSecure());
            clearRefreshCookie(response, request.isSecure());
            invalidateSessionIfPresent(request);
            SecurityContextHolder.clearContext();
        } finally {
            // 只在本过滤器设置过租户时才清理，避免提前清导致后续用不到
            if (ctxTenantId != null) {
                com.example.jobtracker.tenant.TenantContext.clear();
            }
        }

        // 始终只调用一次
        chain.doFilter(request, response);
    }

    private void clearAccessCookie(HttpServletResponse response, boolean secure) {
        StringBuilder sb = new StringBuilder("ACCESS=; Path=/; Max-Age=0; HttpOnly; ");
        if (secure) sb.append("Secure; SameSite=Lax");
        else sb.append("SameSite=Lax");
        response.addHeader("Set-Cookie", sb.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response, boolean secure) {
        StringBuilder sb = new StringBuilder("REFRESH=; Path=/; Max-Age=0; HttpOnly; ");
        if (secure) sb.append("Secure; SameSite=Lax");
        else sb.append("SameSite=Lax");
        response.addHeader("Set-Cookie", sb.toString());
    }

    private void invalidateSessionIfPresent(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) s.invalidate();
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim().replace("\"", "");
        }
        return readCookie(request, "ACCESS");
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v.replace("\"", "");
            }
        }
        return null;
    }
}
