
package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.security.LoginUser; // ← 若包名不同请改
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

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

    /** 若取不到则抛异常（多数业务场景用这个） */
    public UUID requireTenantId() {
        return optionalTenantId()
                .orElseThrow(() -> new IllegalStateException("Tenant id not found in authentication"));
    }

    /** 可选获取（某些非强依赖场景可用） */
    public Optional<UUID> optionalTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();

        // ① 自定义 Principal：LoginUser（方式1，推荐）
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser lu && lu.tenantId() != null) {
            return Optional.of(lu.tenantId());
        }

        // ② Resource Server 模式：JWT 的 claim
        if (auth instanceof JwtAuthenticationToken jat) {
            Object claim = jat.getToken().getClaim("tenant");
            if (claim != null) {
                try {
                    return Optional.of(UUID.fromString(claim.toString()));
                } catch (IllegalArgumentException ignore) { /* fallthrough */ }
            }
        }

        // ③ Authentication details 中的 Map（若你把 tenantId 放在 details 里）
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object tid = map.get("tenantId");
            if (tid != null) {
                try {
                    return Optional.of(UUID.fromString(tid.toString()));
                } catch (IllegalArgumentException ignore) { /* fallthrough */ }
            }
        }

        // ④ 兜底：从 MDC 读取（如果你的过滤器里 put 过）
        String mdcTid = MDC.get("tenant");
        if (mdcTid != null && !mdcTid.isBlank()) {
            try {
                return Optional.of(UUID.fromString(mdcTid));
            } catch (IllegalArgumentException ignore) { /* fallthrough */ }
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

        // 自定义 Principal（方式1，推荐）
        if (p instanceof LoginUser lu && lu.username() != null && !lu.username().isBlank()) {
            return lu.username();
        }
        // Spring Security 的 UserDetails
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }
        // 通用 Principal
        if (p instanceof Principal pr) {
            return pr.getName();
        }
        // 兜底
        return auth.getName();
    }
}



