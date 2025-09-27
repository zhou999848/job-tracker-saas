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


}

