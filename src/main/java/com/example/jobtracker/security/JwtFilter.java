package com.example.jobtracker.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;


    /**
     * JWT 认证过滤器 / JWT Authentication Filter
     *
     * 作用：
     *  - 在每次 HTTP 请求前拦截并检查 JWT Token
     *  - 如果 Token 有效，则将认证信息放入 Spring Security 的上下文
     *  - 如果 Token 无效或缺失，则不设置认证信息（交由 Security 判断权限）
     *
     * This filter:
     *  - Intercepts every HTTP request
     *  - Checks for a valid JWT token in Authorization header or Cookie
     *  - If valid, sets authentication into Spring Security context
     *  - If invalid/missing, leaves authentication empty (Security will handle it)
     */
    @Component
    public class JwtFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

        private final JwtUtil jwtUtil;                     // JWT 工具类 / Utility for JWT parsing
        private final MyUserDetailsService userDetailsService; // 自定义用户信息加载服务 / Custom UserDetailsService

        public JwtFilter(JwtUtil jwtUtil, MyUserDetailsService userDetailsService) {
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
                    || path.equals("/logout")// 登录页面 / login page
                    || path.equals("/api/users/login")        // 登录 API / login API
                    || path.equals("/api/users/register")     // 注册 API / register API
                    || path.equals("application/pdf") // PDF 下载 / PDF download
                    || path.startsWith("/css/")               // 静态资源 / static resources
                    || path.equals("/style.css")
                    || path.startsWith("/images/")
                    || path.startsWith("/js/");
        }

        /**
         * 核心过滤逻辑 / Main filter logic
         */
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            // 1) 获取 Token（可能来自 Authorization 头，也可能来自 Cookie）
            // 1) Get token from Authorization header or Cookie
            String token = resolveToken(request);

            // 2) 如果 token 存在，并且当前 SecurityContext 没有认证信息，则进行解析
            // 2) If token exists and no authentication yet, try parsing
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // 从 Token 中解析出用户名 / Extract username from token
                    String username = jwtUtil.getUsernameFromToken(token); // 可能抛 JwtException / may throw JwtException

                    if (username != null) {
                        // 加载用户详细信息（角色、权限等）
                        // Load user details (roles, permissions, etc.)
                        var userDetails = userDetailsService.loadUserByUsername(username);

                        // 创建 Spring Security 的认证对象
                        // Create Spring Security authentication token
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        // 附加请求详情（IP、Session 等）
                        // Attach request details (IP, session, etc.)
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 将认证信息放入 SecurityContext，使后续请求可识别用户
                        // Set authentication into SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    // Token 过期 / Token expired
                    log.info("[JWT] Token expired: {}", e.getMessage());
                    // 可选：直接返回 401，也可继续由控制器处理
                    // Optional: set 401 or let controller handle
                    // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    // return;
                } catch (io.jsonwebtoken.security.SignatureException e) {
                    // 签名不匹配 / Invalid signature
                    log.warn("[JWT] Invalid signature: {}", e.getMessage());
                    // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    // return;
                } catch (io.jsonwebtoken.JwtException e) {
                    // 其他 JWT 解析错误 / Other JWT errors
                    log.warn("[JWT] Invalid token: {}", e.getMessage());
                } catch (Exception e) {
                    // 未预料的错误 / Unexpected errors
                    log.error("[JWT] Unexpected error when authenticating: {}", e.toString());
                }
            }

            // 3) 放行请求，继续后续过滤器 / Continue filter chain
            filterChain.doFilter(request, response);
        }

        /**
         * 从 Authorization Bearer 或 Cookie "JWT" 中解析 Token
         * Resolve token from Authorization Bearer or Cookie "JWT".
         */
        private String resolveToken(HttpServletRequest request) {
            // 1. 从 Authorization 头读取
            // 1. Read from Authorization header
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String t = header.substring(7).trim(); // 去掉 "Bearer " 前缀 / Remove "Bearer " prefix
                // 如果被引号包裹，去掉引号 / Strip quotes if wrapped
                if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                    t = t.substring(1, t.length() - 1);
                }
                return t.isEmpty() ? null : t;
            }

            // 2. 从 Cookie 中读取
            // 2. Read from Cookie
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("JWT".equals(c.getName())) { // 找到名为 JWT 的 Cookie / Find cookie named JWT
                        String t = c.getValue();
                        if (t != null) {
                            t = t.trim();
                            // 如果被引号包裹，去掉引号 / Strip quotes if wrapped
                            if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                                t = t.substring(1, t.length() - 1);
                            }
                            return t.isEmpty() ? null : t;
                        }
                    }
                }
            }

            return null; // 没找到 Token / No token found
        }
    }

