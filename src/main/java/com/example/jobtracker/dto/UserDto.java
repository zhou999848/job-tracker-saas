package com.example.jobtracker.dto;


import com.example.jobtracker.TenantInvite7a4.Role;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserDto {

    @NotBlank(message = "username cannot be blank")
    private String username;
    @NotBlank(message = "password cannot be blank")
    @Size(min = 8, max = 72, message = "Password length 8~72")
    // 至少包含大小写与数字，示例规则（可自行调整）
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Need upper, lower and digit")
    private String password;
    private UUID tenantId; // 所属租户ID
    private String tenantName;
    // ✅ 新增字段：角色
    @Column(nullable = false)
    private String role = "USER"; // 默认值 USER

    // getter / setter
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}
