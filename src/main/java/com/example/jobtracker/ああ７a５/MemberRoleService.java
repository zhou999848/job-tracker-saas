package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.TenantInvite7a4.Role;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.service.CurrentTenant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MemberRoleService {

    private final UserRepository users;
    private final SecurityUtils securityUtils;
    private final CurrentTenant currentTenant;

    public MemberRoleService(UserRepository users, SecurityUtils securityUtils , CurrentTenant currentTenant) {
        this.currentTenant = currentTenant;
        this.users = users;
        this.securityUtils = securityUtils;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void changeRole(UUID tenantId, UUID userId, Role newRole) {

        boolean callerIsSysAdmin = currentTenant.isSystemAdmin();

        // ① SYSTEM_ADMIN 允许跨租户；普通管理员只能管理自己租户
        if (!callerIsSysAdmin) {
            TenantGuard.requireSameTenant(tenantId);
        }

        User target = users.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("User not found in tenant"));

        Role old = target.getRole();
        if (old == newRole) return;

        // ② 普通管理员不能修改 SYSTEM_ADMIN
        if (!callerIsSysAdmin && old == Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("无权限修改系统管理员角色");
        }

        // ③ 普通管理员不能创建 SYSTEM_ADMIN
        if (!callerIsSysAdmin && newRole == Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("无权限创建系统管理员");
        }

        // ④ 业务保护逻辑
        if (old == Role.TENANT_ADMIN && newRole == Role.USER) {
            long admins = users.countByTenant_IdAndRole(tenantId, Role.TENANT_ADMIN);
            if (admins <= 1) throw new IllegalStateException("禁止降级最后一个管理员");
        }

        if (target.getId().equals(securityUtils.currentUserId()) && newRole == Role.USER) {
            throw new IllegalStateException("禁止将自己降级为普通用户");
        }

        // ⑤ SYSTEM_ADMIN 不需要脱离租户，因为它固定属于 system-tenant
        target.setRole(newRole);
    }


}





