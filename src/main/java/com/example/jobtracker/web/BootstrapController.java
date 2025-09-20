package com.example.jobtracker.web;

import com.example.jobtracker.dto.UserDto;
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

    @Value("${app.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.token:}")
    private String setupToken;

    public BootstrapController(UserService userService, UserRepository userRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
    }

    @PostMapping("/tenants/{tenantId}/admin")
    public ResponseEntity<?> createFirstAdmin(@PathVariable UUID tenantId,
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
        // 3) 仅允许“该租户还没有任何用户”时执行
        if (userRepo.countByTenantId(tenantId) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "TENANT_ALREADY_INITIALIZED"));
        }

        // 4) 强制设管理员
        // 如果你的 UserDto#setRole 接受字符串：
        dto.setRole("ADMIN");
        // 如果你的 UserDto 是 enum：请改成 dto.setRole(Role.ADMIN);

        String id = userService.registerViaAdminOrInvite(dto, tenantId);
        return ResponseEntity.created(URI.create("/api/users/" + id))
                .body(Map.of("id", id));
    }
}
