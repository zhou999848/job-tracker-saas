package com.example.jobtracker.security;
import java.io.Serializable;
import java.util.UUID;

public record LoginUser(UUID userId, String username, UUID tenantId) implements Serializable {
    public static UUID getUserId() {return UUID.randomUUID();};
}
