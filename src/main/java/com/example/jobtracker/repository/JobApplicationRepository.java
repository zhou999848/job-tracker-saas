package com.example.jobtracker.repository;
import com.example.jobtracker.dto.JobApplicationDto;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.jobtracker.domain.JobApplication;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface JobApplicationRepository extends JpaRepository<JobApplication,UUID> {
    // 列表：按租户 + 用户名分页
    Page<JobApplication> findByTenantIdAndUser_Username(UUID tenantId, String username, Pageable pageable);

    // 搜索：按租户 + 关键字(公司名) + 用户名分页
    Page<JobApplication> findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(
            UUID tenantId, String keyword, String username, Pageable pageable);

    // 详情：按租户 + 主键
    Optional<JobApplication> findByIdAndTenantId(UUID id, UUID tenantId);

    // （可选）仅按租户分页（用于租户管理员总览）
    Page<JobApplication> findByTenantId(UUID tenantId, Pageable pageable);

}
