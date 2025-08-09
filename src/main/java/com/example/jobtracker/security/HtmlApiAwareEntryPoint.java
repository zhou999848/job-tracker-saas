package com.example.jobtracker.security;

import jakarta.servlet.http.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class HtmlApiAwareEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException) {

        String uri = request.getRequestURI();
        // API 走 JSON 401；页面走 302 重定向
        if (uri.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return;
        }
        String qs = request.getQueryString();
        String target = (qs == null) ? uri : (uri + "?" + qs);
        String redirect = "/login?redirect=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        try {
            response.sendRedirect(redirect);
        } catch (Exception ignored) {}
    }
}
