package com.example.jobtracker.web;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bootstrap")
public class BootstrapController {

    private final UserService userService;
    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;

    @Value("${app.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.token:}")
    private String setupToken;

    public BootstrapController(UserService userService, UserRepository userRepo, TenantRepository tenantRepo) {

        this.userService = userService;
        this.userRepo = userRepo;

    this.tenantRepo = tenantRepo;}

    @PostMapping("/tenants/{tenantId}/admin")
    public ResponseEntity<?> createFirstAdmin(@PathVariable String tenantName,
                                              @RequestHeader(name = "X-Setup-Token", required = false) String token,
                                              @RequestBody UserDto dto) {
        // 1) 开关校验
        if (!enabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "BOOTSTRAP_DISABLED"));
        }
        // 2) 令牌校验
        if (token == null || token.isBlank() || setupToken == null || setupToken.isBlank() || !token.equals(setupToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_SETUP_TOKEN"));
        }
        // 3) 解析 tenantName -> tenantId
        Tenant tenant = tenantRepo.findByName(tenantName.trim())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        UUID tenantId = tenant.getId();

        if (userRepo.countByTenantId(tenantId) > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "ADMIN_ALREADY_EXISTS"));
        }
        // 4) 强制设管理员
        // 如果你的 UserDto#setRole 接受字符串：
        dto.setRole("ADMIN");
        dto.setTenantName(tenantName);
        // 如果你的 UserDto 是 enum：请改成 dto.setRole(Role.ADMIN);

        String id = userService.registerViaAdminOrInvite(dto,tenantId);
        return ResponseEntity.created(URI.create("/api/users/" + id))
                .body(Map.of("id", id));
    }
}
