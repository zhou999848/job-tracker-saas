package com.example.jobtracker.web;


import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.jobtracker.dto.LoginRequest;//("/login")
import com.example.jobtracker.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService service;
    @Autowired
    public UserController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,UserService service) {

        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.service = service;
    }


    @PostMapping("/register")
    public String register(@RequestBody UserDto dto) {
        service.register(dto);
        return "注册成功！";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("🔍 开始验证用户：" + request.getUsername());

            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()
                    )
            );

            System.out.println("✅ 认证成功，准备生成 token");

            String token = jwtUtil.generateToken(request.getUsername());

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ 登录失败：" + e.getMessage());
            return ResponseEntity.status(401).body("用户名或密码错误");
        }
    }

}
