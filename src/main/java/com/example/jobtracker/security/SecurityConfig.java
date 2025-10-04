package com.example.jobtracker.security;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // ✅ 开启 @PreAuthorize 等注解

public class SecurityConfig {

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public static org.springframework.boot.web.servlet.ServletListenerRegistrationBean<
            org.springframework.security.web.session.HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(
                new org.springframework.security.web.session.HttpSessionEventPublisher());
    }

// SecurityConfig

    //@Bean
    //    // 仅调试：保证数据库密码为 BCrypt
  //  CommandLineRunner seed(UserRepository repo, PasswordEncoder pe) {
      //  return args -> repo.findByUsername("user1").orElseGet(() -> {
        //    var u = new com.example.jobtracker.domain.User();
          //  u.setUsername("user1");
            //u.setPassword(pe.encode("123456"));
            //return repo.save(u);
        //});
    //}


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public MyUserDetailsService myUserDetailsService(UserRepository userRepo) {
        return new MyUserDetailsService(userRepo);
    }

    @Bean
    public AuthenticationProvider daoAuthProvider(MyUserDetailsService uds, PasswordEncoder pe) {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(pe);
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider dao) {
        return new ProviderManager(dao);
    }

    @Bean
    public JwtFilter jwtFilter(JwtUtil jwtUtil, MyUserDetailsService uds, UserRepository userRepo, TenantRepository tenantRepo) {
        return new JwtFilter(jwtUtil, uds, userRepo,tenantRepo);
    }

    @Bean
    public AuthenticationEntryPoint htmlApiAwareEntryPoint() {
        return (req, res, ex) -> {
            String uri = req.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String target = (uri == null || "/error".equals(uri)) ? "/jobs" : uri;
                String redirect = URLEncoder.encode(target, StandardCharsets.UTF_8);
                // 先尝试静默刷新；失败的话，控制器里会再跳 /login
             // res.sendRedirect(STR."/login?redirect=\{redirect}");
             res.sendRedirect("/auth/silent-refresh?redirect=" + redirect);
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter,
            AuthenticationEntryPoint htmlApiAwareEntryPoint) throws Exception {

        http
                // ?用自定? /login、/logout
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())

                // ?? 完全?? CSRF（无状? + JWT）
                .csrf(csrf -> csrf.disable())

                // 无状?
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ????（保持?原来的）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/login","/api/tenants","/api/invites/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/logout","/api/tenants","/api/users/login","/admin/tenants/{tenantId}/users","/api/invites/{token}/accept").permitAll()
                        .requestMatchers("/css/**","/images/**","/api/auth/refresh","/register","/api/users/register","/auth/silent-refresh","/js/**",
                                "/style.css","/favicon.ico","/error","/api/bootstrap/**","/api/admin/tenants/{tenantId}/members/{userId}").permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(e -> e.authenticationEntryPoint(htmlApiAwareEntryPoint));

        // 仍然? JWT ??器?在 UsernamePasswordAuthenticationFilter 之前
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }}
