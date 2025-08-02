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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

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
     * ✅ 用户注册接口 / User Registration
     * [POST] /api/users/register
     */
    @PostMapping("/register")
    public String register(@RequestBody UserDto dto) {
        logger.info("[Register] username={} - 用户注册 / User registration", dto.getUsername());
        service.register(dto);
        return "注册成功！/ Registered successfully!";
    }

    /**
     * ✅ 登录接口 / User Login
     * [POST] /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        logger.info("[Login Attempt] username={} - 用户尝试登录 / User trying to login", request.getUsername());
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()
                    )
            );

            logger.info("[Login Success] username={} - 登录成功 / Authentication successful", request.getUsername());

            String token = jwtUtil.generateToken(request.getUsername());

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            logger.info("[Token Issued] username={} - 生成JWT成功 / JWT token generated", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warn("[Login Failed] username={} - 登录失败 / Login failed: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body("用户名或密码错误 / Invalid username or password");
        }
    }
}