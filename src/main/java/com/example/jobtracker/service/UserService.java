package com.example.jobtracker.service;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.exception.DuplicateUsernameException;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;

import com.example.jobtracker.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repo;
    private final TenantRepository tenantRepo;
    private final PasswordEncoder encoder;
    private final CurrentTenant currentTenant;

    public UserService(UserRepository repo, TenantRepository tenantRepo, PasswordEncoder encoder,CurrentTenant currentTenant) {
       this.currentTenant = currentTenant;
        this.repo = repo;
        this.tenantRepo = tenantRepo;
        this.encoder = encoder;
    }
    /**
     * ✅ 推荐：安全注册（管理员/邀请链接传入 tenantId，不信任前端）
     * 规则：用户名在【租户内唯一】。
     * @return 新用户ID（字符串）
     */
    @Transactional
    public String registerViaAdminOrInvite(UserDto dto, UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            throw new IllegalArgumentException("username and password are required");
        }

        // 统一化（可按需决定是否 toLowerCase）
        String username = dto.getUsername().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
        // 可选：邮箱式用户名时建议小写化
        // username = username.toLowerCase(Locale.ROOT);

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // ✅ 关键：租户内判重
        if (repo.existsByTenantIdAndUsername(tenant.getId(), username)) {
            throw new DuplicateUsernameException("Username already exists in this tenant");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setTenant(tenant);



        repo.save(user);
        return user.getId().toString(); // ✅ 返回新用户ID
    }

    /** ⚠️ 仅限管理员/后台使用（保留兼容旧接口，不推荐外部调用） */
    @Transactional
    public void register(UserDto dto) {
        if (dto == null || dto.getTenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        registerViaAdminOrInvite(dto, dto.getTenantId());
    }




    @Transactional//新增
    public void updateProfile(String username, UpdateProfileRequest req) {
        UUID tenantId = currentTenant.requireTenantId();
        User user = repo.findByTenantIdAndUsername(tenantId, username)

                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDisplayName(req.getDisplayName());
        user.setEmail(req.getEmail());
        // 其他可更新字段（禁止在这里更新角色/状态等敏感字段）
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        UUID tenantId = currentTenant.requireTenantId();
        User user = repo.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!encoder.matches(req.getCurrentPassword(), user.getPassword())) {
            // 旧密码不对：返回 400 更合适（前端可提示）
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        String encoded = encoder.encode(req.getNewPassword());
        user.setPassword(encoded);
        user.setPasswordChangedAt(Instant.now()); // 可选：配合 JWT 失效策略
        // 关键：改密后立刻踢掉该用户的所有 session（包含其他浏览器/设备）



        }
    public Optional<User> findByUsername(String username) {
        UUID tenantId = currentTenant.requireTenantId();
       return repo.findByTenantIdAndUsername(tenantId,username);
    }
    }

