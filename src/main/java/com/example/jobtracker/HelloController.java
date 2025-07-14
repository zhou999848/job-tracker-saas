package com.example.jobtracker;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class HelloController {
@GetMapping("/version")
public Map<String,String>version() {
    return Map.of("/versio", "0.1.0");
}
}