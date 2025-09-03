package com.example.jobtracker.web;


import com.example.jobtracker.dto.ApiResponse;
import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    private final UserService userService;

    public ProfileApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse> updateProfileApi(@Valid @RequestBody UpdateProfileRequest req,
                                                        BindingResult br,
                                                        Principal principal) {
        if (principal == null) {
            // 让前端拿到 401 才能触发刷新
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("UNAUTHORIZED"));
        }
        if (br.hasErrors()) {
            String msg = br.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .findFirst().orElse("不正な入力です");
            return ResponseEntity.badRequest().body(ApiResponse.error(msg));
        }
        userService.updateProfile(principal.getName(), req);

        // 也可以把更新后的 me 返回给前端（如需同步 UI）
        var meOpt = userService.findByUsername(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok("プロフィールが更新されました", meOpt.orElse(null)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePasswordApi(@Valid @RequestBody ChangePasswordRequest req,
                                                         BindingResult br,
                                                         HttpServletResponse response,
                                                         Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("UNAUTHORIZED"));
        }
        if (br.hasErrors()) {
            String msg = br.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .findFirst().orElse("不正な入力です");
            return ResponseEntity.badRequest().body(ApiResponse.error(msg));
        }
        try {
            userService.changePassword(principal.getName(), req);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("現在のパスワードが正しくありません"));
        }

        // 清理认证 Cookie（ACCESS/REFRESH）
        clearAllAuthCookies(response);

        return ResponseEntity.ok(ApiResponse.ok("パスワードが変更されました。再度ログインしてください"));
    }

    // 建议放在同一个 Controller 里
    private void clearAllAuthCookies(HttpServletResponse response) {
        // 1) 访问令牌：ACCESS（路径 /）
        // 本地 http（Secure=false, SameSite=Lax）
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        // 线上 https（Secure=true, SameSite=None）
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 2) 刷新令牌：REFRESH（路径 /api/auth/refresh）
        // 本地 http
        response.addHeader("Set-Cookie", "REFRESH=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        // 线上 https
        response.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth/refresh; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 3) 兼容历史：若曾用过单一 JWT 名称，顺手清掉（路径 /）
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
    }
}

