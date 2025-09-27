package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.Role;
import java.nio.file.AccessDeniedException;
import java.util.List;
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

    public Tenant createTenant(String name) {
        if (tenantRepo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists");
        }
        Tenant t = new Tenant();
        t.setName(name);
        return tenantRepo.save(t);
    }

    public List<Tenant> listAll() {
        return tenantRepo.findAll();
    }
    public Tenant getTenant(java.util.UUID tenantId) {
        return tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    }

}



