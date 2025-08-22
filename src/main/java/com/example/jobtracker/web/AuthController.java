package com.example.jobtracker.web;

import com.example.jobtracker.dto.LoginRequest;
import com.example.jobtracker.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        // 只从 REFRESH Cookie 取
        String refresh = jwtUtil.resolveToken(request, "REFRESH");
        if (refresh == null || !jwtUtil.validateRefreshToken(refresh)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh"));
        }

        String username = jwtUtil.getUsername(refresh);
        String newAccess = jwtUtil.generateAccessToken(username);

        boolean secure = request.isSecure(); // 本地 false
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", newAccess)
                .httpOnly(true).secure(false).sameSite("Lax")
                .path("/")               // 业务请求都会带
                .maxAge(30)//15 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Refreshed"));
    }
}
