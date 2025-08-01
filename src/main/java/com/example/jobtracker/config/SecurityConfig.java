package com.example.jobtracker.config;

import com.example.jobtracker.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register", "/api/users/login","/api/jobs").permitAll() // 注册和登录不拦截
                        .anyRequest().authenticated() // 其他都要登录验证身份
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 禁用 session，每次请求靠 JWT 验证
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // 添加 JwtFilter放在默认的用户名密码认证过滤器 之前。 这样可以让 Spring Security 使用 JWT 来验证用户身份


                .build();
    }


    @Bean//
        // 公开AuthenticationManager，以便在你自己的控制器或服务类中可以手动执行认证逻辑。
        // (Exposes the AuthenticationManager so that you can manually perform authentication logic in your
        // own controllers or service classes.)
        // (独自のコントローラーまたはサービス クラスで認証ロジックを手動で実行できるように、AuthenticationManager を公開します。)
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

