package com.example.jobtracker.ああ７a５;


import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.TenantDto;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;


@Service
public class SystemTenantService {
    private final TenantRepository tenants;
    private final UserRepository users;

    public SystemTenantService(TenantRepository tenants, UserRepository users) {
        this.tenants = tenants;
        this.users = users;
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<TenantSummaryDto> listTenants(String q, Pageable pageable) {
        Page<Tenant> page = (q == null || q.isBlank())
                ? tenants.findAll(pageable)
                : tenants.findAllByName(q, pageable);
        return page.map(t -> new TenantSummaryDto(
                t.getId(),
                t.getName(),
                t.getStatus(),
                users.countByTenant_Id(t.getId()),
                t.getOtherField()
        ));
    }

    @Transactional
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void suspend(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));
        t.setStatus(TenantStatus.SUSPENDED);
    }

    @Transactional
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void resume(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));
        t.setStatus(TenantStatus.ACTIVE);
    }
}
