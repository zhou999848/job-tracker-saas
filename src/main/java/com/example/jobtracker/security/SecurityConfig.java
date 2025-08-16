package com.example.jobtracker.security;

import com.example.jobtracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean//仅调试，为保证数据库的密码为Bcrypt
    CommandLineRunner seed(UserRepository repo, PasswordEncoder pe) {
        return args -> {
            repo.findByUsername("user1").orElseGet(() -> {
                var u = new com.example.jobtracker.domain.User();
                u.setUsername("user1");
                u.setPassword(pe.encode("123456")); // BCrypt
                return repo.save(u);
            });
        };
    }

    // 1) 只保留一个 PasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // 想兼容老密码再换成：PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // 2) 只保留一个 UserDetailsService（你的 MyUserDetailsService）
    //    如果你还有别的 UDS Bean，给这个加 @Primary 或删掉其它内存用户配置
    @Bean
    @Primary
    public MyUserDetailsService myUserDetailsService(UserRepository userRepo) {
        return new MyUserDetailsService(userRepo);
    }

    // 3) DaoAuthenticationProvider 明确绑定 “上面的 UDS + Encoder”
    @Bean
    public AuthenticationProvider daoAuthProvider(MyUserDetailsService uds,
                                                  PasswordEncoder pe) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(pe);
        // p.setHideUserNotFoundExceptions(false); // 调试更直观
        return p;
    }

    // 4) AuthenticationManager 只装入上面的 Provider
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider dao) {
        return new ProviderManager(dao);
    }

    // 5) 你的 JWT 过滤器作为 Bean 注入
    @Bean
    public JwtFilter jwtFilter(JwtUtil jwtUtil, MyUserDetailsService uds) {
        return new JwtFilter(jwtUtil, uds);
    }


    @Bean
    public AuthenticationEntryPoint htmlApiAwareEntryPoint() {
        return (req, res, ex) -> {
            String uri = req.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String target = (uri == null || "/error".equals(uri)) ? "/jobs" : uri;
                String redirect = java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
                res.sendRedirect(STR."/login?redirect=\{redirect}");
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter,
            AuthenticationEntryPoint htmlApiAwareEntryPoint) throws Exception {
        // 1) 真正用上你配置过的 Cookie 仓库（可被 JS 读到，便于 fetch 写 header）
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");                 // 全站有效
        repo.setCookieName("XSRF-TOKEN-V2");     // 统一新名字，避免历史残留

        // 2) 关键：使用 XorCsrfTokenRequestAttributeHandler（会对来自 Header/表单的掩码 token 做解掩码）
        var xor = new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler();
        // 若你的表单是 multipart 或需要从表单字段读取 token，也打开这行：
       // xor.setTokenFromMultipartDataEnabled(true);

        http
                .formLogin(f -> f.disable())     // 用你自己的 /login
                .logout(l -> l.disable())        // 用你自己的 /logout
                .csrf(csrf -> csrf
                                .csrfTokenRepository(repo)                  // ✅ 用上 repo
                                .csrfTokenRequestHandler(xor)    // ✅ 表单隐藏字段可用
                                // 如果登录是 JSON/没有 CSRF 字段，建议忽略：

                                .ignoringRequestMatchers("/api/**","/logout")   //在 SecurityConfig 把 /logout 加进 CSRF 忽略： 这能保证你现在的 @PostMapping("/logout") 一定跑到，从而把 Cookie 清掉。
                        // 更安全（长期做法）保持 CSRF 开启，不忽略 /logout，在页面表单里带 CSRF 隐藏域：
                        // .ignoringRequestMatchers("/logout") // 若登出不是表单，临时忽略
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy. IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/logout").permitAll()
                        .requestMatchers("/css/**","/images/**","/js/**","/style.css","/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(htmlApiAwareEntryPoint));
// 让 token 在渲染页面前就被“触发”并写入 Cookie
        http.addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class);
        // 关键：注入“实例”，不要再调用无参方法
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
/** 强制“取一下” token，触发生成 + 写 Cookie */
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
            token.getToken(); // ← 访问一次即可触发生成/保存到 Cookie
        }
        filterChain.doFilter(request, response);
    }
}}

