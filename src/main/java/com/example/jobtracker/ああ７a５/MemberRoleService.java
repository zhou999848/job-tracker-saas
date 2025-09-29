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
    public MemberRoleService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void changeRole(UUID tenantId, UUID userId, Role newRole) {//TanantController调用
        TenantGuard.requireSameTenant(tenantId);
        User user = users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        User target = users.findByTenantIdAndUsername(tenantId, user.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found in tenant"));
        Role old = target.getRole();
        if (old == newRole) return; // 幂等

        // 保护：不得把“最后一个管理员”降级
        if (old == Role.TENANT_ADMIN && newRole == Role.USER) {
            long admins = users.countByTenantIdAndRole(tenantId, Role.TENANT_ADMIN);
            if (admins <= 1) throw new IllegalStateException("禁止降级最后一个管理员");
        }

        // 可选保护：禁止管理员把自己降为 USER（避免把租户玩坏）
        if (target.getId().equals(SecurityUtils.currentUserId()) && newRole == Role.USER) {
            throw new IllegalStateException("禁止将自己降级为普通用户");
        }

        target.setRole(newRole);
    }
}

