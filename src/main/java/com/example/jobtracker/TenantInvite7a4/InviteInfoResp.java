package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.TenantInvite7a4.Role;
import java.time.Instant;

public record InviteInfoResp(String email, String tenantName, Role role, Instant expiresAt, boolean used) {}