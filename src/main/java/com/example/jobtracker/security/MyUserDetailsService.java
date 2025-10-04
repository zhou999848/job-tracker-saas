// src/main/java/com/example/jobtracker/security/MyUserDetailsService.java
package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public MyUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ★ 只依赖 tenantId（由 TenantResolverFilter 或 JWT 解析后放入 ThreadLocal）
        UUID tenantId = TenantContext.requireTenantIdFromRequest();

        User user = repo.findByTenant_IdAndUsername(tenantId, username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        // 规范化角色（确保授予 ROLE_XXX）
        String role = (user.getRole() == null ? "USER" : user.getRole().name()).toUpperCase();
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + role)
                .build();
    }
}
