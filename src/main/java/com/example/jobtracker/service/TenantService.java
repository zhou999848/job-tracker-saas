package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService {
    private final TenantRepository repo;

    public TenantService(TenantRepository repo) {
        this.repo = repo;
    }

    public Tenant createTenant(String name) {
        if (repo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists");
        }
        Tenant t = new Tenant();
        t.setName(name);
        return repo.save(t);
    }

    public List<Tenant> listAll() {
        return repo.findAll();
    }
}

