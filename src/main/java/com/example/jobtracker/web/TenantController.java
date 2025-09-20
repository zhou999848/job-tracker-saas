package com.example.jobtracker.web;


import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.service.TenantService;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    /**
     * 仅系统管理员可创建租户
     */
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping
    public Tenant create(@RequestParam String name) {
        return service.createTenant(name);
    }

    /**
     * 仅系统管理员可查看所有租户
     */
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping
    public List<Tenant> list() {
        return service.listAll();
    }

    /**
     * 任意已认证用户：查看自己所属租户（便于前端显示当前租户名）
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public Tenant myTenant() {
        UUID tenantId = TenantContext.requireTenantIdFromRequest();
        return service.getTenant(tenantId);
    }
    /**
     * 租户管理员：邀请本租户成员
     */
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @PostMapping("/{tenantId}/invite")
    public void inviteMember(@PathVariable UUID tenantId, @RequestParam String username) {
        service.inviteMember(tenantId, username);
    }

    /**
     * 租户管理员：移除本租户成员
     */
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @DeleteMapping("/{tenantId}/members/{userId}")
    public void removeMember(@PathVariable UUID tenantId, @PathVariable UUID userId) {
        service.removeMember(tenantId, userId);
    }
}

