package com.example.jobtracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// RequestContextFilter.java
@Component
public class MdcContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", rid);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        MDC.put("username", (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANON");
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
