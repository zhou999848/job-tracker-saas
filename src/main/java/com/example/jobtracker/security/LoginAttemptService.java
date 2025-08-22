package com.example.jobtracker.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final int MAX_ATTEMPT = 5;

    public void loginFailed(String username) {
        attempts.put(username, attempts.getOrDefault(username, 0) + 1);
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }

    public boolean isBlocked(String username) {
        return attempts.getOrDefault(username, 0) >= MAX_ATTEMPT;
    }
}

