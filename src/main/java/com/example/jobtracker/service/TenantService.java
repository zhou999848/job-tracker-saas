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
    /** ---------- 本题：租户管理员成员管理 ---------- */

    /**
     * 租户管理员邀请成员：
     * 规则：
     *  - 必须是 TENANT_ADMIN（或 SYSTEM_ADMIN）
     *  - 非系统管理员只能操作自己租户
     *  - 若用户不存在：创建基础账号（role=USER，临时密码）并绑定到本租户
     *  - 若用户存在但已属于其他租户：拒绝
     *  - 若用户存在且未绑定租户：直接绑定到本租户
     */

    private void assertTenantScope(UUID tenantId) throws AccessDeniedException {
        UUID current = TenantContext.requireTenantIdFromRequest();
        boolean isSystemAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        if (!isSystemAdmin && !current.equals(tenantId)) {
            throw new AccessDeniedException("禁止跨租户操作");
        }
    }
    @Transactional
    public void inviteMember(UUID tenantId, String username) {
        try {
            assertTenantScope(tenantId);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));

        User user = userRepo.findByTenantIdAndUsername(tenantId,username)
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername(username);
                    u.setPassword(passwordEncoder.encode("Init@123")); // 临时密码
                    u.setRole("USER");
                    return u;
                });

        user.setTenant(tenant);
        userRepo.save(user);
    }

    @Transactional
    public void removeMember(UUID tenantId, UUID userId) {
        try {
            assertTenantScope(tenantId);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (user.getTenant() == null || !tenantId.equals(user.getTenant().getId())) {
            throw new IllegalArgumentException("用户不属于该租户");
        }

        user.setTenant(null); // 解除绑定
        userRepo.save(user);
    }
}



