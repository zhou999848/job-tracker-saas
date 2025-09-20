
package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository repo;
    private Instant passwordChangedAt = Instant.now(); // 或者根据实际需求赋值

    public MyUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    private static String normalizeRole(String raw) {
        if (raw == null) return "USER";
        String r = raw.trim().toUpperCase();
        // 如果数据库里已经存了 ROLE_ 前缀，去掉再拼
        if (r.startsWith("ROLE_")) {
            r = r.substring(5);
        }
        return r.isBlank() ? "USER" : r;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UUID tenantId = com.example.jobtracker.tenant.TenantContext.requireTenantIdFromRequest();
        User user = repo.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名不存在"));

        // 规范化角色
        String rawRole = (user.getRole() == null || user.getRole().isBlank()) ? "USER" : user.getRole();
        String role = rawRole.trim().toUpperCase();
        if (role.startsWith("ROLE_")) {
            role = role.substring(5); // 去掉多余的 ROLE_
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + role) // 一定是 ROLE_SYSTEM_ADMIN / ROLE_TENANT_ADMIN / ROLE_USER
                .build();
    }


    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }
}
