package com.example.jobtracker.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController  // 告诉 Spring 这个类是个 Web 接口控制器
@RequestMapping("/api")  // 所有方法都以 /api 开头
public class HelloController {

    @GetMapping("/version")  // 访问路径 /api/version
    public Map<String, String> version() {
        return Map.of("version", "0.1.0");
    }
}
