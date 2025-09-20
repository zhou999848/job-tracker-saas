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

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveAccessToken(request);
            if (token != null) {
                try {
                    if (!jwtUtil.validateAccessTokenStrict(token)) {
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }

                    String username = jwtUtil.getUsername(token);
                    UUID tenantId = jwtUtil.getTenantId(token);
                    Instant tokenIat = jwtUtil.getIssuedAt(token);

                    // 密码变更校验
                    Instant pwdAt = userRepository.findByTenantIdAndUsername( tenantId,username)
                            .map(User::getPasswordChangedAt)
                            .orElse(null);

                    boolean invalidByPwdChange = (pwdAt != null)
                            && (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                    if (invalidByPwdChange) {
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }

                    // ★ 构建认证
                    com.example.jobtracker.tenant.TenantContext.set(tenantId); // 放进上下文
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

// ★ 自定义 Principal，带 tenantId
                    LoginUser principal = new LoginUser(
                            // 这里可以放 user 的 id，如果你有的话；没有就 null
                            null,
                            username,
                            tenantId
                    );

                    org.slf4j.LoggerFactory.getLogger(JwtFilter.class)
                            .info("[AUTH] user={}, tenant={}, authorities={}",
                                    username, tenantId, userDetails.getAuthorities());

// Authentication 里用自定义 principal
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);


                } catch (Exception e) {
                    log.error("[JWT] unexpected auth error", e);
                    clearAccessCookie(response, request.isSecure());
                } finally {
                    com.example.jobtracker.tenant.TenantContext.clear();
                }
            }
        }
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
