package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.service.CurrentTenant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TenantProfileService {
    private final TenantRepository tenants;
    private final CurrentTenant currentTenant;

    public TenantProfileService(TenantRepository tenants, CurrentTenant currentTenant) {
        this.currentTenant = currentTenant;
        this.tenants = tenants;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void rename(UUID tenantId, String newName) {

        // ---- 系统管理员允许跨租户修改 ----
        if (!currentTenant.isSystemAdmin()) {
            TenantGuard.requireSameTenant(tenantId);
        }

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }

        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));

        t.setName(newName.trim());
    }

}
