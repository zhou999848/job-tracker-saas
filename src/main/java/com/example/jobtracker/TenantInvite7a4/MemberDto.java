package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.TenantInvite7a4.Role;
import java.time.Instant;
import java.util.UUID;

public record MemberDto(UUID userId, String username, String email, Role role, Instant createdAt) {}