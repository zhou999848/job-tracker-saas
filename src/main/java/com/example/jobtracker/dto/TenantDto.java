package com.example.jobtracker.dto;


import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String status, // 如果你的实体是 Enum，也可把这里改成 Enum 类型并同步调整下方 @Query
        long memberCount,
        Instant createdAt
) {}


