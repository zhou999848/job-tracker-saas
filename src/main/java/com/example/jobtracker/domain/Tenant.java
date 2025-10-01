package com.example.jobtracker.domain;

import com.example.jobtracker.ああ７a５.TenantStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // ✅ 自动 UUID
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 租户名称（公司名/团队名）

    @OneToMany(mappedBy = "tenant")
    private List<User> users = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    // getter/setter
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

public void setName(String name) {
        this.name = name;}

public List<User> getUsers() {
        return users;
    }
    public void setUsers(List<User> users) {
        this.users = users;
    }
    public TenantStatus getStatus() {
        return status;
    }
    public void setStatus(TenantStatus status) { this.status = status;
}

    public Instant getOtherField() {

        return null;
    }
}

