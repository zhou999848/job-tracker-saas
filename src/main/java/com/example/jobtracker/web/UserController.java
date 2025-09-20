package com.example.jobtracker.web;


import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.dto.UserDto;
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

    @Autowired
    public UserController(AuthenticationManager authManager,
                          JwtUtil jwtUtil, UserService service) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.service = service;
    }

    /**
     * ✅ 推荐入口（管理员/邀请注册）
     * [POST] /api/admin/tenants/{tenantId}/users
     * @PreAuthorize 只允许管理员调用
     */
    @PostMapping("/admin/tenants/{tenantId}/users")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','TENANT_ADMIN')")
    public String registerViaAdminOrInvite(@PathVariable UUID tenantId, @RequestBody UserDto dto) {
        logger.info("[Register-Admin] username={} tenant={} - 管理员/邀请注册", dto.getUsername(), tenantId);
        service.registerViaAdminOrInvite(dto, tenantId);
        return "Registered successfully!";
    }

    /**
     * ⚠️ 仅限后台/兼容旧接口（不推荐外部调用）
     * [POST] /api/users/register
     */
    @PostMapping("/register")
    public String register(@RequestBody UserDto dto) {
        logger.warn("[Register-Deprecated] username={} - 仅限后台使用！", dto.getUsername());
        service.register(dto); // 注意：这里用 dto.getTenantId()，不能对外开放
        return "Registered successfully!";
    }
    /**
     * ✅ 登录接口 / User Login
     * [POST] /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            // ★ 把租户放到上下文，供 UserDetailsService 使用
            com.example.jobtracker.tenant.TenantContext.set(req.getTenantId());
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            String token = jwtUtil.generateAccessToken(req.getTenantId(), req.getUsername()); // ★
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("用户名或密码错误 / Invalid username or password");
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
