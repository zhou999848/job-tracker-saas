package com.example.jobtracker.TenantInvite7a4;

// 1) 角色枚举（简单直白）
public enum Role {
    USER,          // 普通成员
    TENANT_ADMIN,  // 租户管理员：管自己租户的成员/资源
    SYSTEM_ADMIN   // 系统管理员：全局视角
}

