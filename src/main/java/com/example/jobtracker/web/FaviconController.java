// src/main/java/.../web/FaviconController.java
package com.example.jobtracker.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FaviconController {
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        // 不返回图标，直接 204，浏览器不再跳到错误页
        return ResponseEntity.noContent().build();
    }
}
