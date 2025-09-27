package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.TenantInvite7a4.Role;
import com.example.jobtracker.TenantInvite7a4.TenantInvite;

public record CreateInviteReq(String email, Role role, Integer daysToExpire) {}