package com.example.jobtracker.config;

import com.example.jobtracker.security.HtmlApiAwareEntryPoint;
import com.example.jobtracker.security.JwtFilter;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
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

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    private final JwtFilter jwtFilter;
    private final HtmlApiAwareEntryPoint htmlApiAwareEntryPoint;
    public SecurityConfig(JwtFilter jwtFilter,
                          HtmlApiAwareEntryPoint htmlApiAwareEntryPoint) {
        this.jwtFilter = jwtFilter; this.htmlApiAwareEntryPoint = htmlApiAwareEntryPoint;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/style.css", "/css/**").permitAll()
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // 仍然用 POST /logout
                        // 方案1：用 Spring 自带的删除（注意它没法设置 SameSite/HttpOnly）
                        .deleteCookies("JWT")
                        // 方案2（关键）：再手动发一个带 SameSite/HttpOnly 的 Set-Cookie 覆盖
                        .addLogoutHandler((req, res, auth) -> {
                            var clear = org.springframework.http.ResponseCookie.from("JWT", "")
                                    .httpOnly(true)  // 登录时也这样
                                    .secure(false)   // 本地 http 用 false；线上 https 用 true
                                    .path("/")       // 一定要和登录时一致
                                    .sameSite("Lax") // 一定要和登录时一致
                                    .maxAge(0)       // 立即过期
                                    .build();
                            res.addHeader("Set-Cookie", clear.toString());
                        })
                        .logoutSuccessHandler((req, res, auth) -> res.sendRedirect("/login"))
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(htmlApiAwareEntryPoint)) // ★
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))// 禁用 session，?次?求靠 JWT ??
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
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

