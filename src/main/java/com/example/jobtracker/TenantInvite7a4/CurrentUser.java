package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUser {
    private final UserRepository userRepo;

    public CurrentUser(UserRepository userRepo) { this.userRepo = userRepo; }

    public UUID requireUserId(UUID tenantId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("No authentication");

        Object details = auth.getDetails();
        if (details instanceof LoginUser lu && lu.userId() != null) {
            return lu.userId();
        }

        // 兜底：用 username + tenantId 查
        String username = auth.getName();
        return userRepo.findByTenantIdAndUsername(tenantId, username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found under tenant"));
    }

    public String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("No authentication");
        return auth.getName();
    }
}

