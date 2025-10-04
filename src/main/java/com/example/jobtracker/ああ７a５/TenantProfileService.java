package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TenantProfileService {
    private final TenantRepository tenants;

    public TenantProfileService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void rename(UUID tenantId, String newName) {
        TenantGuard.requireSameTenant(tenantId); // ★ 校验同租户
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));
        t.setName(newName.trim());
    }
}
