package com.example.jobtracker.config;

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

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/style.css").permitAll()
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll() // 注册和登録不lan截
                        .anyRequest().authenticated() // 其他都要登???身fen
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 禁用 session，?次?求靠 JWT ??
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // 添加 JwtFilter放在默?的用?名密?????器 之前。 ??可以? Spring Security 使用 JWT 来??用?身?


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

