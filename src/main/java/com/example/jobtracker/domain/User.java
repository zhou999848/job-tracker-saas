package com.example.jobtracker.domain;
import com.example.jobtracker.TenantInvite7a4.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // 关键！
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String username;   // 登录名
    private String password;   // 加密后的密码
    // ... existing fields

    private Instant passwordChangedAt; // 密码最近更新时间, 用于密码过期检查,用于让旧 Token 失效。
private String displayName;
private String email;

// SYSTEM_ADMIN 的场景下 tenant 可为 null（系统级用户，不隶属于任何租户）
    @ManyToOne//多用户对一租户(tenant)
    @JoinColumn(name = "tenant_id", nullable = true)
    private Tenant tenant;
    // ✅ 新增字段：角色
    // 2) User 实体加角色（字符串存储最直观）
    private String tenantName;
    @Enumerated(EnumType.STRING)
    private Role role;


    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt= Instant.now();



    @Version
    private Long version;


    // getter / setter
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public String getTenantName() {
        return tenantName;
    }
    public void setTenantName(String tenantName) { this.tenantName = tenantName;
}}
