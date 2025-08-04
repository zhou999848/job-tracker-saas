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

   @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // 暂时禁用 CSRF，开发阶段可用
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",               // 登录页面
                                "/css/**",              // 样式文件
                                "/js/**",               // JS 脚本
                                "/images/**",           // 图片资源
                                "/api/users/register"   // 注册 API
                        ).permitAll()              // 放行这些路径
                        .anyRequest().authenticated() // 其他请求都需要登录
                )
                .formLogin(login -> login
                        .loginPage("/login")                       // 自定义登录页面
                        .defaultSuccessUrl("/jobs", true)          // 登录成功后跳转
                        .failureUrl("/login?error")        // 登录失败后跳转
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")         // 登出后跳转
                        .permitAll()
                )
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

