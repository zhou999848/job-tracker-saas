package com.example.jobtracker.TenantInvite7a4;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantInviteRepo extends JpaRepository<TenantInvite, UUID> {
    Optional<TenantInvite> findByToken(String token);
    List<TenantInvite> findByTenant_Id(UUID tenantId);

}
