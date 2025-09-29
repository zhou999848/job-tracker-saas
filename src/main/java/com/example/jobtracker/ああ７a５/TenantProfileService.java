package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
public class TenantProfileService {
    private final TenantRepository tenants;
    public TenantProfileService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void rename(UUID tenantId, String newName) {//TenantController调用
        TenantGuard.requireSameTenant(tenantId);
        if (newName == null || newName.isBlank()) throw new IllegalArgumentException("名称不能为空");
        Tenant t = tenants.findById(tenantId).orElseThrow();
        t.setName(newName.trim());
    }
}
