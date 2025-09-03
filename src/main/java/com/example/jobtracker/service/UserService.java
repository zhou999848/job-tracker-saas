package com.example.jobtracker.service;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.SessionKickoutService;
import jakarta.transaction.Transactional;
import com.example.jobtracker.security.SessionKickoutService;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




@Service
public class UserService {
private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final SessionKickoutService sessionKickoutService;
private final UserRepository repo;
    private final PasswordEncoder encoder;
    public UserService(UserRepository repo, PasswordEncoder encoder, SessionKickoutService sessionKickoutService) {
        this.repo = repo;
        this.encoder = encoder;
        this.sessionKickoutService = sessionKickoutService;
    }

    public void register(UserDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));  // 加密
        repo.save(user);
    }
    @Transactional//新增
    public void updateProfile(String username, UpdateProfileRequest req) {
        User user = repo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDisplayName(req.getDisplayName());
        user.setEmail(req.getEmail());
        // 其他可更新字段（禁止在这里更新角色/状态等敏感字段）
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        User user = repo.findByUsername(username)
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

        int n = sessionKickoutService.kickout(username);
        logger.info("Kicked {} sessions for user {}", n, username);
    }
    public Optional<User> findByUsername(String username) {
        return repo.findByUsername(username);
    }
}
