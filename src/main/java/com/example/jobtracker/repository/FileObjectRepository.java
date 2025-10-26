package com.example.jobtracker.repository;

import com.example.jobtracker.domain.FileObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileObjectRepository extends JpaRepository<FileObject, UUID> {

    // 用于做“同租户”权限校验（更安全）
    Optional<FileObject> findByIdAndTenantId(UUID id, UUID tenantId);
}

