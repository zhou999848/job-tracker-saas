package com.example.jobtracker.domain;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String username;   // 登录名
    private String password;   // 加密后的密码
    // ... existing fields

    private Instant passwordChangedAt; // 密码最近更新时间, 用于密码过期检查,用于让旧 Token 失效。
private String displayName;
private String email;

    @ManyToOne//多用户对一租户(tenant)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant t) { this.passwordChangedAt = t; }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
}
