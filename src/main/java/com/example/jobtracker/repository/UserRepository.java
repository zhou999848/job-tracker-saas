package com.example.jobtracker.repository;

import com.example.jobtracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    //Optional<User> findByUsername(String username);  // 改为带tenantId的（见下）
    boolean existsByUsername(String username);



    long countByTenantId(UUID tenantId);//统计方法


    // ✅ 租户内唯一：用于创建前判重
    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    // 可选：查具体用户（可能别处也用得上）
    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);
}
