
// src/main/java/com/example/jobtracker/service/CurrentTenant.java
package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.security.LoginUser;
import com.example.jobtracker.tenant.TenantContext;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CurrentTenant {

    private final TenantRepository tenantRepo;

    public CurrentTenant(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    /** 强依赖场景：必须存在 tenantId（来自 ThreadLocal 单一真相） */
    public UUID requireTenantId() {
        return TenantContext.requireTenantIdFromRequest();
    }

    /** 弱依赖场景：优先 ThreadLocal；否则再尝试历史兼容来源 */
    public Optional<UUID> optionalTenantId() {
        // ① ThreadLocal（推荐 & 单一真相）
        UUID fromCtx = TenantContext.getId();
        if (fromCtx != null) return Optional.of(fromCtx);

        // ② 兼容历史：尝试从 Authentication/JWT/details/MDC 读取（尽量早迁移移除）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();

        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser lu && lu.tenantId() != null) {
            return Optional.of(lu.tenantId());
        }

        if (auth instanceof JwtAuthenticationToken jat) {
            Object claim = jat.getToken().getClaim("tenantId"); // 统一采用 tenantId
            if (claim == null) claim = jat.getToken().getClaim("tenant"); // 兼容旧 key
            if (claim != null) {
                try { return Optional.of(UUID.fromString(claim.toString())); }
                catch (IllegalArgumentException ignore) {}
            }
        }

        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object tid = map.get("tenantId");
            if (tid != null) {
                try { return Optional.of(UUID.fromString(tid.toString())); }
                catch (IllegalArgumentException ignore) {}
            }
        }

        String mdcTid = MDC.get("tenantId"); // 统一 key
        if (!StringUtils.hasText(mdcTid)) mdcTid = MDC.get("tenant"); // 兼容旧 key
        if (StringUtils.hasText(mdcTid)) {
            try { return Optional.of(UUID.fromString(mdcTid)); }
            catch (IllegalArgumentException ignore) {}
        }

        return Optional.empty();
    }

    public Tenant requireTenant() {
        UUID tid = requireTenantId();
        return tenantRepo.findById(tid)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tid));
    }

    /** 优先 LoginUser → UserDetails → Principal → auth.getName() */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("No authentication");

        Object p = auth.getPrincipal();
        if (p instanceof LoginUser lu && StringUtils.hasText(lu.username())) {
            return lu.username();
        }
        if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        if (p instanceof Principal pr) {
            return pr.getName();
        }
        return auth.getName();
    }
}
