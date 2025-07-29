package com.example.jobtracker.service;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public void register(UserDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));  // 加密
        repo.save(user);
    }
}
