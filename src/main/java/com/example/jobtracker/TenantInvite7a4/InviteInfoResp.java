package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.TenantInvite7a4.Role;
import java.time.Instant;
import java.util.UUID;

public record InviteInfoResp(UUID id, String email, String tenantName, Role role, Instant expiresAt, boolean used, String inviteToken) {}