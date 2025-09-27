package com.example.jobtracker.TenantInvite7a4;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantInviteRepo extends JpaRepository<TenantInvite, UUID> {
    Optional<TenantInvite> findByToken(String token);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);


            Optional<TenantInvite> findAllByTenantIdAndEmail(UUID tenantId, String email);
}
