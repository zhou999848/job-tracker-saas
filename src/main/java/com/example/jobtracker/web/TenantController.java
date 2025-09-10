package com.example.jobtracker.web;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.service.TenantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PostMapping
    public Tenant create(@RequestParam String name) {
        return service.createTenant(name);
    }

    @GetMapping
    public List<Tenant> list() {
        return service.listAll();
    }
}
