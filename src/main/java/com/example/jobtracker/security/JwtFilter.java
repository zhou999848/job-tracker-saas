package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
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
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = resolveAccessToken(request);
                if (token != null) {
                    if (!jwtUtil.validateAccessTokenStrict(token)) {
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                    } else {
                        String username = jwtUtil.getUsername(token);
                        UUID tenantId = jwtUtil.getTenantId(token);
                        Instant tokenIat = jwtUtil.getIssuedAt(token);

                        Optional<User> optUser = userRepository.findByTenantIdAndUsername(tenantId, username);
                        if (optUser.isEmpty()) {
                            clearAccessCookie(response, request.isSecure());
                            clearRefreshCookie(response, request.isSecure());
                            invalidateSessionIfPresent(request);
                            SecurityContextHolder.clearContext();
                        } else {
                            User user = optUser.get();

                            Instant pwdAt = user.getPasswordChangedAt();
                            boolean invalidByPwdChange =
                                    (pwdAt != null) && (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                            if (invalidByPwdChange) {
                                clearAccessCookie(response, request.isSecure());
                                clearRefreshCookie(response, request.isSecure());
                                invalidateSessionIfPresent(request);
                                SecurityContextHolder.clearContext();
                            } else {
                                // ★★★ 在进入控制器之前设置 ThreadLocal（单一真相）
                                ctxTenantId = tenantId;
                                com.example.jobtracker.tenant.TenantContext.set(tenantId);
                                // （可选）Request Attribute，仅作调试/兼容
                                request.setAttribute(com.example.jobtracker.tenant.TenantContext.REQUEST_ATTR, tenantId);

                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                LoginUser principal = new LoginUser(
                                        user.getId(),
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

            // ★★★ 先放行到后续过滤器/控制器
            chain.doFilter(request, response);

        } catch (Exception e) {
            // 认证阶段的异常：降级，仍然放行到后续异常处理链
            log.error("[JWT] unexpected auth error", e);
            clearAccessCookie(response, request.isSecure());
            clearRefreshCookie(response, request.isSecure());
            invalidateSessionIfPresent(request);
            SecurityContextHolder.clearContext();
            throw e; // 交给上层统一错误处理（或不抛，看你策略）
        } finally {
            // ★★★ 确保在请求结束后清理 ThreadLocal，避免线程复用污染
            if (ctxTenantId != null) {
                com.example.jobtracker.tenant.TenantContext.clear();
            }
            // 不必强制在这里清 SecurityContextHolder，Spring Security 会在链路末尾处理
        }
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
