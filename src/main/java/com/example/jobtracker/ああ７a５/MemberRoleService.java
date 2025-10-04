package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.TenantInvite7a4.Role;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MemberRoleService {

    private final UserRepository users;
    private final SecurityUtils securityUtils;

    public MemberRoleService(UserRepository users, SecurityUtils securityUtils) {
        this.users = users;
        this.securityUtils = securityUtils;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void changeRole(UUID tenantId, UUID userId, Role newRole) {
        TenantGuard.requireSameTenant(tenantId);

        User target = users.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("User not found in tenant"));

        Role old = target.getRole();
        if (old == newRole) return;

        if (old == Role.TENANT_ADMIN && newRole == Role.USER) {
            long admins = users.countByTenant_IdAndRole(tenantId, Role.TENANT_ADMIN);
            if (admins <= 1) throw new IllegalStateException("禁止降级最后一个管理员");
        }

        if (target.getId().equals(securityUtils.currentUserId()) && newRole == Role.USER) {
            throw new IllegalStateException("禁止将自己降级为普通用户");
        }

        target.setRole(newRole);
        // JPA 脏检查会提交，无需显式 save
    }
}





