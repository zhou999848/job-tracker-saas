package com.example.jobtracker.repository;

import com.example.jobtracker.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jobtracker.TenantInvite7a4.Role;

import org.springframework.data.domain.Pageable;
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

    Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);//（如用户名与邮箱等同，也可不需）

    Optional<User> findAllByTenantId(UUID tenantId);

    long countByTenantIdAndRole(UUID tenantId, Role role);

    boolean existsByTenant_IdAndUsername(UUID tenantId, String username);


    Optional<User> findByTenant_NameAndUsername(String tenantName, String username);
    long countByTenant_Name(String tenantName);
    long countByTenant_NameAndRole(String tenantName, Role role);

    Optional<User> findByTenant_IdAndUsername(UUID tenantId, String username);
    Optional<User> findByIdAndTenant_Id(UUID userId, UUID tenantId);
    long countByTenant_IdAndRole(UUID tenantId, Role role);
    long countByTenant_Id(UUID tenantId);
}