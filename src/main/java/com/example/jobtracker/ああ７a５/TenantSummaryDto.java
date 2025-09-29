package com.example.jobtracker.ああ７a５;

import java.time.Instant;
import java.util.UUID;

public record TenantSummaryDto(UUID id, String name, TenantStatus status, long memberCount, Instant createdAt) {}
