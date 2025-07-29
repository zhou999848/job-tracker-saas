package com.example.jobtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // 允许 POST 请求（关闭跨站验证）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register").permitAll()  // 注册接口放行
                        .anyRequest().authenticated()  // 其他接口需要登录
                );
        return http.build();
    }
}
