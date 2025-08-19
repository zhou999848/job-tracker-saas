package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.jobtracker.security.MyUserDetailsService;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.JwtUtil;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final Duration IAT_SKEW = Duration.ofSeconds(2); // iat vs DB 时间精度容差

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;                     // JWT 工具类 / Utility for JWT parsing
    private final MyUserDetailsService userDetailsService; // 自定义用户信息加载服务 / Custom UserDetailsService

    public JwtFilter(JwtUtil jwtUtil,
                     MyUserDetailsService userDetailsService,
                     UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 指定哪些请求不需要进入 JWT 检查
     * Specify public endpoints to skip JWT filtering
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login")
                || path.equals("/logout")
                || path.equals("/api/users/login")
                || path.equals("/api/users/register")
                // ↓ 静态资源放行
                || path.startsWith("/css/")
                || path.equals("/style.css")
                || path.startsWith("/images/")
                || path.startsWith("/js/")
                || path.equals("/favicon.ico");
        // ⚠️ 删除了 path.equals("application/pdf")：那不是 URL 路径，会误绕过过滤器
    }

    /**
     * 核心过滤逻辑 / Main filter logic
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 只有在当前还未认证时才尝试用 JWT 认证（避免覆盖已有状态）
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request); // 先 Cookie("JWT")，再 Authorization: Bearer
            //aaaaa
            if (token != null && jwtUtil.validateSignature(token)) {
                try {
                    String username = jwtUtil.getUsernameFromToken(token);
                    Instant tokenIat = jwtUtil.getIssuedAt(token); // 从 claims.getIssuedAt()
                    if (log.isDebugEnabled()) {
                        log.debug("JWT found. uri={}, user={}, iat={}", request.getRequestURI(), username, tokenIat);
                    }
                    //aaaaa
                    // DB 取密码修改时间（只查这一列最省）
                    Instant pwdAt = userRepository.findByUsername(username)
                            .map(User::getPasswordChangedAt)
                            .orElse(null);

                    // 判旧：token 的 iat ≤ passwordChangedAt(+容差) → 旧 token
                    boolean invalidByPwd = (pwdAt != null) &&

                            (tokenIat == null || !tokenIat.isAfter(pwdAt.plus(IAT_SKEW)));

                    if (invalidByPwd) {
                        if (log.isDebugEnabled()) {
                            log.debug("Reject JWT due to password change. uri={}, iat={}, passwordChangedAt={}",
                                    request.getRequestURI(), tokenIat, pwdAt);
                        }
                        // 关键：清掉浏览器里的 JWT，避免后续每次都带旧 token
                        expireJwtCookie(response);
                        // 关键：早返回（继续链，但不设置认证；避免后续任何地方“复活”）
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 通过校验后才创建认证信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    if (log.isDebugEnabled()) {
                        log.debug("JWT accepted. uri={}, user={}, iat={}, passwordChangedAt={}",
                                request.getRequestURI(), username, tokenIat, pwdAt);
                    }

                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    // Token 过期 / Token expired
                    log.info("[JWT] Token expired: {}", e.getMessage());
                    // 过期也清一次 Cookie，避免浏览器反复带过期 token
                    expireJwtCookie(response);
                } catch (io.jsonwebtoken.security.SignatureException e) {
                    // 签名不匹配 / Invalid signature
                    log.warn("[JWT] Invalid signature: {}", e.getMessage());
                } catch (io.jsonwebtoken.JwtException e) {
                    // 其他 JWT 解析错误 / Other JWT errors
                    log.warn("[JWT] Invalid token: {}", e.getMessage());
                } catch (Exception e) {
                    // 未预料的错误 / Unexpected errors
                    log.error("[JWT] Unexpected error when authenticating", e);
                }
            }
        }

        // 放行请求，继续后续过滤器 / Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization Bearer 或 Cookie "JWT" 中解析 Token
     * Resolve token from Authorization Bearer or Cookie "JWT".
     */
    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization: Bearer
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1);
            }
            return t.isEmpty() ? null : t;
        }

        // 2) Cookie: JWT
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("JWT".equals(c.getName())) {
                    String t = c.getValue();
                    if (t != null) {
                        t = t.trim();
                        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                            t = t.substring(1, t.length() - 1);
                        }
                        return t.isEmpty() ? null : t;
                    }
                }
            }
        }
        return null;
    }

    /** 清空 JWT Cookie（当前浏览器端） */
    private void expireJwtCookie(HttpServletResponse response) {
        // 按你项目的 Cookie 属性调整 secure/sameSite
        String expired = "JWT=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        // 如果是 HTTPS，建议加上 "; Secure"
        response.addHeader("Set-Cookie", expired);
    }
}
