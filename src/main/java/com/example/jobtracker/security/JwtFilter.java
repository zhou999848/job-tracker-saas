package com.example.jobtracker.security;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jboss.logging.MDC;
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
    private final TenantRepository tenantRepo;

    public JwtFilter(JwtUtil jwtUtil,
                     MyUserDetailsService userDetailsService,
                     UserRepository userRepository,TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
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

        UUID ctxTenantId = null; // 只在 finally 用于判断是否需要清理 ThreadLocal

        try {
            // 仅当当前还没有认证时才尝试用 JWT 建立认证
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = resolveAccessToken(request);
                if (token != null) {
                    if (!jwtUtil.validateAccessTokenStrict(token)) {
                        // token 非法/过期：清理并保持未认证状态
                        clearAccessCookie(response, request.isSecure());
                        clearRefreshCookie(response, request.isSecure());
                        invalidateSessionIfPresent(request);
                        SecurityContextHolder.clearContext();
                    } else {
                        final String username = jwtUtil.getUsername(token);
                        final UUID tenantId = jwtUtil.getTenantId(token);
                        final Instant tokenIat = jwtUtil.getIssuedAt(token);

                        if (!StringUtils.hasText(username) || tenantId == null) {
                            // 缺关键字段，按无效处理
                            clearAccessCookie(response, request.isSecure());
                            clearRefreshCookie(response, request.isSecure());
                            invalidateSessionIfPresent(request);
                            SecurityContextHolder.clearContext();
                        } else {
                            // 校验用户是否存在于该租户
                            Optional<User> optUser = userRepository.findByTenantIdAndUsername(tenantId, username);
                            if (optUser.isEmpty()) {
                                clearAccessCookie(response, request.isSecure());
                                clearRefreshCookie(response, request.isSecure());
                                invalidateSessionIfPresent(request);
                                SecurityContextHolder.clearContext();
                            } else {
                                User user = optUser.get();

                                // 如果用户修改过密码且时间晚于 token 的 iat，则判定 token 失效
                                Instant pwdAt = user.getPasswordChangedAt();
                                boolean invalidByPwdChange =
                                        (pwdAt != null) && (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                                if (invalidByPwdChange) {
                                    clearAccessCookie(response, request.isSecure());
                                    clearRefreshCookie(response, request.isSecure());
                                    invalidateSessionIfPresent(request);
                                    SecurityContextHolder.clearContext();
                                } else {
                                    // ★★★ 单一真相：在进入控制器前把 tenantId 放入 ThreadLocal + MDC
                                    TenantContext.set(tenantId);
                                    MDC.put("tenantId", tenantId.toString());
                                    request.setAttribute("tenantId", tenantId);

                                    // 再加载用户详情（若 UserDetailsService 依赖 TenantContext）
                                    var userDetails = userDetailsService.loadUserByUsername(username);

                                    // 使用 UserDetails 作为 principal，保证 getName() == username
                                    var auth = new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                                    // 将包含 userId/username/tenantId 的 LoginUser 放在 details，便于业务层扩展读取
                                    var principal = new LoginUser(user.getId(), user.getUsername(), tenantId);
                                    // 追加在 details（不覆盖 Spring 的基础 details，可合并或封装为自定义 details 对象）
                                    // 这里简单起见，直接覆盖为自定义对象：
                                    auth.setDetails(principal);

                                    SecurityContextHolder.getContext().setAuthentication(auth);

                                    log.info("[AUTH] user={}, tenant={}, authorities={}",
                                            username, tenantId, userDetails.getAuthorities());
                                }
                            }
                        }
                    }
                }
            }

            // 放行后续过滤器/控制器
            chain.doFilter(request, response);

        } catch (Exception e) {
            // 认证阶段异常：降级并交由全局异常处理
            log.error("[JWT] unexpected auth error", e);
            clearAccessCookie(response, request.isSecure());
            clearRefreshCookie(response, request.isSecure());
            invalidateSessionIfPresent(request);
            SecurityContextHolder.clearContext();
            throw e;
        } finally {
            // ★★★ 仅当本过滤器实际设置过 ThreadLocal 时才清理，避免误清理其他地方设置的上下文
            if (ctxTenantId != null) {
                TenantContext.clear();
            }
            // SecurityContextHolder 的清理交给 Spring Security 链路末尾
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
