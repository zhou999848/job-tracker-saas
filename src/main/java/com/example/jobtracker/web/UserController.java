package com.example.jobtracker.web;

import com.example.jobtracker.dto.UserDto;
import com.example.jobtracker.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public String register(@RequestBody UserDto dto) {
        service.register(dto);
        return "注册成功！";
    }
}
