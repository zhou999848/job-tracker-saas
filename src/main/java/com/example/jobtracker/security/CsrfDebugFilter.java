package com.example.jobtracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

@Component
class CsrfDebugFilter extends OncePerRequestFilter {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CsrfDebugFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if ("POST".equals(req.getMethod()) && ("/login".equals(req.getServletPath()) || "/logout".equals(req.getServletPath()))) {
            CsrfToken attr = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            String cookieVal = Optional.ofNullable(req.getCookies())
                    .stream().flatMap(Arrays::stream)
                    .filter(c -> "XSRF-TOKEN".equals(c.getName()))
                    .map(Cookie::getValue).findFirst().orElse(null);
            log.info("CSRF debug: hidden/_csrf={}, cookie/XSRF-TOKEN={}", attr != null ? attr.getToken() : null, cookieVal);
        }
        chain.doFilter(req, res);
    }
}
