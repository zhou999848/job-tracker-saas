package com.example.jobtracker.dto;

import java.util.UUID;

public class LoginRequest {
    private String username;
    private String password;
    private String tenantName;
    // Getter / Setterx
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTenantName() { return tenantName; }
public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}
