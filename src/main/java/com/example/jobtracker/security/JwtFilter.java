package com.example.jobtracker.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final MyUserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, MyUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // ✅ 可选：跳过不需要鉴权的请求 / Skip public endpoints
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login")
                || path.equals("/api/users/login")
                || path.equals("/api/users/register")
                || path.startsWith("/css/")
                || path.equals("/style.css")
                || path.startsWith("/images/")
                || path.startsWith("/js/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1) 统一解析 Token / Resolve token from Authorization or Cookie
        String token = resolveToken(request);

        // 2) 仅当未认证、且拿到了 token 时才尝试解析
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtil.getUsernameFromToken(token); // 可能抛 JwtException

                if (username != null) {
                    var userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // 过期 / expired
                log.info("[JWT] Token expired: {}", e.getMessage());
                // 这里可选：直接给 401；也可以放过由控制器返回未登录页
                // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                // return;
            } catch (io.jsonwebtoken.security.SignatureException e) {
                // 签名不匹配 / signature invalid
                log.warn("[JWT] Invalid signature: {}",
                        e.getMessage());
                // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                // return;
            } catch (io.jsonwebtoken.JwtException e) {
                // 其他 JWT 异常 / other JWT errors
                log.warn("[JWT] Invalid token: {}", e.getMessage());
            } catch (Exception e) {
                log.error("[JWT] Unexpected error when authenticating: {}", e.toString());
            }
        }

        // 3) 继续后续 Filter 链 / Continue chain
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization Bearer 或名为 JWT 的 Cookie 获取 token。
     * Get token from Authorization Bearer or Cookie "JWT".
     */
    private String resolveToken(HttpServletRequest request) {
        // Authorization: Bearer xxx
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            // 去掉可能包起来的引号 / strip surrounding quotes
            if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1);
            }
            return t.isEmpty() ? null : t;
        }

        // Cookie: JWT=xxx
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
}
