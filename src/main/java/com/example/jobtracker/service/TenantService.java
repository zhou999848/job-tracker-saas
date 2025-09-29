package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.TenantDto;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
import com.example.jobtracker.ああ７a５.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.Role;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.example.jobtracker.TenantInvite7a4.Role.USER;

@Service
public class TenantService {
    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepo,
                         UserRepository userRepo,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID createTenant(String name) {
        if (tenantRepo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists");
        }
        Tenant t = new Tenant();
        t.setId(UUID.randomUUID());
        t.setName(name);
        t.setStatus(TenantStatus.ACTIVE);
        tenantRepo.save(t);
        return t.getId();
    }

    // ✅ 新的方法：分页 + 搜索
    public Page<Map<String,Object>> list(String q, Pageable pageable) {
        return tenantRepo.findTenantAdminPage(q, pageable);
    }

    // ✅ 保留你现有的方法（不变）
    public Tenant getTenant(java.util.UUID tenantId) {
        return tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    }
}





