
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
@Order(Ordered.HIGHEST_PRECEDENCE) // 保证在 Spring Security/JWT 之前
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final AntPathMatcher APM = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // 统一化多斜杠，避免 StrictHttpFirewall 因 "//" 拦截
        String uri = req.getRequestURI().replaceAll("/{2,}", "/");

        try {
            tryExtractFromPath(uri);

            // （可选兜底）从 Header 读
            if (TenantContext.get() == null) {
                String h = req.getHeader("X-Tenant-Id");
                if (h != null && !h.isBlank()) {
                    TenantContext.set(UUID.fromString(h.trim()));
                }
            }

            chain.doFilter(req, res);
        } finally {
            // 一定要清理，线程复用会串租户
            TenantContext.clear();
        }
    }

    private void tryExtractFromPath(String uri) {
        // 按实际路径增删 pattern；支持 /api/tenants/{id} 及其子路径
        String[] patterns = {
                "/api/tenants/{tenantId}",
                "/api/tenants/{tenantId}/**",
                "/api/tenants/{id}",
                "/api/tenants/{id}/**",
                // 如果你的应用部署在子上下文，考虑 "/**/api/tenants/{tenantId}/**"
        };
        for (String p : patterns) {
            if (APM.match(p, uri)) {
                Map<String,String> vars = APM.extractUriTemplateVariables(p, uri);
                String raw = vars.getOrDefault("tenantId", vars.get("id"));
                if (raw != null && !raw.isBlank()) {
                    TenantContext.set(UUID.fromString(raw));
                }
                return;
            }
        }
    }
}
