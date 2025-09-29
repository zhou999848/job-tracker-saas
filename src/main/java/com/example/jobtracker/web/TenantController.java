package com.example.jobtracker.web;


import com.example.jobtracker.TenantInvite7a4.Role;
import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.TenantDto;
import com.example.jobtracker.ああ７a５.MemberRoleService;
import com.example.jobtracker.service.TenantService;
import com.example.jobtracker.ああ７a５.RenameTenantRequest;
import com.example.jobtracker.ああ７a５.SystemTenantService;
import com.example.jobtracker.ああ７a５.TenantProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService service;

  private final SystemTenantService systemTenantService;
    private final TenantProfileService Pservice;
    private final MemberRoleService roleService;

    public TenantController(TenantProfileService Pservice, MemberRoleService roleService, TenantService service, SystemTenantService systemTenantService) {
        this.systemTenantService = systemTenantService;
        this.service = service;
        this.roleService = roleService;
        this.Pservice = Pservice;
    }

    /**
     * 仅系统管理员可创建租户
     */
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping
    public void create(@RequestParam String name) {

        UUID tenantId = service.createTenant(name);

    }
    /**
     * 仅系统管理员可查看所有租户
     */
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping
    public Page<Map<String,Object>> list(    //week7day5(week7day1)
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return service.list(q, pageable);
    }



    @PatchMapping("/{tenantId}/members/{userId}/role")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void changeRole(@PathVariable UUID tenantId,
                           @PathVariable UUID userId,
                           @RequestParam Role role) {
        roleService.changeRole(tenantId, userId, role);
    }

    @PatchMapping("/{tenantId}/rename")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void rename(@PathVariable UUID tenantId,
                       @RequestBody RenameTenantRequest request) {
        Pservice.rename(tenantId, request.getNewName());
    }

    @PatchMapping("/{tenantId}/suspend")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void suspend(@PathVariable UUID tenantId) {
        systemTenantService.suspend(tenantId);
    }
    @PatchMapping("/{tenantId}/resume")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void resume(@PathVariable UUID tenantId) {
        systemTenantService.resume(tenantId);
    }
}




