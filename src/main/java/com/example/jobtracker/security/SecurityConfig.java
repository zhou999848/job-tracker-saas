package com.example.jobtracker.security;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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


    @Bean // 仅调试：保证数据库密码为 BCrypt
    CommandLineRunner seed(UserRepository repo, PasswordEncoder pe) {
        return args -> repo.findByUsername("user1").orElseGet(() -> {
            var u = new com.example.jobtracker.domain.User();
            u.setUsername("user1");
            u.setPassword(pe.encode("123456"));
            return repo.save(u);
        });
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean @Primary
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
    public JwtFilter jwtFilter(JwtUtil jwtUtil, MyUserDetailsService uds, UserRepository userRepo) {
        return new JwtFilter(jwtUtil, uds, userRepo);
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
                res.sendRedirect("/login?redirect=" + redirect); // ← 不用预览特性
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter,
            AuthenticationEntryPoint htmlApiAwareEntryPoint) throws Exception {
        // 1) 真正用上?配置?的 Cookie ??（可被 JS ?到，便于 fetch 写 header）
       CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");                 // 全站有效
        repo.setCookieName("XSRF-TOKEN-V2");     // ?一新名字，避免?史残留

        // 2) ??：使用 XorCsrfTokenRequestAttributeHandler（会?来自 Header/表?的掩? token 做解掩?）
       var xor = new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler();
        // 若?的表?是 multipart 或需要从表?字段?取 token，也打??行：
        // xor.setTokenFromMultipartDataEnabled(true);

        http
                .formLogin(f -> f.disable())     // 用?自己的 /login
                .logout(l -> l.disable())        // 用?自己的 /logout
                .csrf(csrf -> csrf
                                .csrfTokenRepository(repo)                  // ? 用上 repo
                                .csrfTokenRequestHandler(xor)    // ? 表??藏字段可用
                                // 如果登?是 JSON/没有 CSRF 字段，建?忽略：

                                .ignoringRequestMatchers("/api/**")   //在 SecurityConfig 把 /logout 加? CSRF 忽略： ?能保???在的 @PostMapping("/logout") 一定?到，从而把 Cookie 清掉。
                        // 更安全（?期做法）保持 CSRF ??，不忽略 /logout，在?面表?里? CSRF ?藏域：
                        // .ignoringRequestMatchers("/logout") // 若登出不是表?，??忽略
                )
                .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(100)
                .sessionRegistry(sessionRegistry())
        )//IF_REQUIRED IF_REQUIRED
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/logout").permitAll()
                        .requestMatchers("/css/**","/images/**","/api/auth/refresh","/js/**","/style.css","/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(htmlApiAwareEntryPoint));
// ? token 在?染?面前就被“触?”并写入 Cookie
       http.addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class);
        // ??：注入“?例”，不要再?用无参方法
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    //新し   ?制“取一下” token，触?生成 + 写 Cookie */
    static final class CsrfCookieFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            org.springframework.security.web.csrf.CsrfToken token =
                    (org.springframework.security.web.csrf.CsrfToken)
                            request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            if (token != null) {
                token.getToken(); // ← ??一次即可触?生成/保存到 Cookie
            }
            filterChain.doFilter(request, response);
        }
    }
}




