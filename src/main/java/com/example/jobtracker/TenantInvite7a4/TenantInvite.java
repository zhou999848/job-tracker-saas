package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import jakarta.persistence.*;

import com.example.jobtracker.TenantInvite7a4.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_invites")
public class TenantInvite {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // USER / TENANT_ADMIN

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    // TenantInvite.java

    @JoinColumn(name = "created_by", nullable = true)  // 原来可能是 nullable=false
    private UUID createdBy;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isUsed() { return usedAt != null; }
    public boolean isExpired(Clock clock) { return Instant.now(clock).isAfter(expiresAt); }
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }



}
