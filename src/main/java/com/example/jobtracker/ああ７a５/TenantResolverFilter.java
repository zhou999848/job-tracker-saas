// src/main/java/com/example/jobtracker/ああ７a５/TenantResolverFilter.java
package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保在 Spring Security/JWT 之前执行
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final AntPathMatcher APM = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // 统一多斜杠，避免 StrictHttpFirewall 因 "//" 拦截
        String uri = req.getRequestURI().replaceAll("/{2,}", "/");

        try {
            // 1) 先尝试从路径 /api/tenants/{tenantId}/** 提取
            tryExtractTenantIdFromPath(uri);

            // 2) 再尝试从 Header 读取（登录等无路径参数的场景）
            if (TenantContext.getId() == null) {
                String h = req.getHeader("X-Tenant-Id");
                if (h != null && !h.isBlank()) {
                    try {
                        TenantContext.set(UUID.fromString(h.trim()));
                    } catch (IllegalArgumentException e) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.setContentType("text/plain;charset=UTF-8");
                        res.getWriter().write("Invalid X-Tenant-Id");
                        return;
                    }
                }
            }

            chain.doFilter(req, res);
        } finally {
            // 一定要清理，线程复用会串租户
            TenantContext.clear();
        }
    }

    private void tryExtractTenantIdFromPath(String uri) {
        // 按实际路由增删 pattern；支持 /api/tenants/{tenantId} 及其子路径
        String[] patterns = {
                "/api/tenants/{tenantId}",
                "/api/tenants/{tenantId}/**",
                // 如果应用部署在子上下文，可加 "/**/api/tenants/{tenantId}/**"
        };
        for (String p : patterns) {
            if (APM.match(p, uri)) {
                Map<String, String> vars = APM.extractUriTemplateVariables(p, uri);
                String raw = vars.get("tenantId");
                if (raw != null && !raw.isBlank()) {
                    UUID tid = UUID.fromString(raw);
                    TenantContext.set(tid);
                }
                return;
            }
        }
    }
}

