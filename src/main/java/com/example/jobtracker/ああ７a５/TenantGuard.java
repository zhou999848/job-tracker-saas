package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;

import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TenantGuard {
    private final TenantRepository tenantRepository;

    public TenantGuard(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public static UUID currentTenantId() { return TenantContext.requireTenantIdFromRequest(); }

    public static void requireSameTenant(UUID tenantId) {
        UUID cur = currentTenantId();
        if (!cur.equals(tenantId)) {
            throw new AccessDeniedException("跨租户访问被拒绝");
        }
    }

    /** 可选：写保护（暂停态禁止写） */
    public void requireWritableTenant(UUID tenantId) {
        requireSameTenant(tenantId);
        Tenant t = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));
        if (t.getStatus() == TenantStatus.SUSPENDED) {
            throw new AccessDeniedException("租户已被暂停，禁止写操作");
        }
    }
}
