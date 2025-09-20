package com.example.jobtracker.dto;

import java.util.UUID;

public class LoginRequest {
    private String username;
    private String password;
    private UUID tenantId;
    // Getter / Setterx
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

}
