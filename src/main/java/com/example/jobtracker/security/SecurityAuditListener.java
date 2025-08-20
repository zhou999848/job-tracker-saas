package com.example.jobtracker.security;

import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

// SecurityAuditListener.java
@Component
public class SecurityAuditListener {
    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    @EventListener
    public void onSuccess(org.springframework.security.authentication.event.AuthenticationSuccessEvent e) {
        String u = e.getAuthentication().getName();
        log.info("LOGIN_SUCCESS user={}", u);
    }

    @EventListener
    public void onFailure(org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent e) {
        String u = e.getAuthentication().getName();
        log.warn("LOGIN_FAILURE user={} reason={}", u, e.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onDenied(org.springframework.security.authorization.event.AuthorizationDeniedEvent<?> e) {
        // Spring Security 6 的授权拒绝事件
        log.warn("ACCESS_DENIED details={}", e);
    }
}
