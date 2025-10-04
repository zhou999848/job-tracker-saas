package com.example.jobtracker.web;


import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.jobtracker.dto.LoginRequest;//("/login")
import com.example.jobtracker.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService service;
    private final TenantRepository tenantRepo;

    @Autowired
    public UserController(AuthenticationManager authManager,
                          JwtUtil jwtUtil, UserService service,TenantRepository tenantRepo) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.service = service;
        this.tenantRepo = tenantRepo;
    }
    /**
     * ✅ 推荐入口（管理员/邀请注册）—— 使用租户ID（UUID）
     * [POST] /api/admin/tenants/{tenantId}/users
     */
    @PostMapping("/admin/tenants/{tenantId}/users")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','TENANT_ADMIN')")
    public String registerViaAdminOrInvite(
            @PathVariable UUID tenantId,
            @RequestBody UserDto dto) {
        logger.info("[Register-Admin] username={} tenantId={} - 管理员/邀请注册",
                dto.getUsername(), tenantId);
        service.registerViaAdminOrInvite(dto, tenantId);
        return "Registered successfully!";
    }

    /**
     * ⚠️ 仅限后台/兼容旧接口（不对外）
     * [POST] /api/users/register
     */
    @PostMapping("/register")
    public String register(@RequestBody UserDto dto) {
        logger.warn("[Register-Deprecated] username={} - 仅限后台使用！", dto.getUsername());
        service.register(dto); // 使用 dto.tenantId
        return "Registered successfully!";
    }

    /**
     * ✅ 登录接口 / User Login
     * [POST] /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            // 1) 校验输入
            if (req.getTenantName() == null || req.getTenantName().isBlank()
                    || req.getUsername() == null || req.getPassword() == null) {
                return ResponseEntity.badRequest().body("tenantName/username/password required");
            }

            // 2) 解析 tenantName -> tenantId
            Tenant tenant = tenantRepo.findByName(req.getTenantName().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
            UUID tenantId = tenant.getId();

            // 3) 将租户放入上下文（供 UserDetailsService 使用）
            com.example.jobtracker.tenant.TenantContext.set(tenantId);

            // 4) 执行认证
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

            // 5) 生成 JWT（建议在 token 里带上 tenantId 与 username）
            String token = jwtUtil.generateAccessToken(tenantId, req.getUsername());

            // 6) 返回
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "tenantId", tenantId.toString(),
                    "tenantName", tenant.getName(),
                    "username", req.getUsername()
            ));
        } catch (Exception e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("用户名或密码错误 / Invalid username or password");
        } finally {
            com.example.jobtracker.tenant.TenantContext.clear();
        }
    }

    @GetMapping("/me")
    public Map<String, Object> getMe() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据需要返回字段（避免泄漏敏感信息）
        return Map.of("username", username);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        service.updateProfile(username, req);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                            HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
       service.changePassword(username, req);

        // 关键：让旧 JWT 立即失效 —— 简单做法：清 Cookie，前端重定向到登录
        ResponseCookie cleared = ResponseCookie.from("JWT", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());

        return ResponseEntity.ok(Map.of(
                "message", "Password changed, please login again"
        ));
    }
}
